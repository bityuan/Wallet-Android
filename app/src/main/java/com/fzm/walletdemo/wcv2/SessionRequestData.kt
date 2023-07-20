package com.fzm.walletdemo.wcv2

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SessionRequestData(
    val topic: String,
    val appIcon: String?,
    val appName: String?,
    val appUri: String?,
    val requestId: Long,
    val params: String,
    val chain: String?,
    val method: String,
) : Parcelable


data class WCEthereumTransaction(
    val from: String,
    val to: String,
    val nonce: String?,
    val gas: String,
    val gasPrice: String?,
    val gasLimit: String?,
    val value: String,
    val data: String
) {
    override fun toString(): String {
        return "WCEthereumTransaction(from='$from', to=$to, nonce=$nonce, gasPrice=$gasPrice, gasLimit=$gasLimit, value=$value, data='$data')"
    }
}

data class CreateTran(
    val from: String,
    val gas: Long,
    val gasPrice: Long,
    val input: String,
    val nonce: Long,
    val to: String,
    val value: Long
)