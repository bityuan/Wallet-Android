package com.fzm.walletdemo.ui

import com.fzm.wallet.sdk.IPConfig
import com.fzm.walletdemo.W

class WalletHelper {
    companion object {
        fun isSQ(): Boolean {
            return when (W.appType) {
                IPConfig.APP_YBC, IPConfig.APP_YBS, IPConfig.APP_YJM -> true
                else -> false
            }

        }
    }
}