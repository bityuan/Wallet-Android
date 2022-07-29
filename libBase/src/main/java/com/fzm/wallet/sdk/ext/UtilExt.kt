package com.fzm.wallet.sdk.ext

import android.util.ArrayMap
import android.view.View
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject

fun <T : Any?> String.jsonToMap(): ArrayMap<String, T> {
    return Gson().fromJson<ArrayMap<String, T>>(this, ArrayMap::class.java)
}


fun toJSONStr(vararg params: Pair<String, Any?>): String {
    val param = JSONObject()
    for (i in params) {
        val value = if (i.second == null) "" else i.second
        param.put(i.first, value)
    }
    return param.toString()
}

fun View.oneClick() {
    var oldTime: Long = 0
    if (System.currentTimeMillis() - oldTime  > 500) {
        setOnClickListener {

        }
    }
    oldTime = System.currentTimeMillis()

}
