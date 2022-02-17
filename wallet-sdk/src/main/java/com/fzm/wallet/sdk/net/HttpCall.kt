package com.fzm.wallet.sdk.net

import android.text.TextUtils

//if(T::class.java.isAssignableFrom(List::class.java)) {}
suspend fun <T> apiCall(call: suspend () -> HttpResponse<T>): HttpResult<T> {
    return try {
        call().let {
            if (it.code == 0 || it.code == 200) {
                HttpResult.Success(it.data)
            } else {
                HttpResult.Error(if (TextUtils.isEmpty(it.msg)) it.message else it.msg)
            }
        }
    } catch (e: Exception) {
        HttpResult.Error(HttpResult.handleException(e)!!)
    }


}


suspend fun <T> goCall(call: suspend () -> GoResponse<T>): HttpResult<T> {
    return try {
        call().let {
            if (it.error == null) {
                HttpResult.Success(it.result)
            } else {
                HttpResult.Error(it.error)
            }
        }
    } catch (e: Exception) {
        HttpResult.Error(HttpResult.handleException(e)!!)
    }

}

