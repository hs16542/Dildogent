package com.hs16542.dildogent.llmutil

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.hs16542.dildogent.llmutil.databinding.ActivityVideoEmotionAnalysisBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 视频情感分析Activity
 * 提供视频播放和实时情感分析的用户界面
 */
@androidx.media3.common.util.UnstableApi
class VideoEmotionAnalysisActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "VideoEmotionAnalysisActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private lateinit var binding: ActivityVideoEmotionAnalysisBinding
    private lateinit var videoEmotionAnalyzer: VideoEmotionAnalyzer
    
    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupVideoPlayer()
        } else {
            Toast.makeText(this, "需要相关权限才能使用此功能", Toast.LENGTH_LONG).show()
        }
    }
    
    // 文件选择
    private val selectVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { videoUri ->
            loadVideo(videoUri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEmotionAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }
    
    /**
     * 设置用户界面
     */
    private fun setupUI() {
        // 设置标题
        supportActionBar?.title = "视频情感分析"
        
        // 设置按钮点击事件
        binding.btnSelectVideo.setOnClickListener {
            selectVideoFile()
        }
        
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }
        
        binding.btnStop.setOnClickListener {
            stopVideo()
        }
        
        // 设置进度条
        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val position = (progress * videoEmotionAnalyzer.getDuration()) / 100
                    videoEmotionAnalyzer.seekTo(position)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 初始化视频情感分析器
        videoEmotionAnalyzer = VideoEmotionAnalyzer(this)
        videoEmotionAnalyzer.initializePlayer()
        
        // 观察状态变化
        observeStateChanges()
    }
    
    /**
     * 检查权限
     */
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            setupVideoPlayer()
        }
    }
    
    /**
     * 设置视频播放器
     */
    private fun setupVideoPlayer() {
        // 将ExoPlayer的PlayerView设置到布局中
        videoEmotionAnalyzer.exoPlayer?.let { player ->
            binding.playerView.player = player
        }
    }
    
    /**
     * 选择视频文件
     */
    private fun selectVideoFile() {
        selectVideoLauncher.launch("video/*")
    }
    
    /**
     * 加载视频
     */
    private fun loadVideo(videoUri: Uri) {
        try {
            videoEmotionAnalyzer.loadVideo(videoUri)
            binding.tvVideoInfo.text = "视频已加载: ${videoUri.lastPathSegment}"
            binding.btnPlayPause.isEnabled = true
            binding.btnStop.isEnabled = true
        } catch (e: Exception) {
            Log.e(TAG, "加载视频失败", e)
            Toast.makeText(this, "加载视频失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 切换播放/暂停
     */
    private fun togglePlayPause() {
        if (videoEmotionAnalyzer.isPlaying()) {
            videoEmotionAnalyzer.pausePlayback()
            binding.btnPlayPause.text = "播放"
        } else {
           // videoEmotionAnalyzer.resumePlayback()
            videoEmotionAnalyzer.startPlaybackAndAnalysis()
            binding.btnPlayPause.text = "暂停"
        }
    }
    
    /**
     * 停止视频
     */
    private fun stopVideo() {
        videoEmotionAnalyzer.stopAnalysis()
        binding.btnPlayPause.text = "播放"
        binding.btnPlayPause.isEnabled = false
        binding.btnStop.isEnabled = false
        binding.tvVideoInfo.text = "请选择视频文件"
        clearAnalysisResults()
    }
    
    /**
     * 观察状态变化
     */
    private fun observeStateChanges() {
        lifecycleScope.launch {
            videoEmotionAnalyzer.analysisState.collectLatest { state ->
                updateUIForState(state)
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
     * 根据状态更新UI
     */
    private fun updateUIForState(state: AnalysisState) {
        when (state) {
            is AnalysisState.Idle -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "就绪"
            }
            is AnalysisState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvStatus.text = "加载中..."
            }
            is AnalysisState.Analyzing -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "分析中..."
                binding.btnPlayPause.text = "暂停"
            }
            is AnalysisState.Paused -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "已暂停"
                binding.btnPlayPause.text = "播放"
            }
            is AnalysisState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "错误"
                Toast.makeText(this, "分析过程中出现错误", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * 更新情感显示
     */
    private fun updateEmotionDisplay(emotion: EmotionResultInternal?) {
        emotion?.let { result ->
            binding.tvEmotion.text = "情感: ${result.emotion}"
            binding.tvConfidence.text = "置信度: ${(result.confidence * 100).toInt()}%"
            binding.tvIntensity.text = "强度: ${(result.intensity * 100).toInt()}%"
            
            // 更新关键词显示
            val keywordsText = if (result.keywords.isNotEmpty()) {
                "关键词: ${result.keywords.joinToString(", ")}"
            } else {
                "关键词: 无"
            }
            binding.tvKeywords.text = keywordsText
            
            // 根据情感类型设置颜色
            setEmotionColor(result.emotion)
        } ?: run {
            binding.tvEmotion.text = "情感: 等待分析..."
            binding.tvConfidence.text = "置信度: --"
            binding.tvIntensity.text = "强度: --"
            binding.tvKeywords.text = "关键词: --"
            binding.tvEmotion.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }
    
    /**
     * 更新转录文本显示
     */
    private fun updateTranscriptionDisplay(text: String) {
        if (text.isNotEmpty()) {
            binding.tvTranscription.text = "识别文本: $text"
            binding.tvTranscription.visibility = View.VISIBLE
        } else {
            binding.tvTranscription.visibility = View.GONE
        }
    }
    
    /**
     * 设置情感颜色
     */
    private fun setEmotionColor(emotion: String) {
        val colorRes = when (emotion) {
            "喜悦" -> android.R.color.holo_green_light
            "悲伤" -> android.R.color.holo_blue_light
            "愤怒" -> android.R.color.holo_red_light
            "恐惧" -> android.R.color.holo_purple
            "惊讶" -> android.R.color.holo_orange_light
            "厌恶" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        binding.tvEmotion.setTextColor(ContextCompat.getColor(this, colorRes))
    }
    
    /**
     * 清除分析结果
     */
    private fun clearAnalysisResults() {
        binding.tvEmotion.text = "情感: --"
        binding.tvConfidence.text = "置信度: --"
        binding.tvIntensity.text = "强度: --"
        binding.tvKeywords.text = "关键词: --"
        binding.tvTranscription.visibility = View.GONE
        binding.tvEmotion.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoEmotionAnalyzer.release()
    }
} 