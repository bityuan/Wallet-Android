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

    private val wallet: Wallet<Coin>
        get() = _wallet ?: EmptyWallet

    private var _wallet: Wallet<Coin>? = null

    private var pWallet: PWallet? = null

    override val current: Flow<Wallet<Coin>>
        get() = _current

    private val _current = MutableStateFlow<Wallet<Coin>>(EmptyWallet)

    override fun init(context: Context, module: Module?) {
        module?.walletNetModule()
    }

    override fun changeWallet(wallet: PWallet?, user: String): Boolean {
        if (wallet == null || pWallet?.id == wallet.id) return false
        MMkvUtil.encode("${user}${PWallet.PWALLET_ID}", wallet.id)
        this.pWallet = wallet
        this._wallet = when (wallet.type) {
            PWallet.TYPE_NOMAL -> NormalWallet(wallet)
            else -> NormalWallet(wallet)
        }.also { w -> _current.update { w } }
        return true
    }

    override fun getCurrentWallet(user: String): PWallet? {
        val id = MMkvUtil.decodeLong("${user}${PWallet.PWALLET_ID}")
        return LitePal.find(PWallet::class.java, id)
            ?: LitePal.findFirst(PWallet::class.java)?.also {
                changeWallet(it, user)
            }
    }

    override fun findWallet(id: String?): PWallet? {
        if (id.isNullOrEmpty()) return null
        return LitePal.find(PWallet::class.java, id.toLong())
    }

    override suspend fun importWallet(configuration: WalletConfiguration): String {
        val wallet = when (configuration.type) {
            PWallet.TYPE_NOMAL -> NormalWallet(PWallet())
            else -> NormalWallet(PWallet())
        }
        return wallet.init(configuration)
    }

    override suspend fun deleteWallet(password: String, confirmation: suspend () -> Boolean) {
        wallet.delete(password, confirmation)
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
    ): Flow<List<Coin>> = _current.flatMapLatest {
        it.getCoinBalance(initialDelay, period, requireQuotation)
    }

    override suspend fun getTransactionList(coin: Coin, type: Long, index: Long, size: Long): List<Transactions> {
        return wallet.getTransactionList(coin, type, index, size)
    }

    override suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions? {
        return wallet.getTransactionByHash(chain, tokenSymbol, hash)
    }
}