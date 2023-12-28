package com.fzm.wallet.sdk.net

data class GoResponse<out T>(val id: Int, val error: Error?, val result: T?,val data:T?)
data class DNSResponse<out T>(val code: String?, val message: String?, val status: String?,val data:T?)

//  result: {
//  	"jsonrpc" : "2.0",
//  	"id"      : 1,
//  	"error"   : {
//  		"code"    : -32000,
//  		"message" : "ErrBalanceLessThanTenTimesFee"
//  	}
//  }
data class Error(val code:Int?,val message:String?)
