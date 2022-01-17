package com.fzm.walletmodule.event

import com.fzm.wallet.sdk.db.entity.PWallet

class MyWalletEvent(pWallet: PWallet) {
    var mPWallet: PWallet? = null

    init {
        mPWallet = pWallet
    }
}