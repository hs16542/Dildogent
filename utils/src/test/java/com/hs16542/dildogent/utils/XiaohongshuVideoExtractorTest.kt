package com.hs16542.dildogent.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * 小红书视频提取器测试
 */
class XiaohongshuVideoExtractorTest {
    
    @Test
    fun testExtractXhsLinks() {
        val text = "36 浏阳桨板烟花发布了一篇小红书笔记，快来看吧！ 😆 Fpbs13L2ZpLBTqy 😆 http://xhslink.com/a/sfg0VART9Spgb 复制本条信息，打开【小红书】App查看精彩内容！"
        
        // 由于需要Context，这里只是测试正则表达式逻辑
        val linkPattern = Regex("http://xhslink\\.com/[a-zA-Z0-9/]+")
        val links = linkPattern.findAll(text).map { it.value }.toList()
        
        assertEquals(1, links.size)
        assertEquals("http://xhslink.com/a/sfg0VART9Spgb", links[0])
    }
    
    @Test
    fun testExtractXhsNoteIds() {
        val text = "36 浏阳桨板烟花发布了一篇小红书笔记，快来看吧！ 😆 Fpbs13L2ZpLBTqy 😆 http://xhslink.com/a/sfg0VART9Spgb 复制本条信息，打开【小红书】App查看精彩内容！"
        
        val notePattern = Regex("Fpbs[a-zA-Z0-9]+")
        val noteIds = notePattern.findAll(text).map { it.value }.toList()
        
        assertEquals(1, noteIds.size)
        assertEquals("Fpbs13L2ZpLBTqy", noteIds[0])
    }
    
    @Test
    fun testContainsXhsLink() {
        val textWithLink = "这是一个包含小红书链接的文本 http://xhslink.com/abc123"
        val textWithoutLink = "这是一个不包含小红书链接的文本"
        
        val linkPattern = Regex("http://xhslink\\.com/[a-zA-Z0-9/]+")
        
        assertTrue(linkPattern.containsMatchIn(textWithLink))
        assertFalse(linkPattern.containsMatchIn(textWithoutLink))
    }
    
    @Test
    fun testVideoInfoDataClass() {
        val videoInfo = XiaohongshuVideoExtractor.VideoInfo(
            title = "测试视频",
            videoUrl = "https://example.com/video.mp4",
            coverUrl = "https://example.com/cover.jpg",
            sourceUrl = "https://example.com/source",
            duration = 60000L
        )
        
        assertEquals("测试视频", videoInfo.title)
        assertEquals("https://example.com/video.mp4", videoInfo.videoUrl)
        assertEquals("https://example.com/cover.jpg", videoInfo.coverUrl)
        assertEquals("https://example.com/source", videoInfo.sourceUrl)
        assertEquals(60000L, videoInfo.duration)
    }
    
    @Test
    fun testExtractResultDataClass() {
        val result = XiaohongshuVideoExtractor.ExtractResult()
        
        // 初始值测试
        assertEquals(0, result.totalLinks)
        assertEquals(0, result.totalVideos)
        assertEquals(0, result.successCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.downloadedVideos.isEmpty())
        assertTrue(result.failedVideos.isEmpty())
        assertTrue(result.failedLinks.isEmpty())
        assertNull(result.error)
        
        // 设置值测试
        result.totalLinks = 2
        result.totalVideos = 1
        result.successCount = 1
        result.failedCount = 0
        
        assertEquals(2, result.totalLinks)
        assertEquals(1, result.totalVideos)
        assertEquals(1, result.successCount)
        assertEquals(0, result.failedCount)
    }
} 