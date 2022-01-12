package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
object EmptyWallet : Wallet<Coin> {
    override suspend fun init(
        user: String,
        mnem: String,
        mnemType: Int,
        walletName: String,
        password: String,
        coins: List<Coin>
    ): String {
        return ""
    }

    override suspend fun delete(password: suspend () -> String) {

    }

    override suspend fun transfer(coin: Coin, amount: Long) {

    }

    override suspend fun addCoins(coins: List<Coin>, password: suspend () -> String) {

    }

    override suspend fun deleteCoins(coins: List<Coin>) {

    }

    override fun getCoinBalance(
        initialDelay: Long,
        period: Long,
        requireQuotation: Boolean
    ): Flow<List<Coin>> {
        return flow { }
    }

    override suspend fun getTransactionList(
        coin: Coin,
        type: Long,
        index: Long,
        size: Long
    ): List<Transactions> {
        return emptyList()
    }

    override suspend fun getTransactionByHash(
        chain: String,
        tokenSymbol: String,
        hash: String
    ): Transactions? {
        return null
    }
}