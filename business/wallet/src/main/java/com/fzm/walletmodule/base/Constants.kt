package com.fzm.walletmodule.base

import android.text.TextUtils
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletmodule.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import walletapi.Walletapi

open class Constants {
    companion object {
        const val COINS_KEY = "coins_key"
        const val TRAN_STATE_KEY = "tran_state_key"
        const val PAGE_LIMIT = 30L


        fun setCoins(list: List<Coin>) {
            val json = Gson().toJson(list)
            MMkvUtil.encode(COINS_KEY, json)
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

        fun getWalletIcon(chain: String): Int {
            return when (chain) {
                Walletapi.TypeBitcoinString -> R.mipmap.my_wallet_btc
                Walletapi.TypeBtyString -> R.mipmap.my_wallet_bty
                Walletapi.TypeDcrString -> R.mipmap.my_wallet_dcr
                Walletapi.TypeETHString -> R.mipmap.my_wallet_eth
                Walletapi.TypeLitecoinString -> R.mipmap.my_wallet_ltc
                Walletapi.TypeEtherClassicString -> R.mipmap.my_wallet_etc
                Walletapi.TypeZcashString -> R.mipmap.my_wallet_zec
                Walletapi.TypeNeoString -> R.mipmap.my_wallet_neo
                Walletapi.TypeBchString -> R.mipmap.my_wallet_bch
                Walletapi.TypeTrxString -> R.mipmap.my_wallet_trx
                Walletapi.TypeAtomString -> R.mipmap.my_wallet_atom
                Walletapi.TypeBnbString -> R.mipmap.my_wallet_bnb
                Walletapi.TypeHtString -> R.mipmap.my_wallet_ht
                else -> R.mipmap.my_wallet_bty
            }
        }

        fun getWalletBg(chain: String): Int {
            return when (chain) {
                Walletapi.TypeBitcoinString -> R.mipmap.my_wallet_bg_btc
                Walletapi.TypeBtyString -> R.mipmap.my_wallet_bg_bty
                Walletapi.TypeDcrString -> R.mipmap.my_wallet_bg_dcr
                Walletapi.TypeETHString -> R.mipmap.my_wallet_bg_eth
                Walletapi.TypeLitecoinString -> R.mipmap.my_wallet_bg_ltc
                Walletapi.TypeEtherClassicString -> R.mipmap.my_wallet_bg_etc
                Walletapi.TypeZcashString -> R.mipmap.my_wallet_bg_zec
                Walletapi.TypeNeoString -> R.mipmap.my_wallet_bg_neo
                Walletapi.TypeBchString -> R.mipmap.my_wallet_bg_bch
                Walletapi.TypeTrxString, Walletapi.TypeBnbString -> R.mipmap.my_wallet_bg_trx
                Walletapi.TypeAtomString -> R.mipmap.my_wallet_bg_atom
                Walletapi.TypeHtString -> R.mipmap.my_wallet_bg_etc
                else -> R.mipmap.my_wallet_bg_bty
            }
        }
    }

}