package com.hs16542.dildogent.llmutil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * 离线模型演示Activity
 * 展示如何使用离线模型进行情感分析
 */
class OfflineModelDemoActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "OfflineModelDemo"
    }
    
    private lateinit var llmService: LLMServiceInternal
    private lateinit var modelDownloadManager: ModelDownloadManager
    
    private lateinit var inputText: EditText
    private lateinit var analyzeButton: Button
    private lateinit var resultText: TextView
    private lateinit var modelStatusText: TextView
    private lateinit var downloadProgressBar: ProgressBar
    private lateinit var downloadButton: Button
    private lateinit var loadModelButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_model_demo)
        
        initViews()
        initServices()
        setupListeners()
        updateModelStatus()
    }
    
    private fun initViews() {
        inputText = findViewById(R.id.input_text)
        analyzeButton = findViewById(R.id.analyze_button)
        resultText = findViewById(R.id.result_text)
        modelStatusText = findViewById(R.id.model_status_text)
        downloadProgressBar = findViewById(R.id.download_progress_bar)
        downloadButton = findViewById(R.id.download_button)
        loadModelButton = findViewById(R.id.load_model_button)
    }
    
    private fun initServices() {
        llmService = LLMServiceInternal(this)
        modelDownloadManager = ModelDownloadManager(this)
        
        // 设置优先使用离线模型
        llmService.setUseOfflineModelFirst(true)
    }
    
    private fun setupListeners() {
        analyzeButton.setOnClickListener {
            val text = inputText.text.toString()
            if (text.isNotEmpty()) {
                analyzeEmotion(text)
            }
        }
        
        downloadButton.setOnClickListener {
            downloadModel()
        }
        
        loadModelButton.setOnClickListener {
            loadModel()
        }
    }
    
    private fun analyzeEmotion(text: String) {
        analyzeButton.isEnabled = false
        resultText.text = "分析中..."
        
        lifecycleScope.launch {
            try {
                val result = llmService.analyzeEmotion(text)
                
                val resultString = """
                    情感分析结果：
                    
                    情感类型：${result.emotion}
                    置信度：${String.format("%.2f", result.confidence)}
                    强度：${String.format("%.2f", result.intensity)}
                    关键词：${result.keywords.joinToString(", ")}
                    模型类型：${result.modelType}
                    时间戳：${result.timestamp}
                """.trimIndent()
                
                resultText.text = resultString
                
            } catch (e: Exception) {
                Log.e(TAG, "情感分析失败", e)
                resultText.text = "分析失败：${e.message}"
            } finally {
                analyzeButton.isEnabled = true
            }
        }
    }
    
    private fun downloadModel() {
        downloadButton.isEnabled = false
        downloadProgressBar.visibility = View.VISIBLE
        
        // 选择第一个可用模型进行下载
        val modelConfig = ModelDownloadManager.AVAILABLE_MODELS.firstOrNull()
        if (modelConfig == null) {
            resultText.text = "没有可用的模型配置"
            downloadButton.isEnabled = true
            downloadProgressBar.visibility = View.GONE
            return
        }
        
        lifecycleScope.launch {
            try {
                val success = modelDownloadManager.downloadModel(modelConfig) { progress ->
                    runOnUiThread {
                        downloadProgressBar.progress = (progress * 100).toInt()
                    }
                }
                
                if (success) {
                    resultText.text = "模型下载成功：${modelConfig.name}"
                    updateModelStatus()
                } else {
                    resultText.text = "模型下载失败"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "模型下载失败", e)
                resultText.text = "下载失败：${e.message}"
            } finally {
                downloadButton.isEnabled = true
                downloadProgressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadModel() {
        loadModelButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val downloadedModels = modelDownloadManager.getDownloadedModels()
                if (downloadedModels.isEmpty()) {
                    resultText.text = "没有已下载的模型"
                    loadModelButton.isEnabled = true
                    return@launch
                }
                
                // 尝试加载第一个模型
                val modelFile = downloadedModels.first()
                val success = when {
                    modelFile.name.contains("tflite") -> {
                        llmService.loadTensorFlowLiteModel(modelFile.name)
                    }
                    modelFile.name.contains("onnx") -> {
                        llmService.loadOnnxModel(modelFile.name)
                    }
                    else -> {
                        resultText.text = "不支持的模型格式：${modelFile.name}"
                        false
                    }
                }
                
                if (success) {
                    resultText.text = "模型加载成功：${modelFile.name}"
                    updateModelStatus()
                } else {
                    resultText.text = "模型加载失败"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "模型加载失败", e)
                resultText.text = "加载失败：${e.message}"
            } finally {
                loadModelButton.isEnabled = true
            }
        }
    }
    
    private fun updateModelStatus() {
        val isModelLoaded = llmService.isOfflineModelLoaded()
        val modelType = llmService.getCurrentOfflineModelType()
        val downloadedModels = modelDownloadManager.getDownloadedModels()
        
        val statusText = """
            模型状态：
            
            离线模型已加载：${if (isModelLoaded) "是" else "否"}
            当前模型类型：${modelType?.name ?: "无"}
            已下载模型数量：${downloadedModels.size}
            可用存储空间：${String.format("%.1f MB", modelDownloadManager.getAvailableStorage() / 1024.0 / 1024.0)}
        """.trimIndent()
        
        modelStatusText.text = statusText
    }
    
    override fun onDestroy() {
        super.onDestroy()
        llmService.release()
    }
} 