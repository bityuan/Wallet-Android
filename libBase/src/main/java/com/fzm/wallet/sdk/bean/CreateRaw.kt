package com.fzm.wallet.sdk.bean

import java.math.BigInteger


data class CreateRaw(
    val from: String,
    val gas: BigInteger,
    val gasPrice: BigInteger,
    val input: String?,
    val nonce: Long,
    val to: String,
    val value: BigInteger,
    val leafPosition: Long = 0
)