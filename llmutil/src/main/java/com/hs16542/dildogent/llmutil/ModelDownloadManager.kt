package com.hs16542.dildogent.llmutil

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 模型下载管理器
 * 负责离线模型的下载、更新和版本控制
 */
class ModelDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val MODELS_DIR = "models"
        private const val DOWNLOAD_TIMEOUT = 300L // 5分钟超时
        
        // 预定义模型配置
        data class ModelConfig(
            val name: String,
            val url: String,
            val type: OfflineModelManager.Companion.ModelType,
            val version: String,
            val size: Long,
            val description: String
        )
        
        // 示例模型配置（实际使用时应该从服务器获取）
        val AVAILABLE_MODELS = listOf(
            ModelConfig(
                name = "emotion_analysis_tflite",
                url = "https://example.com/models/emotion_analysis.tflite",
                type = OfflineModelManager.Companion.ModelType.TENSORFLOW_LITE,
                version = "1.0.0",
                size = 1024 * 1024 * 10, // 10MB
                description = "基于TensorFlow Lite的情感分析模型"
            ),
            ModelConfig(
                name = "emotion_analysis_onnx",
                url = "https://example.com/models/emotion_analysis.onnx",
                type = OfflineModelManager.Companion.ModelType.ONNX_RUNTIME,
                version = "1.0.0",
                size = 1024 * 1024 * 15, // 15MB
                description = "基于ONNX的情感分析模型"
            )
        )
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private val modelsDir = File(context.filesDir, MODELS_DIR).apply {
        if (!exists()) {
            mkdirs()
        }
    }
    
    /**
     * 下载模型
     */
    suspend fun downloadModel(modelConfig: ModelConfig, progressCallback: ((Float) -> Unit)? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始下载模型: ${modelConfig.name}")
                
                val modelFile = File(modelsDir, "${modelConfig.name}_v${modelConfig.version}")
                val tempFile = File(modelsDir, "${modelConfig.name}_temp")
                
                // 检查是否已存在
                if (modelFile.exists()) {
                    Log.d(TAG, "模型已存在: ${modelFile.absolutePath}")
                    return@withContext true
                }
                
                val request = Request.Builder()
                    .url(modelConfig.url)
                    .build()
                
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code}")
                }
                
                val body = response.body ?: throw IOException("响应体为空")
                val contentLength = body.contentLength()
                
                // 下载到临时文件
                tempFile.outputStream().use { outputStream ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 报告进度
                            if (contentLength > 0) {
                                val progress = totalBytesRead.toFloat() / contentLength
                                progressCallback?.invoke(progress)
                            }
                        }
                    }
                }
                
                // 验证文件大小
                if (tempFile.length() != contentLength && contentLength > 0) {
                    tempFile.delete()
                    throw IOException("文件大小不匹配")
                }
                
                // 重命名为最终文件
                tempFile.renameTo(modelFile)
                
                Log.d(TAG, "模型下载完成: ${modelFile.absolutePath}")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "模型下载失败: ${modelConfig.name}", e)
                // 清理临时文件
                File(modelsDir, "${modelConfig.name}_temp").delete()
                false
            }
        }
    }
    
    /**
     * 获取已下载的模型列表
     */
    fun getDownloadedModels(): List<File> {
        return modelsDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }
    
    /**
     * 检查模型是否需要更新
     */
    suspend fun checkModelUpdate(modelConfig: ModelConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val localFile = File(modelsDir, "${modelConfig.name}_v${modelConfig.version}")
                
                // 如果本地文件不存在，需要下载
                if (!localFile.exists()) {
                    return@withContext true
                }
                
                // 检查文件大小
                if (localFile.length() != modelConfig.size) {
                    return@withContext true
                }
                
                // 这里可以添加更复杂的版本检查逻辑
                // 比如检查文件的MD5哈希值等
                
                false
            } catch (e: Exception) {
                Log.e(TAG, "检查模型更新失败", e)
                true
            }
        }
    }
    
    /**
     * 删除模型
     */
    fun deleteModel(modelName: String): Boolean {
        return try {
            val modelFile = File(modelsDir, modelName)
            if (modelFile.exists()) {
                modelFile.delete()
                Log.d(TAG, "模型已删除: $modelName")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除模型失败: $modelName", e)
            false
        }
    }
    
    /**
     * 获取模型文件路径
     */
    fun getModelPath(modelName: String): String? {
        val modelFile = File(modelsDir, modelName)
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    /**
     * 获取可用存储空间
     */
    fun getAvailableStorage(): Long {
        return modelsDir.freeSpace
    }
    
    /**
     * 清理临时文件
     */
    fun cleanupTempFiles() {
        modelsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith("_temp")) {
                file.delete()
            }
        }
    }
    
    /**
     * 使用WorkManager进行后台下载
     */
    fun scheduleModelDownload(modelConfig: ModelConfig) {
        val downloadWorkRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(
                "model_name" to modelConfig.name,
                "model_url" to modelConfig.url,
                "model_type" to modelConfig.type.name,
                "model_version" to modelConfig.version
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_${modelConfig.name}",
            ExistingWorkPolicy.REPLACE,
            downloadWorkRequest
        )
        
        Log.d(TAG, "已调度模型下载任务: ${modelConfig.name}")
    }
}

/**
 * WorkManager工作器，用于后台下载模型
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val modelName = inputData.getString("model_name") ?: return Result.failure()
        val modelUrl = inputData.getString("model_url") ?: return Result.failure()
        val modelType = inputData.getString("model_type") ?: return Result.failure()
        val modelVersion = inputData.getString("model_version") ?: return Result.failure()
        
        return try {
            val modelConfig = ModelDownloadManager.Companion.ModelConfig(
                name = modelName,
                url = modelUrl,
                type = OfflineModelManager.Companion.ModelType.valueOf(modelType),
                version = modelVersion,
                size = 0,
                description = ""
            )
            
            val downloadManager = ModelDownloadManager(applicationContext)
            val success = downloadManager.downloadModel(modelConfig)
            
            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("ModelDownloadWorker", "下载失败", e)
            Result.failure()
        }
    }
} 