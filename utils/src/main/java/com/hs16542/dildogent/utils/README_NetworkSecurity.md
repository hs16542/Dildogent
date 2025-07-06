# 网络安全问题解决方案

## 问题描述

在Android 9.0 (API 28)及以上版本中，应用默认只能使用HTTPS连接，不允许明文HTTP通信。这导致在访问小红书等使用HTTP的网站时出现以下错误：

```
CLEARTEXT communication to xhslink.com not permitted by network security policy
```

## 解决方案

### 1. 网络安全配置

创建了 `network_security_config.xml` 文件，允许明文通信到特定域名：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- 允许明文通信到小红书相关域名 -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">xhslink.com</domain>
        <domain includeSubdomains="true">xiaohongshu.com</domain>
        <domain includeSubdomains="true">xhs.cn</domain>
        <domain includeSubdomains="true">xhsapp.com</domain>
    </domain-config>
    
    <!-- 调试模式下允许所有明文通信（仅用于开发测试） -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system"/>
            <certificates src="user"/>
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

### 2. AndroidManifest.xml 配置

在 `AndroidManifest.xml` 中添加了网络安全配置：

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="true"
    ...>
```

### 3. NetworkUtil 工具类

创建了 `NetworkUtil` 工具类，提供统一的网络请求处理：

- **自动HTTPS/HTTP切换**: 优先尝试HTTPS，失败后自动切换到HTTP
- **统一错误处理**: 提供详细的日志记录和错误处理
- **重定向支持**: 自动处理URL重定向
- **网络状态检查**: 检查网络连接是否可用

### 4. 优化后的XiaohongshuVideoExtractor

更新了 `XiaohongshuVideoExtractor` 类：

- 使用 `NetworkUtil` 进行网络请求
- 改进了错误处理和日志记录
- 支持HTTPS和HTTP双重尝试

## 使用方法

### 基本网络请求

```kotlin
// 执行网络请求，自动尝试HTTPS和HTTP
val content = NetworkUtil.executeRequest("http://xhslink.com/abc123")

// 获取重定向后的URL
val redirectUrl = NetworkUtil.getRedirectUrl("http://xhslink.com/abc123")

// 检查网络连接
val isNetworkAvailable = NetworkUtil.isNetworkAvailable(context)
```

### 小红书链接解析

```kotlin
// 解析小红书链接（自动处理HTTPS/HTTP）
val realUrl = xiaohongshuVideoExtractor.resolveXhsLink("http://xhslink.com/abc123")

// 提取视频信息
val videoInfos = xiaohongshuVideoExtractor.extractVideoInfo(realUrl)
```

## 安全考虑

1. **域名限制**: 只允许特定域名使用明文通信
2. **调试模式**: 仅在调试模式下允许所有明文通信
3. **HTTPS优先**: 优先使用HTTPS连接，提高安全性
4. **错误处理**: 完善的错误处理和日志记录

## 测试验证

1. 确保应用能够正常访问小红书链接
2. 验证HTTPS和HTTP都能正常工作
3. 检查网络错误处理是否正常
4. 确认日志输出是否详细

## 注意事项

1. **生产环境**: 在生产环境中，建议尽可能使用HTTPS
2. **域名更新**: 如果小红书更改域名，需要更新网络安全配置
3. **权限检查**: 确保应用有网络访问权限
4. **错误监控**: 监控网络请求失败的情况，及时处理

## 相关文件

- `app/src/main/res/xml/network_security_config.xml` - 网络安全配置
- `app/src/main/AndroidManifest.xml` - 应用配置
- `utils/src/main/java/com/hs16542/dildogent/utils/NetworkUtil.kt` - 网络工具类
- `utils/src/main/java/com/hs16542/dildogent/utils/XiaohongshuVideoExtractor.kt` - 视频提取器 