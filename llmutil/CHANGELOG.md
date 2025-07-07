# LLMUtil 模块更新日志

## v1.0.0 (2024-01-XX)

### 新增功能
- 🎯 **视频情感分析器** (`VideoEmotionAnalyzer`)
  - 基于 ExoPlayer 的视频播放控制
  - 实时音频提取和处理
  - 支持多种视频格式

- 🗣️ **语音识别服务** (`SpeechRecognitionService`)
  - 支持百度语音识别 API
  - 支持 Google 语音识别 API
  - 中文语音识别优化
  - 音频格式转换和预处理

- 🧠 **LLM 情感分析服务** (`LLMService`)
  - 支持 OpenAI GPT 系列模型
  - 支持百度文心一言
  - 实时情感倾向分析
  - 情感强度评估和关键词提取

- 📱 **用户界面**
  - 视频情感分析 Activity (`VideoEmotionAnalysisActivity`)
  - LLMUtil 演示 Activity (`LLMUtilDemoActivity`)
  - 现代化的 Material Design 界面
  - 实时状态反馈和结果展示

### 技术特性
- 🔄 **响应式架构**
  - 基于 Kotlin Flow 的状态管理
  - 协程异步处理
  - 实时数据流

- 🛡️ **错误处理**
  - 完善的异常处理机制
  - API 调用失败降级
  - 网络错误重试

- ⚡ **性能优化**
  - 音频分段处理
  - 内存管理优化
  - 后台线程处理

### 配置支持
- 🔑 **API 配置**
  - OpenAI API 密钥配置
  - 百度文心一言访问令牌
  - 百度语音识别访问令牌
  - Google 语音识别 API 密钥

### 文档
- 📖 **完整文档**
  - 详细的 README 文档
  - API 使用说明
  - 配置指南
  - 示例代码

### 测试
- 🧪 **单元测试**
  - VideoEmotionAnalyzer 测试
  - 数据类测试
  - 状态管理测试

### 依赖项
- 📦 **核心依赖**
  - androidx.media3:media3-exoplayer:1.2.1
  - androidx.media3:media3-ui:1.2.1
  - com.squareup.okhttp3:okhttp:4.12.0
  - com.squareup.retrofit2:retrofit:2.9.0
  - com.google.code.gson:gson:2.10.1
  - org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3

### 权限要求
- 📋 **必要权限**
  - INTERNET
  - ACCESS_NETWORK_STATE
  - RECORD_AUDIO
  - MODIFY_AUDIO_SETTINGS
  - READ_EXTERNAL_STORAGE
  - WRITE_EXTERNAL_STORAGE
  - MICROPHONE

---

## 计划中的功能

### v1.1.0
- [ ] 支持更多 LLM 模型（阿里通义千问、讯飞星火等）
- [ ] 本地语音识别支持
- [ ] 情感历史记录和分析趋势
- [ ] 批量视频处理功能

### v1.2.0
- [ ] 视频画面情感分析（基于图像识别）
- [ ] 多语言支持
- [ ] 自定义情感分析模型
- [ ] 云端同步和分析结果分享

### v2.0.0
- [ ] 实时直播情感分析
- [ ] 情感预警系统
- [ ] 高级数据可视化
- [ ] 企业级功能支持 