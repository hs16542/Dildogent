# ToastUtil 使用说明

## 概述

`ToastUtil` 是一个统一的Toast管理工具类，用于解决在子线程中显示Toast时出现的 `Can't toast on a thread that has not called Looper.prepare()` 异常问题。

## 功能特性

- **自动线程检测**: 自动检测当前线程，如果是主线程直接显示，如果是子线程则切换到主线程显示
- **统一管理**: 提供统一的接口，避免直接使用 `Toast.makeText()`
- **安全显示**: 提供使用 `ApplicationContext` 的安全版本，避免内存泄漏

## 使用方法

### 基本用法

```kotlin
// 显示短时间Toast
ToastUtil.showShort(context, "操作成功")

// 显示长时间Toast
ToastUtil.showLong(context, "这是一个较长的提示信息")
```

### 安全用法（推荐）

```kotlin
// 使用ApplicationContext，更安全
ToastUtil.showShortSafe(context, "操作成功")

// 显示长时间Toast（安全版本）
ToastUtil.showLongSafe(context, "这是一个较长的提示信息")
```

## 替换原有代码

### 替换前
```kotlin
// 直接使用Toast.makeText，可能在子线程中报错
Toast.makeText(this, "检测到小红书链接", Toast.LENGTH_SHORT).show()
Toast.makeText(this, "需要存储权限才能下载视频", Toast.LENGTH_LONG).show()
```

### 替换后
```kotlin
// 使用ToastUtil，自动处理线程问题
ToastUtil.showShort(this, "检测到小红书链接")
ToastUtil.showLong(this, "需要存储权限才能下载视频")
```

## 注意事项

1. **导入包**: 确保导入了 `ToastUtil` 类
   ```kotlin
   import com.hs16542.dildogent.utils.ToastUtil
   ```

2. **Context选择**: 
   - 在Activity中使用 `this` 或 `this@ActivityName`
   - 在Fragment中使用 `requireContext()` 或 `context`
   - 推荐使用 `showShortSafe()` 和 `showLongSafe()` 方法，它们使用 `ApplicationContext`

3. **线程安全**: 所有方法都是线程安全的，可以在任何线程中调用

## 实现原理

`ToastUtil` 通过以下方式解决线程问题：

1. **线程检测**: 使用 `Looper.myLooper() == Looper.getMainLooper()` 检测当前线程
2. **主线程切换**: 使用 `Handler(Looper.getMainLooper())` 将Toast显示操作切换到主线程
3. **Context安全**: 提供使用 `ApplicationContext` 的版本，避免Activity引用导致的内存泄漏

## 迁移指南

1. 搜索项目中所有的 `Toast.makeText` 调用
2. 将 `Toast.makeText(context, message, Toast.LENGTH_SHORT).show()` 替换为 `ToastUtil.showShort(context, message)`
3. 将 `Toast.makeText(context, message, Toast.LENGTH_LONG).show()` 替换为 `ToastUtil.showLong(context, message)`
4. 删除 `import android.widget.Toast` 导入语句
5. 添加 `import com.hs16542.dildogent.utils.ToastUtil` 导入语句 