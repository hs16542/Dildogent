package com.hs16542.dildogent.utils

import android.content.Context
import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ToastUtilTest {

    @Mock
    private lateinit var mockContext: Context

    @Test
    fun testShowShort() {
        // 测试短时间Toast显示
        ToastUtil.showShort(mockContext, "测试消息")
        // 由于Toast是系统UI组件，这里主要测试方法调用不会抛出异常
    }

    @Test
    fun testShowLong() {
        // 测试长时间Toast显示
        ToastUtil.showLong(mockContext, "测试长消息")
        // 由于Toast是系统UI组件，这里主要测试方法调用不会抛出异常
    }

    @Test
    fun testShowShortSafe() {
        // 测试安全版本的短时间Toast显示
        ToastUtil.showShortSafe(mockContext, "测试安全消息")
        // 由于Toast是系统UI组件，这里主要测试方法调用不会抛出异常
    }

    @Test
    fun testShowLongSafe() {
        // 测试安全版本的长时间Toast显示
        ToastUtil.showLongSafe(mockContext, "测试安全长消息")
        // 由于Toast是系统UI组件，这里主要测试方法调用不会抛出异常
    }

    @Test
    fun testThreadSafety() {
        // 测试线程安全性
        // 在实际应用中，这个方法会在子线程中被调用
        // 这里主要测试方法调用不会抛出异常
        ToastUtil.showShortSafe(mockContext, "线程安全测试")
    }
} 