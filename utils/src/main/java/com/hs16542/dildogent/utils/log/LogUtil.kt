package com.hs16542.dildogent.utils.log

import android.content.Context
import android.util.Log
import com.hs16542.dildogent.utils.ToastUtil


fun logI(msg: String) {
    Log.i("", msg)
}

/**
 * 显示Toast消息
 * @param context 上下文
 * @param msg 消息内容
 * @param long 是否显示长时间Toast
 */
fun toast(context: Context, msg: String, long: Boolean = false) {
    if (long) {
        ToastUtil.showLongSafe(context, msg)
    } else {
        ToastUtil.showShortSafe(context, msg)
    }
}