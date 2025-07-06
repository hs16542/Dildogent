# ToastUtil 变更日志

## 版本 1.0.0 (2024-12-19)

### 新增功能
- 创建了 `ToastUtil` 工具类，统一管理Toast显示
- 实现了自动线程检测和主线程切换功能
- 提供了基本版本和安全版本的Toast显示方法
- 更新了 `LogUtil.toast()` 函数，使用 `ToastUtil` 实现

### 解决的问题
- 修复了 `Can't toast on a thread that has not called Looper.prepare()` 异常
- 统一了项目中Toast的使用方式
- 避免了在子线程中直接调用Toast导致的崩溃

### 修改的文件
1. **新增文件**:
   - `ToastUtil.kt` - 主要的Toast管理工具类
   - `README_ToastUtil.md` - 使用说明文档
   - `ToastUtilTest.kt` - 单元测试

2. **修改文件**:
   - `VideoExtractDemoActivity.kt` - 将所有 `Toast.makeText()` 替换为 `ToastUtil` 调用
   - `LogUtil.kt` - 更新 `toast()` 函数实现

### 技术实现
- 使用 `Looper.myLooper() == Looper.getMainLooper()` 检测当前线程
- 使用 `Handler(Looper.getMainLooper())` 切换到主线程
- 提供 `ApplicationContext` 版本避免内存泄漏
- 使用单例模式确保线程安全

### 使用方式
```kotlin
// 基本用法
ToastUtil.showShort(context, "操作成功")
ToastUtil.showLong(context, "长消息")

// 安全用法（推荐）
ToastUtil.showShortSafe(context, "操作成功")
ToastUtil.showLongSafe(context, "长消息")
```

### 迁移指南
1. 搜索项目中的 `Toast.makeText` 调用
2. 替换为对应的 `ToastUtil` 方法
3. 删除 `import android.widget.Toast` 导入
4. 添加 `import com.hs16542.dildogent.utils.ToastUtil` 导入 