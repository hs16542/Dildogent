package com.hs16542.dildogent.llmutil

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hs16542.dildogent.llmutil.databinding.ActivityLlmutilDemoBinding
import kotlin.random.Random
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * LLMUtil模块演示Activity
 * 展示如何使用视频情感分析功能
 */
@androidx.media3.common.util.UnstableApi
class LLMUtilDemoActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLlmutilDemoBinding
    private lateinit var videoEmotionAnalyzer: VideoEmotionAnalyzer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLlmutilDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        initializeAnalyzer()
    }
    
    /**
     * 设置用户界面
     */
    private fun setupUI() {
        supportActionBar?.title = "LLMUtil 演示"
        
        // 设置按钮点击事件
        binding.btnStartDemo.setOnClickListener {
            startDemo()
        }
        
        binding.btnStopDemo.setOnClickListener {
            stopDemo()
        }
        
        binding.btnConfigureApi.setOnClickListener {
            showApiConfigurationDialog()
        }
    }
    
    /**
     * 初始化分析器
     */
    private fun initializeAnalyzer() {
        videoEmotionAnalyzer = VideoEmotionAnalyzer(this)
        videoEmotionAnalyzer.initializePlayer()
        
        // 观察状态变化
        observeStateChanges()
    }
    
    /**
     * 观察状态变化
     */
    private fun observeStateChanges() {
        lifecycleScope.launch {
            videoEmotionAnalyzer.analysisState.collectLatest { state ->
                updateStatusDisplay(state)
            }
        }
        
        lifecycleScope.launch {
            videoEmotionAnalyzer.currentEmotion.collectLatest { emotion ->
                updateEmotionDisplay(emotion)
            }
        }
        
        lifecycleScope.launch {
            videoEmotionAnalyzer.transcriptionText.collectLatest { text ->
                updateTranscriptionDisplay(text)
            }
        }
    }
    
    /**
     * 开始演示
     */
    private fun startDemo() {
        binding.btnStartDemo.isEnabled = false
        binding.btnStopDemo.isEnabled = true
        
        // 模拟视频分析过程
        simulateVideoAnalysis()
    }
    
    /**
     * 停止演示
     */
    private fun stopDemo() {
        binding.btnStartDemo.isEnabled = true
        binding.btnStopDemo.isEnabled = false
        
        videoEmotionAnalyzer.stopAnalysis()
        clearDisplay()
    }
    
    /**
     * 模拟视频分析过程
     */
    private fun simulateVideoAnalysis() {
        lifecycleScope.launch {
            // 模拟加载状态
            videoEmotionAnalyzer.loadVideo(android.net.Uri.parse("content://mock/video"))
            
            // 模拟分析过程
            repeat(5) { index ->
                kotlinx.coroutines.delay(2000) // 2秒间隔
                
                // 模拟情感分析结果
                val mockEmotion = EmotionResult(
                    emotion = listOf("喜悦", "悲伤", "愤怒", "恐惧", "惊讶", "厌恶", "中性").random(),
                    confidence = Random.nextDouble(0.7, 0.95).toFloat(),
                    intensity = Random.nextDouble(0.3, 0.9).toFloat(),
                    keywords = listOf("关键词1", "关键词2", "关键词3").take(Random.nextInt(1, 4)),
                    timestamp = System.currentTimeMillis()
                )
                
                // 模拟转录文本
                val mockTexts = listOf(
                    "这是一个模拟的语音识别结果",
                    "今天天气很好，心情愉快",
                    "我们正在测试语音识别功能",
                    "人工智能技术发展迅速",
                    "这个视频内容很有趣"
                )
                
                binding.tvDemoInfo.text = "模拟分析中... (${index + 1}/5)"
            }
            
            binding.tvDemoInfo.text = "演示完成！"
            binding.btnStartDemo.isEnabled = true
            binding.btnStopDemo.isEnabled = false
        }
    }
    
    /**
     * 更新状态显示
     */
    private fun updateStatusDisplay(state: AnalysisState) {
        val statusText = when (state) {
            is AnalysisState.Idle -> "就绪"
            is AnalysisState.Loading -> "加载中..."
            is AnalysisState.Analyzing -> "分析中..."
            is AnalysisState.Paused -> "已暂停"
            is AnalysisState.Error -> "错误"
        }
        
        binding.tvStatus.text = "状态: $statusText"
    }
    
    /**
     * 更新情感显示
     */
    private fun updateEmotionDisplay(emotion: EmotionResult?) {
        emotion?.let { result ->
            binding.tvEmotion.text = "情感: ${result.emotion}"
            binding.tvConfidence.text = "置信度: ${(result.confidence * 100).toInt()}%"
            binding.tvIntensity.text = "强度: ${(result.intensity * 100).toInt()}%"
            
            val keywordsText = if (result.keywords.isNotEmpty()) {
                "关键词: ${result.keywords.joinToString(", ")}"
            } else {
                "关键词: 无"
            }
            binding.tvKeywords.text = keywordsText
        } ?: run {
            binding.tvEmotion.text = "情感: --"
            binding.tvConfidence.text = "置信度: --"
            binding.tvIntensity.text = "强度: --"
            binding.tvKeywords.text = "关键词: --"
        }
    }
    
    /**
     * 更新转录显示
     */
    private fun updateTranscriptionDisplay(text: String) {
        if (text.isNotEmpty()) {
            binding.tvTranscription.text = "识别文本: $text"
            binding.tvTranscription.visibility = android.view.View.VISIBLE
        } else {
            binding.tvTranscription.visibility = android.view.View.GONE
        }
    }
    
    /**
     * 清除显示
     */
    private fun clearDisplay() {
        binding.tvDemoInfo.text = "点击开始演示按钮开始"
        binding.tvEmotion.text = "情感: --"
        binding.tvConfidence.text = "置信度: --"
        binding.tvIntensity.text = "强度: --"
        binding.tvKeywords.text = "关键词: --"
        binding.tvTranscription.visibility = android.view.View.GONE
    }
    
    /**
     * 显示API配置对话框
     */
    private fun showApiConfigurationDialog() {
        val options = arrayOf("配置 OpenAI API", "配置百度文心一言", "配置语音识别")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("API 配置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showOpenAIConfigDialog()
                    1 -> showBaiduConfigDialog()
                    2 -> showSpeechConfigDialog()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * 显示OpenAI配置对话框
     */
    private fun showOpenAIConfigDialog() {
        val input = android.widget.EditText(this)
        input.hint = "输入 OpenAI API 密钥"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("配置 OpenAI API")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val apiKey = input.text.toString()
                if (apiKey.isNotEmpty()) {
                    videoEmotionAnalyzer.llmServiceInternal.setOpenAIApiKey(apiKey)
                    Toast.makeText(this, "OpenAI API 配置成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示百度配置对话框
     */
    private fun showBaiduConfigDialog() {
        val input = android.widget.EditText(this)
        input.hint = "输入百度文心一言访问令牌"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("配置百度文心一言")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val accessToken = input.text.toString()
                if (accessToken.isNotEmpty()) {
                    videoEmotionAnalyzer.llmServiceInternal.setBaiduAccessToken(accessToken)
                    Toast.makeText(this, "百度文心一言配置成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示语音识别配置对话框
     */
    private fun showSpeechConfigDialog() {
        val options = arrayOf("配置百度语音识别", "配置 Google 语音识别")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("语音识别配置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBaiduSpeechConfigDialog()
                    1 -> showGoogleSpeechConfigDialog()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * 显示百度语音识别配置对话框
     */
    private fun showBaiduSpeechConfigDialog() {
        val input = android.widget.EditText(this)
        input.hint = "输入百度语音识别访问令牌"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("配置百度语音识别")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val accessToken = input.text.toString()
                if (accessToken.isNotEmpty()) {
                    videoEmotionAnalyzer.speechRecognitionService.setBaiduAccessToken(accessToken)
                    Toast.makeText(this, "百度语音识别配置成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示Google语音识别配置对话框
     */
    private fun showGoogleSpeechConfigDialog() {
        val input = android.widget.EditText(this)
        input.hint = "输入 Google API 密钥"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("配置 Google 语音识别")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val apiKey = input.text.toString()
                if (apiKey.isNotEmpty()) {
                    videoEmotionAnalyzer.speechRecognitionService.setGoogleApiKey(apiKey)
                    Toast.makeText(this, "Google 语音识别配置成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoEmotionAnalyzer.release()
    }
} 