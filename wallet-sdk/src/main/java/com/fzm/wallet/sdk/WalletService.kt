package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
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

    /**
     * 初始化方法
     *
     * @param context   Context
     * @param module    用于Koin依赖注入
     */
    fun init(context: Context, module: Module?)

    /**
     * 导入钱包
     *
     * @param mnem          助记词
     * @param mnemType      助记词类型
     * @param walletName    钱包名称
     * @param password      钱包密码
     */
    @Throws(Exception::class)
    suspend fun importMnem(mnem: String, mnemType: Int, walletName: String, password: String): PWallet

    /**
     * 获取资产余额列表
     *
     * @param walletId          钱包id
     * @param initialDelay      初始延迟
     * @param period            查询间隔
     * @param requireQuotation  是否查询市场行情
     */
    fun getCoinBalance(walletId: Long, initialDelay: Long, period: Long, requireQuotation: Boolean): Flow<List<Coin>>

    /**
     * 获取交易列表
     *
     * @param coin      币种
     * @param type      账单类型
     * @param index     查询索引
     * @param size      查询数量
     */
    suspend fun getTransactionList(coin: Coin, type: Long, index: Long, size: Long): List<Transactions>

    /**
     * 通过hash查询交易
     *
     * @param chain         链名
     * @param tokenSymbol   币种symbol
     * @param hash          交易hash
     */
    suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions?
}