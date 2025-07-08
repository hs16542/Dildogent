# 离线模型情感分析功能

本项目现在支持端侧离线模型进行情感分析，除了原有的OpenAI和百度文心一言API外，还可以使用TensorFlow Lite和ONNX Runtime等离线模型框架。

## 功能特性

### 支持的模型类型
- **TensorFlow Lite**: 轻量级模型，适合移动端部署
- **ONNX Runtime**: 跨平台模型推理引擎
- **规则基础分析**: 作为后备方案的关键词匹配分析

### 主要功能
- 离线模型下载和管理
- 自动模型加载和推理
- 多模型优先级配置
- 后台下载支持
- GPU加速支持（TensorFlow Lite）

## 快速开始

### 1. 初始化服务

```kotlin
// 创建LLM服务实例
val llmService = LLMServiceInternal(context)

// 设置优先使用离线模型
llmService.setUseOfflineModelFirst(true)
```

### 2. 下载和加载模型

```kotlin
// 创建模型下载管理器
val modelDownloadManager = ModelDownloadManager(context)

// 下载模型
lifecycleScope.launch {
    val modelConfig = ModelDownloadManager.AVAILABLE_MODELS.first()
    val success = modelDownloadManager.downloadModel(modelConfig) { progress ->
        // 处理下载进度
        Log.d("Download", "Progress: ${progress * 100}%")
    }
    
    if (success) {
        // 加载模型
        val modelFile = modelDownloadManager.getDownloadedModels().first()
        val loaded = llmService.loadTensorFlowLiteModel(modelFile.name)
        if (loaded) {
            Log.d("Model", "模型加载成功")
        }
    }
}
```

### 3. 进行情感分析

```kotlin
lifecycleScope.launch {
    val result = llmService.analyzeEmotion("我今天很开心！")
    
    Log.d("Analysis", "情感: ${result.emotion}")
    Log.d("Analysis", "置信度: ${result.confidence}")
    Log.d("Analysis", "强度: ${result.intensity}")
    Log.d("Analysis", "关键词: ${result.keywords}")
    Log.d("Analysis", "模型类型: ${result.modelType}")
}
```

## 模型优先级

系统按以下优先级选择分析方式：

1. **离线模型优先**: 如果设置了`useOfflineModelFirst = true`且模型已加载
2. **OpenAI API**: 如果配置了OpenAI API密钥
3. **百度文心一言**: 如果配置了百度访问令牌
4. **离线模型后备**: 如果模型已加载但未设置优先
5. **规则基础分析**: 作为最后的后备方案

## 模型配置

### 预定义模型

在`ModelDownloadManager.AVAILABLE_MODELS`中配置可用的模型：

```kotlin
val AVAILABLE_MODELS = listOf(
    ModelConfig(
        name = "emotion_analysis_tflite",
        url = "https://your-server.com/models/emotion_analysis.tflite",
        type = OfflineModelManager.ModelType.TENSORFLOW_LITE,
        version = "1.0.0",
        size = 1024 * 1024 * 10, // 10MB
        description = "基于TensorFlow Lite的情感分析模型"
    ),
    ModelConfig(
        name = "emotion_analysis_onnx",
        url = "https://your-server.com/models/emotion_analysis.onnx",
        type = OfflineModelManager.ModelType.ONNX_RUNTIME,
        version = "1.0.0",
        size = 1024 * 1024 * 15, // 15MB
        description = "基于ONNX的情感分析模型"
    )
)
```

### 自定义模型

您也可以添加自己的模型配置：

```kotlin
val customModel = ModelConfig(
    name = "my_emotion_model",
    url = "https://your-server.com/models/my_model.tflite",
    type = OfflineModelManager.ModelType.TENSORFLOW_LITE,
    version = "1.0.0",
    size = 1024 * 1024 * 5, // 5MB
    description = "我的自定义情感分析模型"
)

modelDownloadManager.downloadModel(customModel)
```

## 模型格式要求

### TensorFlow Lite模型
- 输入: 1x128的浮点数组（文本特征）
- 输出: 1x7的浮点数组（7种情感的概率分布）
- 支持的情感标签: ["喜悦", "悲伤", "愤怒", "恐惧", "惊讶", "厌恶", "中性"]

