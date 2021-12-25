package com.fzm.walletmodule.base

import com.fzm.walletmodule.db.entity.Coin

open class Constants {
    companion object {
        const val FROM = "from"
        const val PAGE_LIMIT = 20L
        var DELAYED_TIME = 8 * 1000.toLong()

        var coinList = mutableListOf<Coin>()


        fun setCoins(list: MutableList<Coin>) {
            coinList.clear()
            coinList.addAll(list)
        }
        fun getCoins():List<Coin> {
            if (coinList.size == 0) {
                coinList = defaultCoinList()
            }

            return coinList;
        }


        private fun defaultCoinList(): MutableList<Coin> {
            val coinList = mutableListOf<Coin>()
            val btyCoin = Coin()
            btyCoin.name = "BTY"
            btyCoin.chain = "BTY"
            btyCoin.platform = "bty"
            btyCoin.nickname = "比特元"
            btyCoin.treaty = "1"
            coinList.add(btyCoin)
            return coinList
        }
    }
}