package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.bean.response.TransactionResponse
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.litepal.LitePal
import java.util.*

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
abstract class BaseWallet(protected val wallet: PWallet) : Wallet<Coin> {

    protected val gson by lazy { Gson() }
    protected val walletRepository by rootScope.inject<WalletRepository>(walletQualifier)

    override suspend fun delete(password: String, confirmation: suspend () -> Boolean) {
        val verified = withContext(Dispatchers.IO) {
            GoWallet.checkPasswd(password, wallet.password)
        }
        if (verified) {
            if (confirmation()) {
                withContext(Dispatchers.IO) {
                    val coins = LitePal.select()
                        .where("pwallet_id = ? group by chain", wallet.id.toString())
                        .find(Coin::class.java)
                    LitePal.delete(PWallet::class.java, wallet.id)
                }
            }
        } else {
            throw IllegalArgumentException("密码输入错误")
        }
    }

    override suspend fun transfer(coin: Coin, amount: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun addCoins(coins: List<Coin>, password: suspend () -> String) {
        var cachePass = ""
        for (c in coins) {
            checkCoin(c) {
                cachePass.ifEmpty {
                    withContext(Dispatchers.Main.immediate) {
                        password().also { p -> cachePass = p }
                    }
                }
            }
        }

        val existCoins = LitePal.where("pwallet_id = ?", wallet.id.toString())
            .find(Coin::class.java, true)
    }

    @Throws(Exception::class)
    private suspend fun checkCoin(coin: Coin, password: suspend () -> String) {
        if (coin.chain == null) return
        val sameChainCoin =
            LitePal.select().where("chain = ? and pwallet_id = ?", coin.chain, wallet.id.toString())
                .findFirst(Coin::class.java)
        if (sameChainCoin != null) {
            coin.address = sameChainCoin.address
            coin.pubkey = sameChainCoin.pubkey
            coin.setPrivkey(sameChainCoin.encPrivkey)
        } else {
            val pass = password()
            if (pass.isEmpty()) return

        }
    }

    override suspend fun deleteCoins(coins: List<Coin>) {
        for (c in coins) {
            c.status = Coin.STATUS_DISABLE
            c.update(c.id)
        }
    }

    override fun getCoinBalance(
        initialDelay: Long,
        period: Long,
        requireQuotation: Boolean
    ): Flow<List<Coin>> = flow {
        if (initialDelay > 0) delay(initialDelay)
        var initEmit = true
        while (true) {
            coroutineScope {
                val deferred = ArrayDeque<Deferred<Unit>>()
                val coins = LitePal.where(
                    "pwallet_id = ? and status = ?",
                    wallet.id.toString(),
                    Coin.STATUS_ENABLE.toString()
                )
                    .find(Coin::class.java, true)
                for (coin in coins) {
                    deferred.add(async(Dispatchers.IO) {
                        try {
                            coin.balance = GoWallet.handleBalance(coin)
                            coin.update(coin.id)
                            return@async
                        } catch (e: Exception) {
                            // 资产获取异常
                        }
                    })
                }
                val quotationDeferred =
                    if (requireQuotation || coins.any { it.nickname.isNullOrEmpty() }) {
                        // 查询资产行情等
                        async { walletRepository.getCoinList(coins.map { "${it.name},${it.platform}" }) }
                    } else null
                if (initEmit) {
                    initEmit = false
                    // 第一次订阅时先提前发射本地缓存数据
                    emit(coins)
                }
                quotationDeferred?.await()?.dataOrNull()?.also { coinMeta ->
                    val coinMap = coins.associateBy { "${it.chain}-${it.name}-${it.platform}" }
                    for (meta in coinMeta) {
                        coinMap["${meta.chain}-${meta.name}-${meta.platform}"]?.apply {
                            this.rmb = meta.rmb
                            this.icon = meta.icon
                            this.nickname = meta.nickname
                            update(id)
                        }
                    }
                    emit(coins)
                }
                while (deferred.isNotEmpty()) {
                    deferred.poll()?.await()
                }
                emit(coins)
            }
            delay(period.coerceAtLeast(1000L))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getTransactionList(
        coin: Coin,
        type: Long,
        index: Long,
        size: Long
    ): List<Transactions> {
        var coinName = coin.name
        if (GoWallet.isBTYChild(coin)) {
            coinName =
                if (coin.treaty == "1") "${coin.platform}.${coin.name}" else "${coin.platform}.coins"
        }

        val data = if (index == 0L) {
            GoWallet.getTranList(coin.address, coin.chain, coinName, type, index, size)
        } else {
            GoWallet.getTranList(coin.address, coin.chain, coinName, type, index, size)
        }
        if (data.isNullOrEmpty()) {
            val local = MMkvUtil.decodeString(getKey(coin, type))
            return gson.fromJson(local, TransactionResponse::class.java).result ?: emptyList()
        }
        if (index == 0L) {
            // 缓存第一页数据
            MMkvUtil.encode(getKey(coin, type), data)
        }
        return gson.fromJson(data, TransactionResponse::class.java).result ?: emptyList()
    }

    override suspend fun getTransactionByHash(
        chain: String,
        tokenSymbol: String,
        hash: String
    ): Transactions? {
        val data = GoWallet.getTranByTxid(chain, tokenSymbol, hash)
        if (data.isNullOrEmpty()) return null
        return try {
            gson.fromJson(data, Transactions::class.java)
        } catch (e: Exception) {
            null
        }
    }

    protected fun getChineseMnem(mnem: String): String {
        val afterString = mnem.replace(" ", "")
        val afterString2 = afterString.replace("\n", "")
        return afterString2.replace("", " ").trim()
    }

    protected fun getKey(coin: Coin, type: Long): String =
        "${coin.chain}${coin.address}${coin.name}$type}"
}