### ONNX模型
- 输入: 128维浮点数组
- 输出: 7维浮点数组
- 支持GPU加速

## 性能优化

### GPU加速
TensorFlow Lite模型会自动尝试使用GPU加速：

```kotlin
val options = Interpreter.Options()
try {
    val gpuDelegate = org.tensorflow.lite.gpu.GpuDelegate()
    options.addDelegate(gpuDelegate)
    Log.d("GPU", "启用GPU加速")
} catch (e: Exception) {
    Log.w("GPU", "GPU加速不可用，使用CPU")
}
```

### 多线程支持
ONNX Runtime支持多线程推理：

```kotlin
val sessionOptions = com.microsoft.onnxruntime.OrtSession.SessionOptions()
sessionOptions.setIntraOpNumThreads(4)
sessionOptions.setInterOpNumThreads(4)
```

## 后台下载

使用WorkManager进行后台模型下载：

```kotlin
// 调度后台下载
modelDownloadManager.scheduleModelDownload(modelConfig)

// 监听下载状态
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWorkLiveData("download_${modelConfig.name}")
    .observe(this) { workInfos ->
        workInfos.forEach { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> Log.d("Download", "下载完成")
                WorkInfo.State.FAILED -> Log.e("Download", "下载失败")
                WorkInfo.State.RUNNING -> Log.d("Download", "下载中")
            }
        }
    }
```

## 错误处理

### 网络错误
```kotlin
try {
    val result = llmService.analyzeEmotion(text)
} catch (e: Exception) {
    // 系统会自动尝试使用离线模型
    Log.e("Analysis", "在线API失败，使用离线模型", e)
}
```

### 模型加载错误
```kotlin
val success = llmService.loadTensorFlowLiteModel(modelPath)
if (!success) {
    // 模型加载失败，使用规则基础分析
    Log.w("Model", "模型加载失败，使用规则分析")
}
```

## 存储管理

### 检查存储空间
```kotlin
val availableSpace = modelDownloadManager.getAvailableStorage()
val requiredSpace = modelConfig.size

if (availableSpace < requiredSpace) {
    Log.w("Storage", "存储空间不足")
}
```

### 清理模型
```kotlin
// 删除特定模型
modelDownloadManager.deleteModel("emotion_analysis_tflite_v1.0.0")

// 清理临时文件
modelDownloadManager.cleanupTempFiles()
```

## 演示应用

运行`OfflineModelDemoActivity`查看完整的使用示例：

```kotlin
val intent = Intent(this, OfflineModelDemoActivity::class.java)
startActivity(intent)
```

## 注意事项

1. **模型文件大小**: 确保模型文件大小合理，避免占用过多存储空间
2. **网络权限**: 下载模型需要网络权限
3. **存储权限**: Android 10+需要适当的存储权限
4. **内存管理**: 及时释放模型资源避免内存泄漏
5. **版本兼容**: 确保模型版本与推理引擎兼容

## 故障排除

### 模型加载失败
- 检查模型文件是否存在
- 验证模型格式是否正确
- 确认模型输入输出维度匹配

### 推理结果异常
- 检查文本预处理逻辑
- 验证模型输出格式
- 确认情感标签映射正确

### 性能问题
- 启用GPU加速
- 调整线程数量
- 优化模型大小

## 扩展开发

### 添加新的模型类型
1. 在`OfflineModelManager.ModelType`中添加新类型
2. 实现对应的加载和推理逻辑
3. 更新`analyzeEmotionOffline`方法

### 自定义文本预处理
重写`preprocessTextForTensorFlowLite`或`preprocessTextForOnnx`方法：

```kotlin
private fun preprocessTextForTensorFlowLite(text: String): TensorBuffer {
    // 实现自定义的文本预处理逻辑
    val features = yourCustomPreprocessing(text)
    val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 128), org.tensorflow.lite.DataType.FLOAT32)
    inputBuffer.loadArray(features)
    return inputBuffer
}
``` 