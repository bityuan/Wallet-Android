package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletBean
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
object EmptyWallet : Wallet<Coin> {

    override suspend fun init(configuration: WalletConfiguration): String {
        return ""
    }

    override fun getId(): String {
        return "0"
    }

    override val walletInfo: WalletBean
        get() = WalletBean(0L, "", "", 0)

    override suspend fun changeWalletName(name: String): Boolean {
        return false
    }

    override suspend fun changeWalletPassword(old: String, password: String): Boolean {
        return false
    }

    override suspend fun delete(password: String, confirmation: suspend () -> Boolean): Boolean {
        return false
    }

    override suspend fun transfer(
        coin: Coin,
        toAddress: String,
        amount: Double,
        fee: Double,
        note: String?,
        password: String
    ): String {
        return ""
    }

    override suspend fun addCoins(coins: List<Coin>, password: suspend () -> String) {

    }

    override suspend fun deleteCoins(coins: List<Coin>) {

    }

    override fun getCoinBalance(
        requireQuotation: Boolean,
        predicate: ((Coin) -> Boolean)?
    ): Flow<List<Coin>> = emptyFlow()

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
    ): Transactions {
        return Transactions()
    }

    override suspend fun getAddress(chain: String): String? {
        return null
    }

    override suspend fun getCoinBalance(coin: Coin, requireQuotation: Boolean): Coin {
        return coin
    }

    override fun close() {

    }

    override suspend fun getRedPacketAssets(address: String): List<Coin> {
        return emptyList()
    }

    override suspend fun getMainCoin(chain: String): Coin? {
        return null
    }
}