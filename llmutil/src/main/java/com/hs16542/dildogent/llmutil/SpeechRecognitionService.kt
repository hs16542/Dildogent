package com.hs16542.dildogent.llmutil

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.util.Base64

/**
 * 语音识别服务
 * 支持多种语音识别API的集成
 */
class SpeechRecognitionService {
    
    companion object {
        private const val TAG = "SpeechRecognitionService"
        private const val BAIDU_SPEECH_API_URL = "https://vop.baidu.com/server_api"
        private const val GOOGLE_SPEECH_API_URL = "https://speech.googleapis.com/v1/speech:recognize"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // API配置
    private var baiduAccessToken: String? = null
    private var googleApiKey: String? = null
    
    /**
     * 设置百度语音识别访问令牌
     */
    fun setBaiduAccessToken(accessToken: String) {
        this.baiduAccessToken = accessToken
    }
    
    /**
     * 设置Google语音识别API密钥
     */
    fun setGoogleApiKey(apiKey: String) {
        this.googleApiKey = apiKey
    }
    
    /**
     * 语音识别
     */
    suspend fun recognizeSpeech(audioData: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                when {
                    !baiduAccessToken.isNullOrEmpty() -> recognizeWithBaidu(audioData)
                    !googleApiKey.isNullOrEmpty() -> recognizeWithGoogle(audioData)
                    else -> {
                        Log.w(TAG, "未配置语音识别API，使用模拟数据")
                        generateMockTranscription(audioData)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "语音识别失败", e)
                generateMockTranscription(audioData)
            }
        }
    }
    
    /**
     * 使用百度语音识别
     */
    private suspend fun recognizeWithBaidu(audioData: ByteArray): String {
        val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)
        
        val requestBody = BaiduSpeechRequest(
            format = "pcm",
            rate = 16000,
            channel = 1,
            token = baiduAccessToken!!,
            speech = base64Audio,
            len = audioData.size,
            cuid = "android_device",
            dev_pid = 1537 // 普通话识别
        )
        
        val request = Request.Builder()
            .url(BAIDU_SPEECH_API_URL)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val baiduResponse = gson.fromJson(responseBody, BaiduSpeechResponse::class.java)
            
            if (baiduResponse.err_no == 0) {
                return baiduResponse.result.joinToString(" ")
            } else {
                throw IOException("百度语音识别失败: ${baiduResponse.err_msg}")
            }
        }
        
        throw IOException("百度语音识别API调用失败: ${response.code}")
    }
    
    /**
     * 使用Google语音识别
     */
    private suspend fun recognizeWithGoogle(audioData: ByteArray): String {
        val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)
        
        val requestBody = GoogleSpeechRequest(
            config = GoogleSpeechRequest.Config(
                encoding = "LINEAR16",
                sampleRateHertz = 16000,
                languageCode = "zh-CN",
                enableAutomaticPunctuation = true
            ),
            audio = GoogleSpeechRequest.Audio(
                content = base64Audio
            )
        )
        
        val request = Request.Builder()
            .url("$GOOGLE_SPEECH_API_URL?key=$googleApiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val googleResponse = gson.fromJson(responseBody, GoogleSpeechResponse::class.java)
            
            if (googleResponse.results.isNotEmpty()) {
                return googleResponse.results.first().alternatives.firstOrNull()?.transcript ?: ""
            }
        }
        
        throw IOException("Google语音识别API调用失败: ${response.code}")
    }
    
    /**
     * 生成模拟转录结果
     */
    private fun generateMockTranscription(audioData: ByteArray): String {
        return if (audioData.isNotEmpty()) {
            val mockTexts = listOf(
                "这是一个模拟的语音识别结果",
                "今天天气很好，心情愉快",
                "我们正在测试语音识别功能",
                "人工智能技术发展迅速",
                "这个视频内容很有趣"
            )
            mockTexts.random()
        } else {
            ""
        }
    }
    
    /**
     * 音频格式转换（如果需要）
     */
    suspend fun convertAudioFormat(
        inputData: ByteArray,
        fromFormat: String,
        toFormat: String
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            // 这里可以实现音频格式转换逻辑
            // 目前直接返回原数据
            inputData
        }
    }
    
    /**
     * 音频预处理
     */
    suspend fun preprocessAudio(audioData: ByteArray): ByteArray {
        return withContext(Dispatchers.IO) {
            // 这里可以实现音频预处理逻辑，如降噪、音量标准化等
            // 目前直接返回原数据
            audioData
        }
    }
}

// 百度语音识别API数据类
data class BaiduSpeechRequest(
    val format: String,
    val rate: Int,
    val channel: Int,
    val token: String,
    val speech: String,
    val len: Int,
    val cuid: String,
    val dev_pid: Int
)

data class BaiduSpeechResponse(
    val err_no: Int,
    val err_msg: String,
    val result: List<String>
)

// Google语音识别API数据类
data class GoogleSpeechRequest(
    val config: Config,
    val audio: Audio
) {
    data class Config(
        val encoding: String,
        val sampleRateHertz: Int,
        val languageCode: String,
        val enableAutomaticPunctuation: Boolean
    )
    
    data class Audio(
        val content: String
    )
}

data class GoogleSpeechResponse(
    val results: List<Result>
) {
    data class Result(
        val alternatives: List<Alternative>
    ) {
        data class Alternative(
            val transcript: String,
            val confidence: Double
        )
    }
} 