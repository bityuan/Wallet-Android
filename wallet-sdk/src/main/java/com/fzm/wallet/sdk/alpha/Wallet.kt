package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.bean.Transactions
import kotlinx.coroutines.flow.Flow

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
interface Wallet<T> {

    /**
     * 初始化钱包方法
     */
    suspend fun init(configuration: WalletConfiguration): String

    /**
     * 删除钱包
     *
     * @param password      钱包密码
     * @param confirmation  获取用户确认，默认允许
     */
    @Throws(Exception::class)
    suspend fun delete(
        password: String,
        confirmation: suspend () -> Boolean
    )

    /**
     * 转账方法
     */
    suspend fun transfer(coin: T, amount: Long)

    /**
     * 添加币种
     *
     * @param coins     需要添加的币种
     * @param password  获取用户密码（因为密码是可选项，因此用挂起函数形式）
     */
    @Throws(Exception::class)
    suspend fun addCoins(coins: List<T>, password: suspend () -> String)

    /**
     * 删除币种
     *
     */
    suspend fun deleteCoins(coins: List<T>)

    /**
     * 获取资产余额与行情
     */
    fun getCoinBalance(initialDelay: Long, period: Long, requireQuotation: Boolean): Flow<List<T>>

    /**
     * 获取交易列表
     *
     * @param coin      币种
     * @param type      账单类型
     * @param index     查询索引
     * @param size      查询数量
     */
    suspend fun getTransactionList(coin: T, type: Long, index: Long, size: Long): List<Transactions>

    /**
     * 通过hash查询交易
     *
     * @param chain         链名
     * @param tokenSymbol   币种symbol
     * @param hash          交易hash
     */
    suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions?
}