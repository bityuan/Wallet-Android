package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.Module

/**
 * @author zhengjy
 * @since 2022/01/07
 * Description:
 */
interface WalletService {

    companion object {

        private val wallet by lazy { WalletServiceImpl() }

        fun get(): WalletService = wallet
    }

    fun init(context: Context, module: Module?)

    /**
     * 获取币种列表
     */
    suspend fun getCoinList(walletId: String): List<Coin>

    /**
     * 获取资产余额列表
     */
    fun getCoinBalance(coins: List<Coin>, initialDelay: Long, period: Long): Flow<List<Coin>>

    /**
     * 获取交易列表
     */
    suspend fun getTransactionList(coin: Coin, type: Long, index: Long, size: Long): List<Transactions>

    /**
     * 通过hash查询交易
     */
    suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions?
}