# 故障排除指南

## 依赖问题

### 1. TensorFlow Lite GPU委托插件找不到

**错误信息:**
```
Could not find org.tensorflow:tensorflow-lite-gpu-delegate-plugin:2.14.0
```

**解决方案:**
1. **使用稳定版本** (推荐)
   ```kotlin
   implementation("org.tensorflow:tensorflow-lite:2.13.0")
   implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")
   ```

2. **完全移除GPU支持**
   ```kotlin
   implementation("org.tensorflow:tensorflow-lite:2.13.0")
   // 注释掉GPU相关依赖
   // implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")
   ```

3. **使用备用配置**
   - 复制 `build.gradle.kts.backup` 为 `build.gradle.kts`
   - 使用更保守的依赖版本

### 2. ONNX Runtime依赖问题

**错误信息:**
```
Could not resolve com.microsoft.onnxruntime:onnxruntime-android:1.16.3
Could not find com.microsoft.onnxruntime:onnxruntime-extensions-android:1.16.3
```

**解决方案:**
```kotlin
// 使用稳定版本
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.15.1")
// 移除扩展库，可能不可用
// implementation("com.microsoft.onnxruntime:onnxruntime-extensions-android:1.15.1")
```

### 3. 文本处理库问题

**错误信息:**
```
Could not find org.lucee:jieba-analysis:1.0.3
Could not resolve com.hankcs:hanlp:portable-1.8.4
```

**解决方案:**
```kotlin
// 移除外部文本处理库，使用内置实现
// implementation("com.hankcs:hanlp:portable-1.8.4")
// implementation("org.lucee:jieba-analysis:1.0.3")
```

## 构建问题

### 1. 编译错误

**常见错误:**
- 找不到类或方法
- 版本冲突
- 内存不足

**解决方案:**
1. **清理项目**
   ```bash
   ./gradlew clean
   ```

2. **刷新依赖**
   ```bash
   ./gradlew --refresh-dependencies
   ```

3. **增加内存**
   在 `gradle.properties` 中添加：
   ```properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
   ```

### 2. 运行时错误

**GPU相关错误:**
```
java.lang.ClassNotFoundException: org.tensorflow.lite.gpu.GpuDelegate
```

**解决方案:**
代码已经处理了这种情况，会自动回退到CPU模式。

## 最小化配置

如果遇到多个依赖问题，可以使用最小化配置：

**选项1: 使用最小化配置文件**
```bash
cp llmutil/build.gradle.kts.minimal llmutil/build.gradle.kts
```

**选项2: 手动配置**
```kotlin
dependencies {
    // 核心依赖
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.2")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.2")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.15.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // 移除可能有问题的依赖
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.12.0")
    // implementation("com.microsoft.onnxruntime:onnxruntime-extensions-android:1.15.1")
    // implementation("com.hankcs:hanlp:portable-1.8.4")
    // implementation("org.lucee:jieba-analysis:1.0.3")
}
```

## 功能降级

如果某些功能不可用，系统会自动降级：

1. **GPU不可用** → 使用CPU推理
2. **TensorFlow Lite不可用** → 使用ONNX Runtime
3. **离线模型不可用** → 使用规则基础分析
4. **所有模型不可用** → 使用模拟数据

## 测试建议

### 1. 逐步测试
```kotlin
// 1. 测试基本功能
val llmService = LLMServiceInternal(context)
val result = llmService.analyzeEmotion("测试文本")

// 2. 测试离线模型
llmService.setUseOfflineModelFirst(true)
val result2 = llmService.analyzeEmotion("测试文本")

// 3. 测试模型下载
val downloadManager = ModelDownloadManager(context)
val models = downloadManager.getDownloadedModels()
```

### 2. 日志检查
```kotlin
// 在Logcat中搜索以下标签
"OfflineModelManager"
"ModelDownloadManager"
"LLMService"
```

## 常见问题

### Q: 模型下载失败怎么办？
A: 检查网络连接，确保模型URL可访问，或者使用本地模型文件。

### Q: 推理速度很慢怎么办？
A: 确保使用GPU加速，或者优化模型大小。

### Q: 内存使用过高怎么办？
A: 及时释放模型资源，使用更小的模型文件。

### Q: 应用崩溃怎么办？
A: 检查日志，确保所有依赖都正确配置，使用try-catch包装关键代码。

## 联系支持

如果问题仍然存在，请提供以下信息：
1. 完整的错误日志
2. build.gradle.kts文件内容
3. Android Studio版本
4. 目标设备信息 