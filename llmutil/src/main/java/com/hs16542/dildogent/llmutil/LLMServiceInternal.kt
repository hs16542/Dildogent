package com.hs16542.dildogent.llmutil

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * LLM服务接口
 * 支持多种LLM API的集成，包括在线API和离线模型
 */
class LLMServiceInternal(private val context: android.content.Context) {
    
    companion object {
        private const val TAG = "LLMService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val BAIDU_API_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // 离线模型管理器
    private val offlineModelManager = OfflineModelManager(context)
    
    // API密钥配置（实际使用时应该从配置文件或环境变量读取）
    private var openaiApiKey: String? = null
    private var baiduAccessToken: String? = null
    
    // 模型优先级配置
    private var useOfflineModelFirst = false
    
    /**
     * 设置OpenAI API密钥
     */
    fun setOpenAIApiKey(apiKey: String) {
        this.openaiApiKey = apiKey
    }
    
    /**
     * 设置百度文心一言访问令牌
     */
    fun setBaiduAccessToken(accessToken: String) {
        this.baiduAccessToken = accessToken
    }
    
    /**
     * 设置是否优先使用离线模型
     */
    fun setUseOfflineModelFirst(useOfflineFirst: Boolean) {
        this.useOfflineModelFirst = useOfflineFirst
    }
    
    /**
     * 加载TensorFlow Lite模型
     */
    suspend fun loadTensorFlowLiteModel(modelPath: String): Boolean {
        return offlineModelManager.loadTensorFlowLiteModel(modelPath)
    }
    
    /**
     * 加载ONNX模型
     */
    suspend fun loadOnnxModel(modelPath: String): Boolean {
        return offlineModelManager.loadOnnxModel(modelPath)
    }
    
    /**
     * 检查离线模型是否已加载
     */
    fun isOfflineModelLoaded(): Boolean {
        return offlineModelManager.isModelLoaded()
    }
    
    /**
     * 获取当前离线模型类型
     */
    fun getCurrentOfflineModelType(): OfflineModelManager.Companion.ModelType? {
        return offlineModelManager.getCurrentModelType()
    }
    
    /**
     * 分析文本情感
     */
    suspend fun analyzeEmotion(text: String): EmotionResultInternal {
        return withContext(Dispatchers.IO) {
            try {
                // 根据配置选择分析方式
                when {
                    useOfflineModelFirst && offlineModelManager.isModelLoaded() -> {
                        Log.d(TAG, "使用离线模型进行情感分析")
                        offlineModelManager.analyzeEmotionOffline(text)
                    }
                    !openaiApiKey.isNullOrEmpty() -> {
                        Log.d(TAG, "使用OpenAI API进行情感分析")
                        analyzeEmotionWithOpenAI(text)
                    }
                    !baiduAccessToken.isNullOrEmpty() -> {
                        Log.d(TAG, "使用百度文心一言进行情感分析")
                        analyzeEmotionWithBaidu(text)
                    }
                    offlineModelManager.isModelLoaded() -> {
                        Log.d(TAG, "使用离线模型进行情感分析（后备方案）")
                        offlineModelManager.analyzeEmotionOffline(text)
                    }
                    else -> {
                        Log.w(TAG, "未配置任何模型，使用模拟数据")
                        generateMockEmotionResult(text)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "情感分析失败，尝试使用离线模型", e)
                try {
                    if (offlineModelManager.isModelLoaded()) {
                        offlineModelManager.analyzeEmotionOffline(text)
                    } else {
                        generateMockEmotionResult(text)
                    }
                } catch (offlineException: Exception) {
                    Log.e(TAG, "离线模型也失败，使用模拟数据", offlineException)
                    generateMockEmotionResult(text)
                }
            }
        }
    }
    
    /**
     * 使用OpenAI分析情感
     */
    private suspend fun analyzeEmotionWithOpenAI(text: String): EmotionResultInternal {
        val prompt = """
            请分析以下中文文本的情感倾向，并返回JSON格式的结果：
            
            文本：$text
            
            请返回以下格式的JSON：
            {
                "emotion": "主要情感（喜悦/悲伤/愤怒/恐惧/惊讶/厌恶/中性）",
                "confidence": 置信度（0.0-1.0之间的浮点数）,
                "intensity": 情感强度（0.0-1.0之间的浮点数）,
                "keywords": ["关键词1", "关键词2", "关键词3"]
            }
        """.trimIndent()
        
        val requestBody = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIRequest.Message(
                    role = "system",
                    content = "你是一个专业的情感分析助手，专门分析中文文本的情感倾向。"
                ),
                OpenAIRequest.Message(
                    role = "user",
                    content = prompt
                )
            ),
            temperature = 0.3
        )
        
        val request = Request.Builder()
            .url(OPENAI_API_URL)
            .addHeader("Authorization", "Bearer $openaiApiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
            val content = openAIResponse.choices.firstOrNull()?.message?.content
            
            if (!content.isNullOrEmpty()) {
                try {
                    // 尝试解析JSON响应
                    val emotionData = gson.fromJson(content, EmotionData::class.java)
                    return EmotionResultInternal(
                        emotion = emotionData.emotion,
                        confidence = emotionData.confidence,
                        intensity = emotionData.intensity,
                        keywords = emotionData.keywords,
                        timestamp = System.currentTimeMillis(),
                        modelType = "OpenAI GPT"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "解析OpenAI响应失败，使用文本解析", e)
                    return parseEmotionFromText(content, text)
                }
            }
        }
        
        throw IOException("OpenAI API调用失败: ${response.code}")
    }
    
