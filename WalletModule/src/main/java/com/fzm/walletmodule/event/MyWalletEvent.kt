package com.fzm.walletmodule.event

import com.fzm.walletmodule.db.entity.PWallet

class MyWalletEvent(pWallet: PWallet) {
    var mPWallet: PWallet? = null

    init {
        mPWallet = pWallet
    }
}