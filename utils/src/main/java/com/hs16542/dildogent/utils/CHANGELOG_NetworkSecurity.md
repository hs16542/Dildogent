# 网络安全问题修复变更日志

## 版本 1.1.0 (2024-12-19)

### 修复的问题
- 修复了 `CLEARTEXT communication to xhslink.com not permitted by network security policy` 错误
- 解决了Android 9.0+版本中HTTP明文通信被阻止的问题
- 优化了网络请求的错误处理和重试机制

### 新增功能
- 创建了 `NetworkUtil` 工具类，提供统一的网络请求处理
- 实现了自动HTTPS/HTTP切换功能
- 添加了网络安全配置文件
- 改进了小红书链接解析的稳定性

### 新增文件
1. **网络安全配置**:
   - `app/src/main/res/xml/network_security_config.xml` - 网络安全策略配置

2. **网络工具类**:
   - `NetworkUtil.kt` - 统一的网络请求处理工具
   - `NetworkUtilTest.kt` - 网络工具测试类

3. **文档**:
   - `README_NetworkSecurity.md` - 网络安全问题解决方案文档
   - `CHANGELOG_NetworkSecurity.md` - 本变更日志

### 修改的文件
1. **应用配置**:
   - `app/src/main/AndroidManifest.xml` - 添加网络安全配置和明文通信支持

2. **视频提取器**:
   - `XiaohongshuVideoExtractor.kt` - 使用NetworkUtil优化网络请求

### 技术改进
- **HTTPS优先策略**: 优先尝试HTTPS连接，提高安全性
- **自动降级**: HTTPS失败时自动切换到HTTP
- **统一错误处理**: 完善的异常处理和日志记录
- **重定向支持**: 自动处理URL重定向
- **网络状态检查**: 检查网络连接可用性

### 安全考虑
- 只允许特定域名使用明文通信
- 调试模式下提供更宽松的配置
- 保持HTTPS优先的安全策略
- 详细的错误日志便于问题排查

### 测试验证
- 验证小红书链接解析功能
- 测试HTTPS和HTTP双重尝试
- 确认网络错误处理正常
- 检查日志输出详细程度

### 使用方式
```kotlin
// 基本网络请求
val content = NetworkUtil.executeRequest("http://xhslink.com/abc123")

// 获取重定向URL
val redirectUrl = NetworkUtil.getRedirectUrl("http://xhslink.com/abc123")

// 检查网络状态
val isAvailable = NetworkUtil.isNetworkAvailable(context)
```

### 注意事项
1. 生产环境建议尽可能使用HTTPS
2. 需要监控网络请求失败情况
3. 如果小红书更改域名，需要更新网络安全配置
4. 确保应用有网络访问权限

### 兼容性
- 支持Android 9.0 (API 28)及以上版本
- 向后兼容Android 8.0及以下版本
- 自动适配HTTPS和HTTP协议 