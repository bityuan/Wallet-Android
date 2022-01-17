package com.fzm.walletmodule.utils

import android.util.Log
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
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


fun jsonToMap(jsonString: String): HashMap<String, Any>? {
    val jsonObject: JSONObject
    try {
        jsonObject = JSONObject(jsonString)
        val keyIter: Iterator<String> = jsonObject.keys()
        var key: String
        var value: Any
        val valueMap = HashMap<String, Any>()
        while (keyIter.hasNext()) {
            key = keyIter.next()
            value = jsonObject[key] as Any
            valueMap[key] = value
        }
        return valueMap
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    return null
}

fun mapToJson(vararg params: Pair<String, Any?>): String {
    val param = JSONObject()
    for (i in params) {
        val value = if (i.second == null) "" else i.second
        param.put(i.first, value)
    }
    return param.toString()
}

/*
fun mapOftoJson(vararg pairs: Pair<String, Any?>): String {
    val map = mapOf(pairs)
    return Gson().toJson(map)

      val map = mapOf("error" to str,"" to "")
        handler.complete(Gson().toJson(map))
}*/
