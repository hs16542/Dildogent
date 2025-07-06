# Utils 模块

这个模块提供了各种工具功能，目前包含小红书视频自动提取功能。

## 功能特性

### 小红书视频提取器 (XiaohongshuVideoExtractor)

- 自动识别小红书链接格式
- 解析小红书短链接获取真实URL
- 提取视频信息和下载链接
- 自动下载视频文件到本地
- 支持批量处理多个链接
- 提供详细的提取结果统计

## 使用方法

### 1. 基本使用

```kotlin
// 获取工具管理器实例
val utilsManager = UtilsManager.getInstance(context)

// 检查文本是否包含小红书链接
val text = "36 浏阳桨板烟花发布了一篇小红书笔记，快来看吧！ 😆 Fpbs13L2ZpLBTqy 😆 http://xhslink.com/a/sfg0VART9Spgb 复制本条信息，打开【小红书】App查看精彩内容！"

if (utilsManager.containsXhsLink(text)) {
    // 提取链接
    val links = utilsManager.extractXhsLinks(text)
    println("提取到的链接: $links")
    
    // 提取笔记ID
    val noteIds = utilsManager.extractXhsNoteIds(text)
    println("提取到的笔记ID: $noteIds")
}
```

### 2. 视频提取

```kotlin
// 提取并下载视频
utilsManager.extractXhsVideos(
    text = text,
    outputDir = utilsManager.createDownloadDir("XiaohongshuVideos"),
    callback = object : UtilsManager.VideoExtractCallback {
        override fun onStart() {
            // 开始提取
            println("开始提取视频...")
        }
        
        override fun onSuccess(result: XiaohongshuVideoExtractor.ExtractResult) {
            // 提取成功
            println("提取完成！")
            println("总链接数: ${result.totalLinks}")
            println("总视频数: ${result.totalVideos}")
            println("成功下载: ${result.successCount}")
            println("失败数量: ${result.failedCount}")
            
            // 显示下载的视频
            result.downloadedVideos.forEach { downloadedVideo ->
                println("下载成功: ${downloadedVideo.videoInfo.title} -> ${downloadedVideo.filePath}")
            }
        }
        
        override fun onError(error: String) {
            // 提取失败
            println("提取失败: $error")
        }
    }
)
```

### 3. 管理下载文件

```kotlin
// 获取已下载的视频列表
val videos = utilsManager.getDownloadedVideos()
videos.forEach { file ->
    println("视频文件: ${file.name} (${file.length()} bytes)")
}

// 清理下载目录
val success = utilsManager.clearDownloadDir()
if (success) {
    println("清理成功")
} else {
    println("清理失败")
}
```

## 权限要求

在AndroidManifest.xml中需要添加以下权限：

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 支持的链接格式

- 小红书短链接: `http://xhslink.com/xxx`
- 小红书笔记ID: `Fpbsxxxxxxxx`

## 注意事项

1. 需要网络连接才能提取视频
2. 需要存储权限才能下载视频文件
3. 视频下载可能需要较长时间，建议在后台线程中执行
4. 某些视频可能因为版权保护无法下载
5. 请遵守相关法律法规，仅用于个人学习和研究

## 示例代码

完整的示例代码请参考 `VideoExtractDemoActivity.kt` 文件。

## 依赖库

- OkHttp: 网络请求
- Retrofit: HTTP客户端
- Gson: JSON解析
- Kotlin Coroutines: 异步处理 