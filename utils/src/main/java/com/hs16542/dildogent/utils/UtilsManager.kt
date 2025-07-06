package com.hs16542.dildogent.utils

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 工具管理器
 * 提供统一的工具接口，管理各种工具功能
 */
class UtilsManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: UtilsManager? = null
        
        fun getInstance(context: Context): UtilsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UtilsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 小红书视频提取器
    private val xhsVideoExtractor = XiaohongshuVideoExtractor(context)
    
    // 默认下载目录
    val defaultDownloadDir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Dildogent")
    }
    
    /**
     * 创建自定义下载目录
     * @param dirName 目录名称
     * @return 创建的目录
     */
    fun createDownloadDir(dirName: String): File {
        val dir = File(defaultDownloadDir, dirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * 从小红书文本中提取视频
     * @param text 包含小红书链接的文本
     * @param outputDir 输出目录，如果为null则使用默认目录
     * @param callback 回调接口
     */
    fun extractXhsVideos(
        text: String,
        outputDir: File? = null,
        callback: VideoExtractCallback? = null
    ) {
        val targetDir = outputDir ?: defaultDownloadDir
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                callback?.onStart()
                
                val result = xhsVideoExtractor.extractVideosFromText(text, targetDir)
                
                withContext(Dispatchers.Main) {
                    if (result.error != null) {
                        callback?.onError(result.error!!)
                    } else {
                        callback?.onSuccess(result)
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError(e.message ?: "未知错误")
                }
            }
        }
    }
    
    /**
     * 从文本中提取小红书链接
     * @param text 文本内容
     * @return 提取到的链接列表
     */
    fun extractXhsLinks(text: String): List<String> {
        return xhsVideoExtractor.extractXhsLinks(text)
    }
    
    /**
     * 从文本中提取小红书笔记ID
     * @param text 文本内容
     * @return 提取到的笔记ID列表
     */
    fun extractXhsNoteIds(text: String): List<String> {
        return xhsVideoExtractor.extractXhsNoteIds(text)
    }
    
    /**
     * 检查文本是否包含小红书链接
     * @param text 文本内容
     * @return 是否包含小红书链接
     */
    fun containsXhsLink(text: String): Boolean {
        return xhsVideoExtractor.extractXhsLinks(text).isNotEmpty()
    }
    
    /**
     * 获取已下载的视频列表
     * @param dir 目录，如果为null则使用默认目录
     * @return 视频文件列表
     */
    fun getDownloadedVideos(dir: File? = null): List<File> {
        val targetDir = dir ?: defaultDownloadDir
        return if (targetDir.exists() && targetDir.isDirectory) {
            targetDir.listFiles { file ->
                file.isFile && (file.extension.equals("mp4", ignoreCase = true) ||
                        file.extension.equals("mov", ignoreCase = true) ||
                        file.extension.equals("avi", ignoreCase = true))
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 清理下载目录
     * @param dir 目录，如果为null则使用默认目录
     * @return 是否清理成功
     */
    fun clearDownloadDir(dir: File? = null): Boolean {
        val targetDir = dir ?: defaultDownloadDir
        return try {
            if (targetDir.exists()) {
                targetDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 视频提取回调接口
     */
    interface VideoExtractCallback {
        /**
         * 开始提取
         */
        fun onStart() {}
        
        /**
         * 提取成功
         * @param result 提取结果
         */
        fun onSuccess(result: XiaohongshuVideoExtractor.ExtractResult)
        
        /**
         * 提取失败
         * @param error 错误信息
         */
        fun onError(error: String)
    }
} 