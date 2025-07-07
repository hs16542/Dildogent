package com.hs16542.dildogent.llmutil

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.random.Random

/**
 * 视频情感分析器
 * 功能：
 * 1. 视频播放控制
 * 2. 音频提取和分段
 * 3. 语音识别（文本转换）
 * 4. LLM情感分析
 * 5. 实时情感反馈
 */
@androidx.media3.common.util.UnstableApi
class VideoEmotionAnalyzer(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoEmotionAnalyzer"
        private const val AUDIO_SEGMENT_DURATION = 10000L // 10秒音频片段
        private const val EMOTION_ANALYSIS_INTERVAL = 5000L // 5秒分析间隔
    }
    
    // ExoPlayer用于视频播放
    private var _exoPlayer: ExoPlayer? = null
    
    // 协程作用域
    private val analyzerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 状态流
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()
    
    private val _currentEmotion = MutableStateFlow<EmotionResult?>(null)
    val currentEmotion: StateFlow<EmotionResult?> = _currentEmotion.asStateFlow()
    
    private val _transcriptionText = MutableStateFlow<String>("")
    val transcriptionText: StateFlow<String> = _transcriptionText.asStateFlow()
    
    // 音频处理器
    private val audioProcessor = AudioProcessor()
    
    // LLM服务
    val llmServiceInternal = LLMServiceInternal()
    
    // 语音识别服务
    val speechRecognitionService = SpeechRecognitionService()
    
    // 暴露exoPlayer供Activity使用
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer
    
    // 当前视频信息
    private var currentVideoUri: Uri? = null
    private var videoDuration: Long = 0L
    
    /**
     * 初始化播放器
     */
    fun initializePlayer() {
        _exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            videoDuration = duration
                            Log.d(TAG, "视频准备就绪，时长: ${duration}ms")
                        }
                        Player.STATE_ENDED -> {
                            stopAnalysis()
                            Log.d(TAG, "视频播放结束")
                        }
                    }
                }
                
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    // 处理播放位置变化
                    handlePositionChange(newPosition.positionMs)
                }
            })
        }
    }
    
    /**
     * 加载视频文件
     */
    fun loadVideo(videoUri: Uri) {
        currentVideoUri = videoUri
        _analysisState.value = AnalysisState.Loading
        
        _exoPlayer?.let { player ->
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
                .createMediaSource(MediaItem.fromUri(videoUri))
            
            player.setMediaSource(mediaSource)
            player.prepare()
            
            Log.d(TAG, "视频加载完成: $videoUri")
        }
    }
    
    /**
     * 开始播放和分析
     */
    fun startPlaybackAndAnalysis() {
        _exoPlayer?.play()
        _analysisState.value = AnalysisState.Analyzing
        
        // 启动实时分析
        startRealTimeAnalysis()
    }
    
    /**
     * 暂停播放
     */
    fun pausePlayback() {
        _exoPlayer?.pause()
        _analysisState.value = AnalysisState.Paused
    }
    
    /**
     * 恢复播放
     */
    fun resumePlayback() {
        _exoPlayer?.play()
        _analysisState.value = AnalysisState.Analyzing
    }
    
    /**
     * 停止播放和分析
     */
    fun stopAnalysis() {
        _exoPlayer?.stop()
        _analysisState.value = AnalysisState.Idle
        analyzerScope.cancel()
    }
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Long = _exoPlayer?.currentPosition ?: 0L
    
    /**
     * 获取视频总时长
     */
    fun getDuration(): Long = _exoPlayer?.duration ?: 0L
    
    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean = _exoPlayer?.isPlaying ?: false
    
    /**
     * 启动实时分析
     */
    private fun startRealTimeAnalysis() {
        analyzerScope.launch {
            while (isActive && _analysisState.value == AnalysisState.Analyzing) {
                try {
                    val currentPosition = getCurrentPosition()
                    
                    // 提取当前时间段的音频
                    val audioSegment = extractAudioSegment(currentPosition)
                    
                    // 语音识别
                    val transcription = speechRecognitionService.recognizeSpeech(audioSegment)
                    if (transcription.isNotEmpty()) {
                        _transcriptionText.value = transcription
                        
                        // LLM情感分析
                        val emotionResult = llmServiceInternal.analyzeEmotion(transcription)
                        _currentEmotion.value = emotionResult
                        
                        Log.d(TAG, "情感分析结果: $emotionResult")
                    }
                    
                    delay(EMOTION_ANALYSIS_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "实时分析出错", e)
                    delay(EMOTION_ANALYSIS_INTERVAL)
                }
            }
        }
    }
    
    /**
     * 提取音频片段
     */
    private suspend fun extractAudioSegment(startTimeMs: Long): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                currentVideoUri?.let { uri ->
                    audioProcessor.extractAudioSegment(context, uri, startTimeMs, AUDIO_SEGMENT_DURATION)
                } ?: ByteArray(0)
            } catch (e: Exception) {
                Log.e(TAG, "音频提取失败", e)
                ByteArray(0)
            }
        }
    }
    
    /**
     * 处理播放位置变化
     */
    private fun handlePositionChange(newPosition: Long) {
        // 可以在这里添加位置变化时的特殊处理逻辑
        Log.d(TAG, "播放位置变化: ${newPosition}ms")
    }
    
    /**
     * 释放资源
     */
    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        analyzerScope.cancel()
    }
}

