package com.hs16542.dildogent.llmutil

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File

/**
 * 离线模型管理器
 * 支持TensorFlow Lite、ONNX Runtime等多种离线模型框架
 */
class OfflineModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineModelManager"
        
        // 模型类型枚举
        enum class ModelType {
            TENSORFLOW_LITE,
            ONNX_RUNTIME,
            HUGGING_FACE_ONNX
        }
        
        // 情感标签
        private val EMOTION_LABELS = listOf("喜悦", "悲伤", "愤怒", "恐惧", "惊讶", "厌恶", "中性")
    }
    
    private var tfliteInterpreter: Interpreter? = null
    private var onnxSession: OrtSession? = null
    private var currentModelType: ModelType? = null
    private var modelLoaded = false
    
    /**
     * 加载TensorFlow Lite模型
     */
    suspend fun loadTensorFlowLiteModel(modelPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(context.filesDir, modelPath)
                if (!modelFile.exists()) {
                    Log.e(TAG, "模型文件不存在: ${modelFile.absolutePath}")
                    return@withContext false
                }
                
                val options = Interpreter.Options()
                options.setNumThreads(4)
                
                // 尝试使用GPU加速（可选）
                try {
                    // 检查GPU委托是否可用
                    val gpuDelegateClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
                    val gpuDelegate = gpuDelegateClass.getDeclaredConstructor().newInstance()
                    options.addDelegate(gpuDelegate as Delegate)
                    Log.d(TAG, "启用GPU加速")
                } catch (e: Exception) {
                    Log.d(TAG, "GPU加速不可用，使用CPU: ${e.message}")
                }
                
                tfliteInterpreter = Interpreter(modelFile, options)
                currentModelType = ModelType.TENSORFLOW_LITE
                modelLoaded = true
                
                Log.d(TAG, "TensorFlow Lite模型加载成功")
                true
            } catch (e: Exception) {
                Log.e(TAG, "加载TensorFlow Lite模型失败", e)
                false
            }
        }
    }
    
    /**
     * 加载ONNX模型
     */
    suspend fun loadOnnxModel(modelPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(context.filesDir, modelPath)
                if (!modelFile.exists()) {
                    Log.e(TAG, "模型文件不存在: ${modelFile.absolutePath}")
                    return@withContext false
                }
                
                val env = OrtEnvironment.getEnvironment()
                val sessionOptions = SessionOptions()
                sessionOptions.setIntraOpNumThreads(4)
                sessionOptions.setInterOpNumThreads(4)
                
                onnxSession = env.createSession(modelFile.absolutePath, sessionOptions)
                currentModelType = ModelType.ONNX_RUNTIME
                modelLoaded = true
                
                Log.d(TAG, "ONNX模型加载成功")
                true
            } catch (e: Exception) {
                Log.e(TAG, "加载ONNX模型失败", e)
                false
            }
        }
    }
    
    /**
     * 使用离线模型进行情感分析
     */
    suspend fun analyzeEmotionOffline(text: String): EmotionResultInternal {
        return withContext(Dispatchers.IO) {
            if (!modelLoaded) {
                Log.w(TAG, "模型未加载，使用规则基础分析")
                return@withContext analyzeEmotionWithRules(text)
            }
            
            try {
                when (currentModelType) {
                    ModelType.TENSORFLOW_LITE -> analyzeEmotionWithTensorFlowLite(text)
                    ModelType.ONNX_RUNTIME -> analyzeEmotionWithOnnx(text)
                    else -> analyzeEmotionWithRules(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "离线模型分析失败，回退到规则分析", e)
                analyzeEmotionWithRules(text)
            }
        }
    }
    
    /**
     * 使用TensorFlow Lite进行情感分析
     */
    private fun analyzeEmotionWithTensorFlowLite(text: String): EmotionResultInternal {
        val interpreter = tfliteInterpreter ?: throw IllegalStateException("TensorFlow Lite解释器未初始化")
        
        // 文本预处理
        val inputTensor = preprocessTextForTensorFlowLite(text)
        
        // 输出缓冲区
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, EMOTION_LABELS.size), org.tensorflow.lite.DataType.FLOAT32)
        
        // 运行推理
        interpreter.run(inputTensor.buffer, outputBuffer.buffer)
        
        // 后处理
        val probabilities = outputBuffer.floatArray
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        
        return EmotionResultInternal(
            emotion = EMOTION_LABELS[maxIndex],
            confidence = probabilities[maxIndex],
            intensity = probabilities[maxIndex],
            keywords = extractKeywords(text),
            timestamp = System.currentTimeMillis(),
            modelType = "TensorFlow Lite"
        )
    }
    
    /**
     * 使用ONNX Runtime进行情感分析
     */
    private fun analyzeEmotionWithOnnx(text: String): EmotionResultInternal {
        val session = onnxSession ?: throw IllegalStateException("ONNX会话未初始化")
        
        // 文本预处理
        val inputTensor = preprocessTextForOnnx(text)
        
        // 创建输入
        val inputName = session.inputNames.iterator().next()
        val input = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            inputTensor
        )
        
        // 运行推理
        val output = session.run(mapOf(inputName to input))
        
        // 获取输出
        val outputName = session.outputNames.iterator().next()
        val outputTensor = output[outputName] as OnnxTensor
        val outputData = outputTensor.floatBuffer
        
        // 后处理
        val probabilities = FloatArray(EMOTION_LABELS.size)
        outputData.get(probabilities)
        
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        
        return EmotionResultInternal(
            emotion = EMOTION_LABELS[maxIndex],
            confidence = probabilities[maxIndex],
            intensity = probabilities[maxIndex],
            keywords = extractKeywords(text),
            timestamp = System.currentTimeMillis(),
            modelType = "ONNX Runtime"
        )
    }
    
    /**
     * 规则基础的情感分析（作为后备方案）
     */
    private fun analyzeEmotionWithRules(text: String): EmotionResultInternal {
        val emotionKeywords = mapOf(
            "喜悦" to listOf("开心", "快乐", "高兴", "兴奋", "愉快", "欢乐", "笑", "喜", "乐"),
            "悲伤" to listOf("难过", "伤心", "痛苦", "沮丧", "失望", "悲伤", "哭", "泪", "愁"),
            "愤怒" to listOf("生气", "愤怒", "恼火", "气愤", "暴怒", "愤怒", "怒", "火", "气"),
            "恐惧" to listOf("害怕", "恐惧", "担心", "焦虑", "紧张", "恐慌", "怕", "恐", "惊"),
            "惊讶" to listOf("惊讶", "震惊", "意外", "吃惊", "惊奇", "诧异", "惊", "奇", "异"),
            "厌恶" to listOf("恶心", "厌恶", "讨厌", "反感", "嫌弃", "憎恶", "恶", "厌", "嫌")
        )
        
        var detectedEmotion = "中性"
        var maxConfidence = 0.0f
        
        for ((emotion, keywords) in emotionKeywords) {
            val matchCount = keywords.count { keyword ->
                text.contains(keyword)
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
            keywords = extractKeywords(text),
            timestamp = System.currentTimeMillis(),
            modelType = "规则基础"
        )
    }
    
    /**
     * 为TensorFlow Lite预处理文本
     */
    private fun preprocessTextForTensorFlowLite(text: String): TensorBuffer {
        // 简化的文本预处理：将文本转换为数值特征
        val features = FloatArray(128) { 0.0f } // 假设输入维度为128
        
        // 简单的字符级特征提取
        text.forEachIndexed { index, char ->
            if (index < features.size) {
                features[index] = char.code.toFloat() / 65535.0f // 归一化字符编码
            }
        }
        
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 128), org.tensorflow.lite.DataType.FLOAT32)
        inputBuffer.loadArray(features)
        
        return inputBuffer
    }
    
    /**
     * 为ONNX预处理文本
     */
    private fun preprocessTextForOnnx(text: String): FloatArray {
        // 简化的文本预处理
        val features = FloatArray(128) { 0.0f }
        
        text.forEachIndexed { index, char ->
            if (index < features.size) {
                features[index] = char.code.toFloat() / 65535.0f
            }
        }
        
        return features
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
     * 检查模型是否已加载
     */
    fun isModelLoaded(): Boolean = modelLoaded
    
    /**
     * 获取当前模型类型
     */
    fun getCurrentModelType(): ModelType? = currentModelType
    
    /**
     * 释放资源
     */
    fun release() {
        tfliteInterpreter?.close()
        onnxSession?.close()
        tfliteInterpreter = null
        onnxSession = null
        modelLoaded = false
        currentModelType = null
    }
} 