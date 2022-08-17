package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletBean
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import kotlinx.coroutines.flow.Flow

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
interface Wallet<T> : Cloneable {

    /**
     * 初始化账户方法
     */
    suspend fun init(configuration: WalletConfiguration): String

    /**
     * 获取账户id
     */
    fun getId(): String

    /**
     * 账户信息
     */
    val walletInfo: WalletBean

    /**
     * 修改账户名称
     */
    @Throws(Exception::class)
    suspend fun changeWalletName(name: String): Boolean

    /**
     * 修改账户密码
     */
    @Throws(Exception::class)
    suspend fun changeWalletPassword(old: String, password: String): Boolean

    /**
     * 删除账户
     *
     * @param password      账户密码
     * @param confirmation  获取用户确认，默认允许
     */
    @Throws(Exception::class)
    suspend fun delete(
        password: String,
        confirmation: suspend () -> Boolean
    ): Boolean

    /**
     * 转账方法
     *
     * @param toAddress 目标地址
     * @param amount    转账金额
     * @param fee       矿工费
     * @param password  账户密码
     */
    @Throws(Exception::class)
    suspend fun transfer(coin: T, toAddress: String, amount: Double, fee: Double, note: String?, password: String): String

    /**
     *
     * @param password  获取用户密码（因为密码是可选项，因此用挂起函数形式）
     */
    @Throws(Exception::class)
    suspend fun addCoins(coins: List<T>, password: suspend () -> String)

    /**
     *
     */
    suspend fun deleteCoins(coins: List<T>)

    /**
     * 获取资产余额与行情
     */
    fun getCoinBalance(
        requireQuotation: Boolean,
        predicate: ((Coin) -> Boolean)? = null
    ): Flow<List<T>>

    suspend fun getCoinBalance(coin: T, requireQuotation: Boolean): T

    /**
     * 获取交易列表
     *
     * @param type      账单类型
     * @param index     查询索引
     * @param size      查询数量
     */
    suspend fun getTransactionList(coin: T, type: Long, index: Long, size: Long): List<Transactions>

    /**
     * 通过hash查询交易
     *
     * @param chain         链名
     * @param hash          交易hash
     */
    @Throws(Exception::class)
    suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions

    /**
     * 根据主链获取地址
     *
     * @param chain         链名
     */
    suspend fun getAddress(chain: String): String?

    /**
     * 获取红包资产（不包含跨链资产）
     *
     * @param address   地址
     */
    suspend fun getRedPacketAssets(address: String): List<Coin>

    /**
     *
     * @param chain     链名
     */
    suspend fun getMainCoin(chain: String): Coin?

    /**
     * 关闭账户
     */
    fun close()
}