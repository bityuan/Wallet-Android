package com.fzm.walletmodule.bean

import java.io.Serializable

class WalletBackUp : Serializable {
    var select = 0      //0：未选中  1：选中
    var mnem: String? = null

    companion object {
        const val UN_SELECTED = 0
        const val SELECTED = 1
    }
}