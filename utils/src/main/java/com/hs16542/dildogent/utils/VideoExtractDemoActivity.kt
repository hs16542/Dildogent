package com.hs16542.dildogent.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hs16542.dildogent.utils.R
import com.hs16542.dildogent.utils.log.logI

/**
 * 视频提取演示Activity
 * 展示如何使用小红书视频提取功能
 */
class VideoExtractDemoActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private lateinit var utilsManager: UtilsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_extract_demo)
        
        utilsManager = UtilsManager.getInstance(this)
        
        // 检查权限
        checkPermissions()
        
        // 示例：处理小红书链接
        processXhsLinkExample()
    }
    
    /**
     * 检查必要权限
     */
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }
    }
    
    /**
     * 处理小红书链接示例
     */
    private fun processXhsLinkExample() {
        // 示例文本，包含小红书链接
        val exampleText = """
            36 浏阳桨板烟花发布了一篇小红书笔记，快来看吧！ 😆 Fpbs13L2ZpLBTqy 😆 http://xhslink.com/a/sfg0VART9Spgb 复制本条信息，打开【小红书】App查看精彩内容！
        """.trimIndent()
        
        // 1. 检查是否包含小红书链接
        if (utilsManager.containsXhsLink(exampleText)) {
            ToastUtil.showShort(this, "检测到小红书链接")
            
            // 2. 提取链接
            val links = utilsManager.extractXhsLinks(exampleText)
            println("提取到的链接: $links")
            
            // 3. 提取笔记ID
            val noteIds = utilsManager.extractXhsNoteIds(exampleText)
            println("提取到的笔记ID: $noteIds")
            
            // 4. 开始提取视频
            extractVideos(exampleText)
        } else {
            ToastUtil.showShort(this, "未检测到小红书链接")
        }
    }
    
    /**
     * 提取视频
     * @param text 包含小红书链接的文本
     */
    private fun extractVideos(text: String) {
        utilsManager.extractXhsVideos(
            text = text,
            outputDir = utilsManager.createDownloadDir("XiaohongshuVideos"),
            callback = object : UtilsManager.VideoExtractCallback {
                override fun onStart() {
                    ToastUtil.showShort(this@VideoExtractDemoActivity, "开始提取视频...")
                }
                
                override fun onSuccess(result: XiaohongshuVideoExtractor.ExtractResult) {
                    val message = """
                        提取完成！
                        总链接数: ${result.totalLinks}
                        总视频数: ${result.totalVideos}
                        成功下载: ${result.successCount}
                        失败数量: ${result.failedCount}
                    """.trimIndent()
                    
                    ToastUtil.showLong(this@VideoExtractDemoActivity, message)
                    logI(msg = message)
                    
                    // 显示下载的视频信息
                    result.downloadedVideos.forEach { downloadedVideo ->
                        println("下载成功: ${downloadedVideo.videoInfo.title} -> ${downloadedVideo.filePath}")
                    }
                    
                    // 显示失败的信息
                    if (result.failedVideos.isNotEmpty()) {
                        println("失败的视频:")
                        result.failedVideos.forEach { videoInfo ->
                            println("- ${videoInfo.title}: ${videoInfo.videoUrl}")
                        }
                    }
                    
                    if (result.failedLinks.isNotEmpty()) {
                        println("失败的链接:")
                        result.failedLinks.forEach { link ->
                            println("- $link")
                        }
                    }
                }
                
                override fun onError(error: String) {
                    logI("提取失败: $error")
                }
            }
        )
    }
    
    /**
     * 获取已下载的视频列表
     */
    private fun getDownloadedVideos() {
        val videos = utilsManager.getDownloadedVideos()
        println("已下载的视频数量: ${videos.size}")
        videos.forEach { file ->
            println("视频文件: ${file.name} (${file.length()} bytes)")
        }
    }
    
    /**
     * 清理下载目录
     */
    private fun clearDownloadDir() {
        val success = utilsManager.clearDownloadDir()
        if (success) {
            ToastUtil.showShort(this, "清理成功")
        } else {
            ToastUtil.showShort(this, "清理失败")
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                ToastUtil.showShort(this, "权限已授予")
            } else {
                ToastUtil.showLong(this, "需要存储权限才能下载视频")
            }
        }
    }
} 