    /**
     * 使用百度文心一言分析情感
     */
    private suspend fun analyzeEmotionWithBaidu(text: String): EmotionResultInternal {
        val prompt = """
            请分析以下中文文本的情感倾向：
            
            $text
            
            请以JSON格式返回分析结果，包含情感类型、置信度、强度和关键词。
        """.trimIndent()
        
        val requestBody = BaiduRequest(
            messages = listOf(
                BaiduRequest.Message(
                    role = "user",
                    content = prompt
                )
            )
        )
        
        val request = Request.Builder()
            .url("$BAIDU_API_URL?access_token=$baiduAccessToken")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val baiduResponse = gson.fromJson(responseBody, BaiduResponse::class.java)
            val content = baiduResponse.result
            
            if (!content.isNullOrEmpty()) {
                try {
                    val emotionData = gson.fromJson(content, EmotionData::class.java)
                    return EmotionResultInternal(
                        emotion = emotionData.emotion,
                        confidence = emotionData.confidence,
                        intensity = emotionData.intensity,
                        keywords = emotionData.keywords,
                        timestamp = System.currentTimeMillis(),
                        modelType = "百度文心一言"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "解析百度响应失败，使用文本解析", e)
                    return parseEmotionFromText(content, text)
                }
            }
        }
        
        throw IOException("百度API调用失败: ${response.code}")
    }
    
    /**
     * 从文本中解析情感信息
     */
    private fun parseEmotionFromText(llmResponse: String, originalText: String): EmotionResultInternal {
        val emotionKeywords = mapOf(
            "喜悦" to listOf("开心", "快乐", "高兴", "兴奋", "愉快", "欢乐"),
            "悲伤" to listOf("难过", "伤心", "痛苦", "沮丧", "失望", "悲伤"),
            "愤怒" to listOf("生气", "愤怒", "恼火", "气愤", "暴怒", "愤怒"),
            "恐惧" to listOf("害怕", "恐惧", "担心", "焦虑", "紧张", "恐慌"),
            "惊讶" to listOf("惊讶", "震惊", "意外", "吃惊", "惊奇", "诧异"),
            "厌恶" to listOf("恶心", "厌恶", "讨厌", "反感", "嫌弃", "憎恶")
        )
        
        // 简单的关键词匹配
        var detectedEmotion = "中性"
        var maxConfidence = 0.0f
        
        for ((emotion, keywords) in emotionKeywords) {
            val matchCount = keywords.count { keyword ->
                llmResponse.contains(keyword) || originalText.contains(keyword)
            }
            val confidence = matchCount.toFloat() / keywords.size
            if (confidence > maxConfidence) {
                maxConfidence = confidence
                detectedEmotion = emotion
            }
        }
        
        return EmotionResultInternal(
            emotion = detectedEmotion,
            confidence = maxConfidence.coerceAtLeast(0.3f),
            intensity = maxConfidence.coerceAtLeast(0.3f),
            keywords = extractKeywords(originalText),
            timestamp = System.currentTimeMillis(),
            modelType = "文本解析"
        )
    }
    
    /**
     * 生成模拟情感结果
     */
    private fun generateMockEmotionResult(text: String): EmotionResultInternal {
        val emotions = listOf("喜悦", "悲伤", "愤怒", "恐惧", "惊讶", "厌恶", "中性")
        val randomEmotion = emotions.random()
        
        return EmotionResultInternal(
            emotion = randomEmotion,
            confidence = Random.nextDouble(0.7, 0.95).toFloat(),
            intensity = Random.nextDouble(0.3, 0.9).toFloat(),
            keywords = extractKeywords(text),
            timestamp = System.currentTimeMillis(),
            modelType = "模拟数据"
        )
    }
    
    /**
     * 提取关键词
     */
    private fun extractKeywords(text: String): List<String> {
        // 简单的关键词提取逻辑 - 不依赖外部库
        val words = text.split(" ", "，", "。", "！", "？", "、", "；", "：", "\n", "\t")
        return words.filter { 
            it.length > 1 && it.length <= 10 && 
            !it.matches(Regex("^[\\s\\p{Punct}]+$")) // 过滤纯标点符号
        }.take(5)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        offlineModelManager.release()
    }
}

// 数据类定义

data class EmotionData(
    val emotion: String,
    val confidence: Float,
    val intensity: Float,
    val keywords: List<String>
)

data class EmotionResultInternal(
    val emotion: String,
    val confidence: Float,
    val intensity: Float,
    val keywords: List<String>,
    val timestamp: Long,
    val modelType: String = "未知"
)

// OpenAI API 请求/响应类
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double
) {
    data class Message(
        val role: String,
        val content: String
    )
}

data class OpenAIResponse(
    val choices: List<Choice>
) {
    data class Choice(
        val message: Message
    ) {
        data class Message(
            val content: String
        )
    }
}

// 百度文心一言 API 请求/响应类
data class BaiduRequest(
    val messages: List<Message>
) {
    data class Message(
        val role: String,
        val content: String
    )
}

data class BaiduResponse(
    val result: String
) 