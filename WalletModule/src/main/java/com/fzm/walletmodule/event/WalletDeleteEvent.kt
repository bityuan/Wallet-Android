package com.fzm.walletmodule.event

@Deprecated("不需要用了")
class WalletDeleteEvent(walletId: Long) {
     var walletId = 0L

    init {
        this.walletId = walletId
    }
}