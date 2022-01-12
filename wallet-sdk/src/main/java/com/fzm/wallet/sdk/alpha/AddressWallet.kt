package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
class AddressWallet(wallet: PWallet) : BaseWallet(wallet) {

    override suspend fun init(
        user: String,
        mnem: String,
        mnemType: Int,
        walletName: String,
        password: String,
        coins: List<Coin>
    ): String {
        TODO("Not yet implemented")
    }

    override suspend fun transfer(coin: Coin, amount: Long) {
        throw UnsupportedOperationException("观察钱包不支持转账")
    }
}