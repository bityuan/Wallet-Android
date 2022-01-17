package com.fzm.wallet.sdk.net

data class HttpResponse<out T>(val code: Int, val msg: String, val message: String, val data: T?)