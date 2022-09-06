package com.fzm.wallet.sdk

import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet

/**
 * @author zhengjy
 * @since 2022/01/13
 * Description:
 */
class WalletConfiguration {

    /**
     * 钱包类型
     */
    var type: Int = 0
    var mnemonic: String? = null
        private set
    var privateKey: String? = null
        private set
    var address: String? = null
        private set
    var walletName: String? = null
        private set
    var password: String? = null
        private set
    var coins: List<Coin> = ArrayList()

    companion object {

        fun mnemonicWallet(
            mnemonic: String,
            walletName: String,
            password: String,
            coins: List<Coin>
        ) = WalletConfiguration().apply {
            this.type = PWallet.TYPE_NOMAL
            this.mnemonic = mnemonic
            this.walletName = walletName
            this.password = password
            this.coins = coins
        }

        fun privateKeyWallet(
            privateKey: String,
            walletName: String,
            password: String,
            coins: List<Coin>
        ) = WalletConfiguration().apply {
            this.type = PWallet.TYPE_PRI_KEY
            this.privateKey = privateKey
            this.walletName = walletName
            this.password = password
            this.coins = coins
        }
        fun recoverWallet(
            privateKey: String,
            walletName: String,
            password: String,
            coins: List<Coin>
        ) = WalletConfiguration().apply {
            this.type = PWallet.TYPE_RECOVER
            this.privateKey = privateKey
            this.walletName = walletName
            this.password = password
            this.coins = coins
        }

        fun addressWallet(
            address: String,
            walletName: String,
            password: String,
            coins: List<Coin>
        ) = WalletConfiguration().apply {
            this.address = address
            this.walletName = walletName
            this.password = password
            this.coins = coins
        }
    }
}