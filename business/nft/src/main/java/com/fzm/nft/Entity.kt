package com.fzm.nft

data class NftValue(
    val position: Int,
    val contractAddr: String,
    val balance: String
)

data class NftTran(
    val blocktime: Long,
    val fee: String,
    val from: String,
    val height: Long,
    val note: String,
    val status: Int,
    val to: String,
    val tokenid: String,
    val txid: String,
    val value: String
)