package com.fzm.wallet.sdk.ext

import android.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun <T : Any?> String.jsonToMap(): ArrayMap<String, T> {
    return Gson().fromJson<ArrayMap<String, T>>(this, ArrayMap::class.java)
}

fun <T> Map<String, T>.maptoJsonStr(): String {
    return GsonBuilder().disableHtmlEscaping().create().toJson(this)
}
