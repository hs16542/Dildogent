package com.hs16542.dildogent.utils

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NetworkUtilTest {

    @Mock
    private lateinit var mockContext: Context

    @Test
    fun testExecuteRequest() {
        runBlocking {
            // 测试网络请求功能
            // 注意：这是单元测试，实际网络请求可能失败
            val result = NetworkUtil.executeRequest("https://httpbin.org/get")
            // 主要测试方法调用不会抛出异常
        }
    }

    @Test
    fun testGetRedirectUrl() {
        runBlocking {
            // 测试重定向功能
            val result = NetworkUtil.getRedirectUrl("https://httpbin.org/redirect/1")
            // 主要测试方法调用不会抛出异常
        }
    }

    @Test
    fun testIsNetworkAvailable() {
        // 测试网络状态检查
        val result = NetworkUtil.isNetworkAvailable(mockContext)
        // 主要测试方法调用不会抛出异常
    }

    @Test
    fun testXhsLinkResolution() {
        runBlocking {
            // 测试小红书链接解析
            val xhsLink = "http://xhslink.com/a/sfg0VART9Spgb"
            val result = NetworkUtil.getRedirectUrl(xhsLink)
            // 主要测试方法调用不会抛出异常
        }
    }
} 