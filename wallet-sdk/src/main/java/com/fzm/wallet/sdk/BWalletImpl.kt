package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.alpha.EmptyWallet
import com.fzm.wallet.sdk.alpha.NormalWallet
import com.fzm.wallet.sdk.alpha.Wallet
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletNetModule
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.MMkvUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.litepal.LitePal
import org.litepal.extension.find

/**
 * @author zhengjy
 * @since 2022/01/07
 * Description:
 */
internal class BWalletImpl : BWallet {

    companion object {
        const val CURRENT_USER = "CURRENT_USER"
    }

    private val wallet: Wallet<Coin>
        get() = _wallet ?: EmptyWallet

    private var _wallet: Wallet<Coin>? = null

    override val current: Flow<Wallet<Coin>>
        get() = _current

    private val _current = MutableStateFlow<Wallet<Coin>>(EmptyWallet)

    private var btyPrivkey: String = ""

    private val walletRepository by lazy { rootScope.get<WalletRepository>(walletQualifier) }

    override fun init(context: Context, module: Module?) {
        module?.walletNetModule()
        val user = MMkvUtil.decodeString(CURRENT_USER, "")
        val id = MMkvUtil.decodeString("${user}${PWallet.PWALLET_ID}", "").ifEmpty {
            MMkvUtil.decodeLong(PWallet.PWALLET_ID).toString()
        }
        changeWallet(getWallet(id))
    }

    override fun changeWallet(wallet: WalletBean?): Boolean {
        if (wallet == null || this.wallet.getId() == wallet.id.toString()) return false
        MMkvUtil.encode(CURRENT_USER, wallet.user)
        MMkvUtil.encode("${wallet.user}${PWallet.PWALLET_ID}", wallet.id.toString())
        val local = LitePal.find(PWallet::class.java, wallet.id) ?: return false
        when (wallet.type) {
            PWallet.TYPE_NOMAL -> NormalWallet(local)
            else -> NormalWallet(local)
        }.also { w -> updateWalletFlow(w) }
        return true
    }

    override fun changeWallet(id: String): Boolean {
        return changeWallet(getWallet(id))
    }

    private fun updateWalletFlow(wallet: Wallet<Coin>) {
        _wallet = wallet
        _current.update { wallet }
    }

    override fun  getCurrentWallet(): WalletBean? {
        return getWallet(wallet.getId())
    }

    override suspend fun getAllWallet(user: String) = withContext(Dispatchers.IO) {
        LitePal.where("user = ?", user).find(PWallet::class.java).map { it.toWalletBean() }
    }

    override fun findWallet(id: String?): PWallet? {
        if (id.isNullOrEmpty()) return null
        return LitePal.find(PWallet::class.java, id.toLong())
    }

    override fun getWallet(id: String?): WalletBean? {
        if (id.isNullOrEmpty()) return null
        return LitePal.find(PWallet::class.java, id.toLong())?.toWalletBean()
    }

    override suspend fun importWallet(configuration: WalletConfiguration, switch: Boolean): String {
        val wallet = when (configuration.type) {
            PWallet.TYPE_NOMAL -> NormalWallet(PWallet())
            else -> NormalWallet(PWallet())
        }
        return wallet.init(configuration).also { id ->
            if (switch) {
                MMkvUtil.encode(CURRENT_USER, configuration.user)
                MMkvUtil.encode("${configuration.user}${PWallet.PWALLET_ID}", id)
                updateWalletFlow(wallet)
            }
        }
    }

    override suspend fun changeWalletName(name: String): Boolean {
        if (wallet.changeWalletName(name)) {
            updateWalletFlow(wallet.clone())
            return true
        }
        return false
    }

    override suspend fun changeWalletPassword(old: String, password: String): Boolean {
        if (wallet.changeWalletPassword(old, password)) {
            updateWalletFlow(wallet.clone())
            return true
        }
        return false
    }

    override suspend fun deleteWallet(password: String, confirmation: suspend () -> Boolean) {
        val user = wallet.walletInfo.user
        if (wallet.delete(password, confirmation)) {
            val newWallet = LitePal.where("user = ?", user)
                .find(PWallet::class.java, true)
                .firstOrNull()
            if (newWallet != null) {
                changeWallet(newWallet.toWalletBean())
            } else {
                MMkvUtil.encode(CURRENT_USER, "")
                MMkvUtil.encode(PWallet.PWALLET_ID, "")
                updateWalletFlow(EmptyWallet)
            }
        }
    }

    override suspend fun addCoins(coins: List<Coin>, password: suspend () -> String) {
        wallet.addCoins(coins, password)
    }

    override suspend fun deleteCoins(coins: List<Coin>) {
        wallet.deleteCoins(coins)
    }

    override suspend fun transfer(
        coin: Coin,
        toAddress: String,
        amount: Double,
        fee: Double,
        note: String?,
        password: String
    ): String {
        return wallet.transfer(coin, toAddress, amount, fee, note, password)
    }

    override fun getCoinBalance(
        initialDelay: Long,
        period: Long,
        requireQuotation: Boolean,
        predicate: ((Coin) -> Boolean)?
    ): Flow<List<Coin>> = _current.flatMapLatest {
        it.getCoinBalance(initialDelay, period, requireQuotation, predicate)
    }

    override suspend fun getCoinBalance(coin: Coin, requireQuotation: Boolean): Coin {
        return wallet.getCoinBalance(coin, requireQuotation)
    }

    override suspend fun getTransactionList(
        coin: Coin,
        type: Long,
        index: Long,
        size: Long
    ): List<Transactions> {
        return wallet.getTransactionList(coin, type, index, size)
    }

    override suspend fun getTransactionByHash(
        chain: String,
        tokenSymbol: String,
        hash: String
    ): Transactions {
        return wallet.getTransactionByHash(chain, tokenSymbol, hash)
    }

    override suspend fun getAddress(chain: String): String {
        return wallet.getAddress(chain) ?: ""
    }

    override suspend fun getCoin(chain: String): Coin? {
        val coinList = withContext(Dispatchers.IO) {
            LitePal.select().where("chain = ? and pwallet_id = ?", chain, wallet.getId()).find<Coin>()
        }
        return coinList.firstOrNull()
    }

    override suspend fun getAllCoins(): List<Coin> {
        return withContext(Dispatchers.IO) {
            LitePal.select().where("pwallet_id = ?", wallet.getId()).find()
        }
    }

    override suspend fun getBrowserUrl(platform: String): String {
        return walletRepository.getBrowserUrl(platform).dataOrNull()?.brower_url ?: ""
    }

    override fun close() {
        wallet.close()
        MMkvUtil.encode(CURRENT_USER, "")
        MMkvUtil.encode(PWallet.PWALLET_ID, "")
        updateWalletFlow(EmptyWallet)
    }


    fun setBtyPrivkey(value: String) {
        this.btyPrivkey = value
    }

    override fun getBtyPrikey(): String {
        return btyPrivkey
    }
}