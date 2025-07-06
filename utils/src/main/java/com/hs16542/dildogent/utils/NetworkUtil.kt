package com.hs16542.dildogent.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * 网络工具类
 * 提供统一的网络请求处理，支持HTTP和HTTPS
 */
object NetworkUtil {
    
    private const val TAG = "NetworkUtil"
    
    // 用户代理，模拟浏览器请求
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    /**
     * 执行网络请求，自动尝试HTTPS和HTTP
     * @param url 请求URL
     * @param headers 额外的请求头
     * @return 响应内容，失败返回null
     */
    suspend fun executeRequest(url: String, headers: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
        // 尝试HTTPS版本
        val httpsUrl = url.replace("http://", "https://")
        
        // 先尝试HTTPS
        try {
            val httpsResponse = executeSingleRequest(httpsUrl, headers)
            if (httpsResponse != null) {
                Log.d(TAG, "HTTPS请求成功: $httpsUrl")
                return@withContext httpsResponse
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTPS请求失败，尝试HTTP: $httpsUrl", e)
        }
        
        // 如果HTTPS失败，尝试原始HTTP链接
        try {
            val httpResponse = executeSingleRequest(url, headers)
            if (httpResponse != null) {
                Log.d(TAG, "HTTP请求成功: $url")
                return@withContext httpResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP请求也失败: $url", e)
        }
        
        return@withContext null
    }
    
    /**
     * 执行单个网络请求
     * @param url 请求URL
     * @param headers 额外的请求头
     * @return 响应内容，失败返回null
     */
    private suspend fun executeSingleRequest(url: String, headers: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
            
            // 添加额外的请求头
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            // 如果是HTTP请求，添加Upgrade-Insecure-Requests头
            if (url.startsWith("http://")) {
                requestBuilder.header("Upgrade-Insecure-Requests", "1")
            }
            
            val request = requestBuilder.build()
            
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                } else {
                    Log.w(TAG, "请求失败，状态码: ${response.code}, URL: $url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "网络请求异常: $url", e)
        }
        
        return@withContext null
    }
    
    /**
     * 获取重定向后的URL
     * @param url 原始URL
     * @param headers 额外的请求头
     * @return 重定向后的URL，失败返回null
     */
    suspend fun getRedirectUrl(url: String, headers: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
        // 尝试HTTPS版本
        val httpsUrl = url.replace("http://", "https://")
        
        // 先尝试HTTPS
        try {
            val httpsRedirectUrl = getSingleRedirectUrl(httpsUrl, headers)
            if (httpsRedirectUrl != null) {
                Log.d(TAG, "HTTPS重定向成功: $httpsUrl -> $httpsRedirectUrl")
                return@withContext httpsRedirectUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTPS重定向失败，尝试HTTP: $httpsUrl", e)
        }
        
        // 如果HTTPS失败，尝试原始HTTP链接
        try {
            val httpRedirectUrl = getSingleRedirectUrl(url, headers)
            if (httpRedirectUrl != null) {
                Log.d(TAG, "HTTP重定向成功: $url -> $httpRedirectUrl")
                return@withContext httpRedirectUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP重定向也失败: $url", e)
        }
        
        return@withContext null
    }
    
    /**
     * 获取单个请求的重定向URL
     * @param url 请求URL
     * @param headers 额外的请求头
     * @return 重定向后的URL，失败返回null
     */
    private suspend fun getSingleRedirectUrl(url: String, headers: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
            
            // 添加额外的请求头
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            // 如果是HTTP请求，添加Upgrade-Insecure-Requests头
            if (url.startsWith("http://")) {
                requestBuilder.header("Upgrade-Insecure-Requests", "1")
            }
            
            val request = requestBuilder.build()
            
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.request.url.toString()
                } else {
                    Log.w(TAG, "重定向请求失败，状态码: ${response.code}, URL: $url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "重定向请求异常: $url", e)
        }
        
        return@withContext null
    }
    
    /**
     * 检查网络连接是否可用
     * @param context 上下文
     * @return 是否可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            Log.e(TAG, "检查网络连接失败", e)
            false
        }
    }
} 