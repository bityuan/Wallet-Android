package com.fzm.walletdemo.wcv2

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


data class WCEthereumTransaction(
    val from: String,
    val to: String,
    val gas: String,
    val gasPrice: String?,
    val value: String?,
    val nonce: String?,
    val data: String
) {
    override fun toString(): String {
        return "WCEthereumTransaction(from='$from', to=$to, nonce=$nonce, gasPrice=$gasPrice, value=$value, data='$data')"
    }
}

data class CreateTran(
    val from: String,
    val gas: Long,
    val gasPrice: Long,
    val input: String,
    val nonce: Long,
    val to: String,
    val value: Long,
)