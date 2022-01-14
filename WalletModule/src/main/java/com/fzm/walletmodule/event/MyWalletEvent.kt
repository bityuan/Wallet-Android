package com.fzm.walletmodule.event

import com.fzm.wallet.sdk.db.entity.PWallet

class MyWalletEvent(val mPWallet: PWallet?, val isChoose: Boolean = false) {
}