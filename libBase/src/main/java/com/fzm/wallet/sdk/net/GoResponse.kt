package com.fzm.wallet.sdk.net

data class GoResponse<out T>(val id: Int, val error: String?, val result: T?,val data:T?)
data class DNSResponse<out T>(val code: String?, val message: String?, val status: String?,val data:T?)