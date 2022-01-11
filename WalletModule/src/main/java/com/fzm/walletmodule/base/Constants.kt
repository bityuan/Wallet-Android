package com.fzm.walletmodule.base

import android.text.TextUtils
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

open class Constants {
    companion object {
        const val COINS_KEY = "coins_key"
        const val FROM = "from"
        const val PAGE_LIMIT = 20L
        const val DELAYED_TIME = 8 * 1000.toLong()


        fun setCoins(list: List<Coin>) {
            val json = Gson().toJson(list)
            MMkvUtil.encode(COINS_KEY,json)
        }
        fun getCoins(): List<Coin> {
            val json = MMkvUtil.decodeString(COINS_KEY)
            if (TextUtils.isEmpty(json)) {
                return defaultCoinList()
            }
            return Gson().fromJson(json, object : TypeToken<List<Coin?>?>() {}.type);
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