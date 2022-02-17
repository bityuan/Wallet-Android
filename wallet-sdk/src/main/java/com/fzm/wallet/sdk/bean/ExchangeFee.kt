package com.fzm.wallet.sdk.bean

data class ExchangeFee(
    var ID: Long,
    var CreatedAt: String,
    var UpdatedAt: String,
    var fee: Double,
    var gasFeeUsdt: Double,
    var gasFeeAmount: Double
)