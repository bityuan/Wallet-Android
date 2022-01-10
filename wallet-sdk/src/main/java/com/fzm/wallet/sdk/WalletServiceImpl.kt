package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.bean.response.TransactionResponse
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletNetModule
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.module.Module
import org.litepal.LitePal
import java.util.ArrayDeque

/**
 * @author zhengjy
 * @since 2022/01/07
 * Description:
 */
internal class WalletServiceImpl : WalletService {

    private val gson = Gson()

    override fun init(context: Context, module: Module?) {
        module?.walletNetModule()
    }

    override suspend fun importMnem(mnem: String, mnemType: Int, walletName: String, password: String): PWallet {
        if (mnem.isEmpty()) {
            throw Exception("助记词不能为空")
        }
        if (walletName.isEmpty()) {
            throw Exception("钱包名称不能为空")
        }
        if (password.isEmpty()) {
            throw Exception("钱包密码不能为空")
        }
        val wallet = PWallet().apply {
            this.mnemType = mnemType
            if (mnemType == PWallet.TYPE_CHINESE) {
                this.mnem = getChineseMnem(mnem)
            } else {
                this.mnem = mnem
            }
            this.type = PWallet.TYPE_NOMAL
            this.name = walletName
            this.password = password
        }

        return GoWallet.createWallet(wallet, emptyList())
    }

    override fun getCoinBalance(
        walletId: String,
        initialDelay: Long,
        period: Long,
        requireQuotation: Boolean
    ): Flow<List<Coin>> = flow {
        if (initialDelay > 0) delay(initialDelay)
        while (true) {
            coroutineScope {
                val deferred = ArrayDeque<Deferred<Unit>>()
                val coins = LitePal.where("pwallet_id = ? and status = ?", walletId, Coin.STATUS_ENABLE.toString())
                    .find(Coin::class.java, true)
                for (coin in coins) {
                    deferred.add(async(Dispatchers.IO) {
                        try {
                            coin.balance = GoWallet.handleBalance(coin)
                        } catch (e: Exception) {
                            // 资产获取异常
                        }
                    })
                }
                val quotationDeferred: Deferred<Any>? = if (requireQuotation) {
                    TODO("获取币种行情")
                } else null
                while (deferred.isNotEmpty()) {
                    deferred.poll()?.await()
                }
                quotationDeferred?.await()
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

    private fun getChineseMnem(mnem: String): String {
        val afterString = mnem.replace(" ", "")
        val afterString2 = afterString.replace("\n", "")
        return afterString2.replace("", " ").trim()
    }

    private fun getKey(coin: Coin, type: Long): String =
        "${coin.chain}${coin.address}${coin.name}$type}"
}