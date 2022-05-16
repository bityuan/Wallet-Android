package com.fzm.wallet.sdk.net

data class GoResponse<out T>(val id: Int, val error: String?, val result: T?)