package com.fzm.walletmodule.event

import com.fzm.wallet.sdk.db.entity.Coin

class TransactionsEvent {
    var coin: Coin? = null
    var address: String? = null

    constructor(coin: Coin) {
        this.coin = coin
    }

    constructor(coin: Coin, address: String) {
        this.coin = coin
        this.address = address
    }
}