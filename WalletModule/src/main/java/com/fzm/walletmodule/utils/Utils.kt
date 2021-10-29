package com.fzm.walletmodule.utils

import android.util.Log
import walletapi.Walletapi
import java.security.MessageDigest

private var lastClickTime: Long = 0
private var count: Long = 0

fun doMore6(): Boolean {
    val curClickTime = System.currentTimeMillis()
    if (curClickTime - lastClickTime < 3000) {
        count++
        if (count >= 6) {
            return true
        }
    } else {
        count = 0
    }
    lastClickTime = curClickTime
    return false
}

private var lastTime: Long = 0

fun isFastClick(): Boolean {
    var flag = true
    val curClickTime = System.currentTimeMillis()
    if (curClickTime - lastTime > 1000) {
        flag = false
    }
    lastTime = curClickTime
    return flag
}

 fun toMD5(intput: String): String {
    val instance = MessageDigest.getInstance("MD5")
    val digest = instance.digest(intput.toByteArray())
    return Walletapi.byteTohex(digest)
}