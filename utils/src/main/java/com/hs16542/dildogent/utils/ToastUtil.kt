package com.hs16542.dildogent.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Toast工具类
 * 统一管理Toast显示，避免线程问题
 */
object ToastUtil {
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * 显示短时间Toast
     * @param context 上下文
     * @param message 消息内容
     */
    fun showShort(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_SHORT)
    }
    
    /**
     * 显示长时间Toast
     * @param context 上下文
     * @param message 消息内容
     */
    fun showLong(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_LONG)
    }
    
    /**
     * 显示Toast（自动处理线程问题）
     * @param context 上下文
     * @param message 消息内容
     * @param duration 显示时长
     */
    private fun showToast(context: Context, message: String, duration: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 主线程直接显示
            Toast.makeText(context, message, duration).show()
        } else {
            // 子线程切换到主线程显示
            mainHandler.post {
                Toast.makeText(context, message, duration).show()
            }
        }
    }
    
    /**
     * 显示短时间Toast（使用ApplicationContext）
     * @param context 上下文
     * @param message 消息内容
     */
    fun showShortSafe(context: Context, message: String) {
        showToastSafe(context, message, Toast.LENGTH_SHORT)
    }
    
    /**
     * 显示长时间Toast（使用ApplicationContext）
     * @param context 上下文
     * @param message 消息内容
     */
    fun showLongSafe(context: Context, message: String) {
        showToastSafe(context, message, Toast.LENGTH_LONG)
    }
    
    /**
     * 显示Toast（使用ApplicationContext，更安全）
     * @param context 上下文
     * @param message 消息内容
     * @param duration 显示时长
     */
    private fun showToastSafe(context: Context, message: String, duration: Int) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 主线程直接显示
            Toast.makeText(appContext, message, duration).show()
        } else {
            // 子线程切换到主线程显示
            mainHandler.post {
                Toast.makeText(appContext, message, duration).show()
            }
        }
    }
} 