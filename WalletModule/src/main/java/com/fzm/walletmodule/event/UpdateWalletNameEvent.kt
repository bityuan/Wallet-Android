package com.fzm.walletmodule.event

class UpdateWalletNameEvent(needUpdate: Boolean) {
    var needUpdate: Boolean = false

    init {
        this.needUpdate = needUpdate
    }
}