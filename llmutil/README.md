# LLMUtil 模块

## 概述

LLMUtil 是一个专门用于视频情感内容实时分析的 Android 模块。该模块能够跟随播放的汉语视频文件，通过音频提取、语音识别和 LLM 分析，实现视频情感内容的实时分析。

## 主要功能

### 1. 视频播放控制
- 支持多种视频格式的播放
- 基于 ExoPlayer 的高性能播放器
- 播放控制（播放、暂停、停止、跳转）

### 2. 音频提取和处理
- 从视频中实时提取音频片段
- 音频格式转换和预处理
- 支持多种音频格式

### 3. 语音识别
- 支持百度语音识别 API
- 支持 Google 语音识别 API
- 中文语音识别优化
- 实时语音转文本

### 4. LLM 情感分析
- 支持 OpenAI GPT 系列模型
- 支持百度文心一言
- 实时情感倾向分析
- 情感强度评估
- 关键词提取

### 5. 实时反馈
- 实时显示分析结果
- 情感可视化展示
- 关键词高亮显示
- 置信度指标

## 架构设计

```
VideoEmotionAnalyzer (核心分析器)
├── ExoPlayer (视频播放)
├── AudioProcessor (音频处理)
├── SpeechRecognitionService (语音识别)
└── LLMService (情感分析)
```

## 使用方法

### 1. 基本使用

```kotlin
// 创建分析器
val analyzer = VideoEmotionAnalyzer(context)
analyzer.initializePlayer()

// 加载视频
analyzer.loadVideo(videoUri)

// 开始播放和分析
analyzer.startPlaybackAndAnalysis()

// 观察结果
lifecycleScope.launch {
    analyzer.currentEmotion.collectLatest { emotion ->
        // 处理情感分析结果
        println("情感: ${emotion.emotion}")
        println("置信度: ${emotion.confidence}")
        println("强度: ${emotion.intensity}")
        println("关键词: ${emotion.keywords}")
    }
}
```

### 2. 配置 API 密钥

```kotlin
// 配置 OpenAI API
val llmService = LLMService()
llmService.setOpenAIApiKey("your-openai-api-key")

// 配置百度文心一言
llmService.setBaiduAccessToken("your-baidu-access-token")

// 配置语音识别
val speechService = SpeechRecognitionService()
speechService.setBaiduAccessToken("your-baidu-speech-token")
speechService.setGoogleApiKey("your-google-api-key")
```

### 3. 使用 Activity

```kotlin
// 启动视频情感分析 Activity
val intent = Intent(this, VideoEmotionAnalysisActivity::class.java)
startActivity(intent)
```

## API 配置

### OpenAI API 配置

1. 注册 OpenAI 账号并获取 API 密钥
2. 在代码中设置 API 密钥：
```kotlin
llmService.setOpenAIApiKey("sk-your-api-key")
```

### 百度文心一言配置

1. 注册百度智能云账号
2. 开通文心一言服务
3. 获取访问令牌：
```kotlin
llmService.setBaiduAccessToken("your-access-token")
```

### 百度语音识别配置

1. 开通百度语音识别服务
2. 获取访问令牌：
```kotlin
speechService.setBaiduAccessToken("your-speech-token")
```

### Google 语音识别配置

1. 创建 Google Cloud 项目
2. 启用 Speech-to-Text API
3. 创建 API 密钥：
```kotlin
speechService.setGoogleApiKey("your-google-api-key")
```

## 权限要求

在 `AndroidManifest.xml` 中添加以下权限：

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 音频录制权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- 文件读写权限 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- 麦克风权限 -->
<uses-permission android:name="android.permission.MICROPHONE" />
```

## 依赖项

```kotlin
dependencies {
    // 视频播放
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    
    // 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    
    // JSON 解析
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## 性能优化

### 1. 音频分段处理
- 默认每 10 秒提取一次音频片段
- 可根据需要调整 `AUDIO_SEGMENT_DURATION`

### 2. 分析间隔控制
- 默认每 5 秒进行一次情感分析
- 可通过 `EMOTION_ANALYSIS_INTERVAL` 调整

### 3. 内存管理
- 及时释放 ExoPlayer 资源
- 使用协程避免阻塞主线程
- 合理控制音频缓冲区大小

## 错误处理

模块内置了完善的错误处理机制：

1. **网络错误**：自动重试，降级到模拟数据
2. **API 错误**：记录日志，返回默认结果
3. **音频提取错误**：跳过当前片段，继续处理
4. **权限错误**：提示用户授权

## 扩展功能

### 1. 自定义情感分析模型
```kotlin
class CustomLLMService : LLMService() {
    override suspend fun analyzeEmotion(text: String): EmotionResult {
        // 实现自定义分析逻辑
    }
}
```

### 2. 自定义语音识别
```kotlin
class CustomSpeechService : SpeechRecognitionService() {
    override suspend fun recognizeSpeech(audioData: ByteArray): String {
        // 实现自定义识别逻辑
    }
}
```

### 3. 情感历史记录
```kotlin
// 添加历史记录功能
val emotionHistory = mutableListOf<EmotionResult>()
analyzer.currentEmotion.collectLatest { emotion ->
    emotion?.let { emotionHistory.add(it) }
}
```

## 注意事项

1. **API 限制**：注意各 API 的调用频率和配额限制
2. **网络环境**：确保网络连接稳定，建议在 WiFi 环境下使用
3. **音频质量**：音频质量会影响语音识别准确率
4. **隐私保护**：音频数据会发送到第三方服务，注意隐私保护
5. **成本控制**：API 调用会产生费用，注意控制使用量

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基本的视频情感分析功能
- 集成 OpenAI 和百度文心一言
- 支持百度语音识别

## 许可证

本项目采用 MIT 许可证，详见 LICENSE 文件。 