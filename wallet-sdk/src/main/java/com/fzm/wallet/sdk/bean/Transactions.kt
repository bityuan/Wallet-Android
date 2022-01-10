package com.fzm.wallet.sdk.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Transactions : Serializable {

    var blocktime: Long = 0
    var fee: String? = null
    var from: String? = null
    var height = 0
    var to: String? = null
    var txid: String? = null
    var type: String? = null
    var value: String? = null
    var otherAdderss: String? = null

    //新增
    var nickName: String? = null
    var chain: String? = null

    //失败：-1，确认中：0，成功：1
    var status = 0
    var walletId: Long = 0

    //php新增
    @SerializedName("id")
    var netId: String? = null
    var coinname: String? = null
    var note: String? = null

    companion object {
        const val IN_STR = "+"
        const val OUT_STR = "-"
        const val TYPE_RECEIVE = "receive"
        const val TYPE_SEND = "send"
    }
}