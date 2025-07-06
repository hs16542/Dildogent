package com.hs16542.dildogent.utils

import android.content.Context
import android.util.Log
import com.hs16542.dildogent.utils.log.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 小红书视频提取器
 * 用于从小红书链接中自动提取视频文件
 */
class XiaohongshuVideoExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = ""
        
        // 小红书链接正则表达式
        private val XHS_LINK_PATTERN = Pattern.compile("http://xhslink\\.com/[a-zA-Z0-9/]+")
        private val XHS_NOTE_PATTERN = Pattern.compile("Fpbs[a-zA-Z0-9]+")
        
        // 用户代理，模拟浏览器请求
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    /**
     * 从文本中提取小红书链接
     * @param text 包含小红书链接的文本
     * @return 提取到的小红书链接列表
     */
    fun extractXhsLinks(text: String): List<String> {
        val links = mutableListOf<String>()
        val matcher = XHS_LINK_PATTERN.matcher(text)
        
        while (matcher.find()) {
            links.add(matcher.group())
        }
        
        return links
    }
    
    /**
     * 从文本中提取小红书笔记ID
     * @param text 包含小红书笔记ID的文本
     * @return 提取到的小红书笔记ID列表
     */
    fun extractXhsNoteIds(text: String): List<String> {
        val noteIds = mutableListOf<String>()
        val matcher = XHS_NOTE_PATTERN.matcher(text)
        
        while (matcher.find()) {
            noteIds.add(matcher.group())
        }
        
        return noteIds
    }
    
    /**
     * 解析小红书链接获取真实URL
     * @param xhsLink 小红书短链接
     * @return 真实的小红书页面URL
     */
    suspend fun resolveXhsLink(xhsLink: String): String? = withContext(Dispatchers.IO) {
        try {
            // 使用NetworkUtil获取重定向后的URL
            val realUrl = NetworkUtil.getRedirectUrl(xhsLink)
            if (realUrl != null) {
                Log.d(TAG, "解析小红书链接成功: $xhsLink -> $realUrl")
                return@withContext realUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析小红书链接失败: $xhsLink", e)
        }
        
        return@withContext null
    }
    
    /**
     * 从小红书页面提取视频信息
     * @param xhsUrl 小红书页面URL
     * @return 视频信息列表
     */
    suspend fun extractVideoInfo(xhsUrl: String): List<VideoInfo> = withContext(Dispatchers.IO) {
        val videoInfos = mutableListOf<VideoInfo>()
        
        try {
            // 使用NetworkUtil执行请求
            val htmlContent = NetworkUtil.executeRequest(xhsUrl)
            if (htmlContent != null) {
                Log.d(TAG, "提取视频信息成功: $xhsUrl")
                videoInfos.addAll(parseVideoInfoFromHtml(htmlContent, xhsUrl))
            } else {
                Log.w(TAG, "获取页面内容失败: $xhsUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取视频信息失败: $xhsUrl", e)
        }
        
        return@withContext videoInfos
    }
    
    /**
     * 从HTML内容中解析视频信息
     * @param htmlContent HTML内容
     * @param sourceUrl 来源URL
     * @return 视频信息列表
     */
    private fun parseVideoInfoFromHtml(htmlContent: String, sourceUrl: String): List<VideoInfo> {
        val videoInfos = mutableListOf<VideoInfo>()
        
        try {
            // 尝试解析JSON数据
            val jsonPattern = Pattern.compile("\"video\":\\s*\\{[^}]+\\}")
            val jsonMatcher = jsonPattern.matcher(htmlContent)
            
            while (jsonMatcher.find()) {
                val videoJson = jsonMatcher.group()
                val videoInfo = parseVideoJson(videoJson, sourceUrl)
                videoInfo?.let { videoInfos.add(it) }
            }
            
            // 如果JSON解析失败，尝试解析视频URL
            if (videoInfos.isEmpty()) {
                val videoUrlPattern = Pattern.compile("https://[^\"]*\\.mp4[^\"]*")
                val videoUrlMatcher = videoUrlPattern.matcher(htmlContent)
                
                while (videoUrlMatcher.find()) {
                    val videoUrl = videoUrlMatcher.group()
                    val videoInfo = VideoInfo(
                        title = "小红书视频",
                        videoUrl = videoUrl,
                        coverUrl = "",
                        sourceUrl = sourceUrl,
                        duration = 0L
                    )
                    videoInfos.add(videoInfo)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析HTML内容失败", e)
        }
        
        return videoInfos
    }
    
    /**
     * 解析视频JSON数据
     * @param videoJson 视频JSON字符串
     * @param sourceUrl 来源URL
     * @return 视频信息
     */
    private fun parseVideoJson(videoJson: String, sourceUrl: String): VideoInfo? {
        return try {
            // 简单的JSON解析，提取关键信息
            val urlPattern = Pattern.compile("\"url\":\\s*\"([^\"]+)\"")
            val titlePattern = Pattern.compile("\"title\":\\s*\"([^\"]+)\"")
            val coverPattern = Pattern.compile("\"cover\":\\s*\"([^\"]+)\"")
            val durationPattern = Pattern.compile("\"duration\":\\s*(\\d+)")
            
            val urlMatcher = urlPattern.matcher(videoJson)
            val titleMatcher = titlePattern.matcher(videoJson)
            val coverMatcher = coverPattern.matcher(videoJson)
            val durationMatcher = durationPattern.matcher(videoJson)
            
            val videoUrl = if (urlMatcher.find()) urlMatcher.group(1) else ""
            val title = if (titleMatcher.find()) titleMatcher.group(1) else "小红书视频"
            val coverUrl = if (coverMatcher.find()) coverMatcher.group(1) else ""
            val duration = if (durationMatcher.find()) durationMatcher.group(1)?.toLong() ?: 0L else 0L
            
            if (videoUrl.isNotEmpty()) {
                VideoInfo(title, videoUrl, coverUrl, sourceUrl, duration)
            } else null
            
        } catch (e: Exception) {
            Log.e(TAG, "解析视频JSON失败", e)
            null
        }
    }
    
    /**
     * 下载视频文件
     * @param videoInfo 视频信息
     * @param outputDir 输出目录
     * @return 下载的文件路径
     */
    suspend fun downloadVideo(videoInfo: VideoInfo, outputDir: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val fileName = "${videoInfo.title}_${System.currentTimeMillis()}.mp4"
            val outputFile = File(outputDir, fileName)

            logI("out put file: ${outputFile.path}")

            val request = Request.Builder()
                .url(videoInfo.videoUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            logI("request: ${request.url}")
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        FileOutputStream(outputFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    return@withContext outputFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载视频失败: ${videoInfo.videoUrl}", e)
        }
        
        return@withContext null
    }
    
    /**
     * 完整的小红书视频提取流程
     * @param text 包含小红书链接的文本
     * @param outputDir 视频输出目录
     * @return 提取结果
     */
    suspend fun extractVideosFromText(text: String, outputDir: File): ExtractResult {
        val result = ExtractResult()
        
        try {
            // 1. 提取小红书链接
            val xhsLinks = extractXhsLinks(text)
            result.totalLinks = xhsLinks.size
            
            for (link in xhsLinks) {
                try {
                    // 2. 解析真实URL
                    val realUrl = resolveXhsLink(link)
                    if (realUrl != null) {
                        // 3. 提取视频信息
                        val videoInfos = extractVideoInfo(realUrl)
                        result.totalVideos += videoInfos.size
                        
                        // 4. 下载视频
                        for (videoInfo in videoInfos) {
                            val filePath = downloadVideo(videoInfo, outputDir)
                            if (filePath != null) {
                                result.downloadedVideos.add(DownloadedVideo(videoInfo, filePath))
                                result.successCount++
                            } else {
                                result.failedVideos.add(videoInfo)
                                result.failedCount++
                            }
                        }
                    } else {
                        result.failedLinks.add(link)
                        result.failedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理链接失败: $link", e)
                    result.failedLinks.add(link)
                    result.failedCount++
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "视频提取流程失败", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * 视频信息数据类
     */
    data class VideoInfo(
        val title: String,
        val videoUrl: String,
        val coverUrl: String,
        val sourceUrl: String,
        val duration: Long
    )
    
    /**
     * 已下载视频信息
     */
    data class DownloadedVideo(
        val videoInfo: VideoInfo,
        val filePath: String
    )
    
    /**
     * 提取结果
     */
    data class ExtractResult(
        var totalLinks: Int = 0,
        var totalVideos: Int = 0,
        var successCount: Int = 0,
        var failedCount: Int = 0,
        var downloadedVideos: MutableList<DownloadedVideo> = mutableListOf(),
        var failedVideos: MutableList<VideoInfo> = mutableListOf(),
        var failedLinks: MutableList<String> = mutableListOf(),
        var error: String? = null
    )
} 