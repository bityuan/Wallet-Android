package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.alpha.EmptyWallet
import com.fzm.wallet.sdk.alpha.NormalWallet
import com.fzm.wallet.sdk.alpha.Wallet
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletNetModule
import com.fzm.wallet.sdk.utils.MMkvUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import org.koin.core.module.Module
import org.litepal.LitePal

/**
 * @author zhengjy
 * @since 2022/01/07
 * Description:
 */
internal class BWalletImpl : BWallet {

    private val walletState = MutableStateFlow(0)
    private var _wallet: Wallet<Coin>? = null

    private val wallet: Wallet<Coin>
        get() = _wallet ?: EmptyWallet

    private var pWallet: PWallet? = null

    override fun init(context: Context, module: Module?) {
        module?.walletNetModule()
    }

    override fun changeWallet(wallet: PWallet?): Boolean {
        if (wallet == null || pWallet?.id == wallet.id) return false
        this.pWallet = wallet
        this._wallet = when (wallet.type) {
            PWallet.TYPE_NOMAL -> NormalWallet(wallet)
            else -> NormalWallet(wallet)
        }
        walletState.update { it + 1 }
        return true
    }

    override fun getCurrentWallet(user: String): PWallet? {
        val id = MMkvUtil.decodeLong("${user}${PWallet.PWALLET_ID}")
        return LitePal.find(PWallet::class.java, id)
            ?: LitePal.findFirst(PWallet::class.java)?.also {
                setCurrentWalletId(user, it.id)
            }
    }

    override fun setCurrentWalletId(user: String, id: Long) {
        MMkvUtil.encode("${user}${PWallet.PWALLET_ID}", id)
    }

    override suspend fun importNormalWallet(user: String, mnem: String, mnemType: Int, walletName: String, password: String, coins: List<Coin>): String {
        return wallet.init(user, mnem, mnemType, walletName, password, coins)
    }

    override suspend fun deleteWallet(password: suspend () -> String) {
        wallet.delete(password)
    }

    override suspend fun addCoins(coins: List<Coin>, password: suspend () -> String) {
        wallet.addCoins(coins, password)
    }

    override suspend fun deleteCoins(coins: List<Coin>) {
        wallet.deleteCoins(coins)
    }

    override fun getCoinBalance(
        initialDelay: Long,
        period: Long,
        requireQuotation: Boolean
    ): Flow<List<Coin>> = walletState.flatMapLatest {
        wallet.getCoinBalance(initialDelay, period, requireQuotation)
    }


    override suspend fun getTransactionList(coin: Coin, type: Long, index: Long, size: Long): List<Transactions> {
        return wallet.getTransactionList(coin, type, index, size)
    }

    override suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions? {
        return wallet.getTransactionByHash(chain, tokenSymbol, hash)
    }
}