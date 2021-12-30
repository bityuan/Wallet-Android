package com.fzm.walletmodule.event

import com.fzm.walletmodule.db.entity.Coin

class TransactionsEvent {
    var coin: Coin
    var address: String = ""

    constructor(coin: Coin) {
        this.coin = coin
    }

    constructor(coin: Coin, address: String) {
        this.coin = coin
        this.address = address
    }
}