/**
 * 分析状态
 */
sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    object Analyzing : AnalysisState()
    object Paused : AnalysisState()
    object Error : AnalysisState()
}

/**
 * 情感分析结果
 */
data class EmotionResult(
    val emotion: String,           // 主要情感
    val confidence: Float,         // 置信度
    val intensity: Float,          // 情感强度
    val keywords: List<String>,    // 关键词
    val timestamp: Long            // 时间戳
)

/**
 * 音频处理器
 */
class AudioProcessor {
    
    /**
     * 从视频中提取音频片段
     */
    suspend fun extractAudioSegment(
        context: Context,
        videoUri: Uri,
        startTimeMs: Long,
        durationMs: Long
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val outputBuffer = ByteBuffer.allocate(1024 * 1024) // 1MB缓冲区
            
            try {
                extractor.setDataSource(context, videoUri, null)
                
                // 查找音频轨道
                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        break
                    }
                }
                
                if (audioTrackIndex == -1) {
                    throw IllegalStateException("未找到音频轨道")
                }
                
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(startTimeMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                
                val endTimeUs = (startTimeMs + durationMs) * 1000
                val audioData = mutableListOf<Byte>()
                
                while (extractor.sampleTime < endTimeUs) {
                    val sampleSize = extractor.readSampleData(outputBuffer, 0)
                    if (sampleSize < 0) break
                    
                    val buffer = ByteArray(sampleSize)
                    outputBuffer.get(buffer)
                    audioData.addAll(buffer.toList())
                    
                    extractor.advance()
                }
                
                audioData.toByteArray()
            } finally {
                extractor.release()
            }
        }
    }
}

/**
 * LLM服务接口
 */
class LLMService {
    
    /**
     * 分析文本情感
     */
    suspend fun analyzeEmotion(text: String): EmotionResult {
        return withContext(Dispatchers.IO) {
            try {
                // 这里应该调用实际的LLM API
                // 目前返回模拟数据
                val emotions = listOf("喜悦", "悲伤", "愤怒", "恐惧", "惊讶", "厌恶", "中性")
                val randomEmotion = emotions.random()
                
                EmotionResult(
                    emotion = randomEmotion,
                    confidence = Random.nextDouble(0.7, 0.95).toFloat(),
                    intensity = Random.nextDouble(0.3, 0.9).toFloat(),
                    keywords = extractKeywords(text),
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("LLMService", "情感分析失败", e)
                EmotionResult(
                    emotion = "中性",
                    confidence = 0.5f,
                    intensity = 0.5f,
                    keywords = emptyList(),
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * 提取关键词
     */
    private fun extractKeywords(text: String): List<String> {
        // 简单的关键词提取逻辑
        val words = text.split(" ", "，", "。", "！", "？", "、")
        return words.filter { it.length > 1 }.take(5)
    }
}

 