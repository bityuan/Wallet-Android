package com.fzm.walletmodule.net

data class HttpResponse<out T>(val code: Int, val msg: String, val message: String, val data: T?)