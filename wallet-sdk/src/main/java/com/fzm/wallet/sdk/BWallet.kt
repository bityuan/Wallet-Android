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
interface BWallet {

    companion object {

        private val wallet by lazy { BWalletImpl() }

        fun get(): BWallet = wallet
    }

    /**
     * SDK初始化方法
     *
     * @param context   Context
     * @param module    用于Koin依赖注入
     */
    fun init(context: Context, module: Module?)

    /**
     * 切换钱包
     */
    fun changeWallet(wallet: PWallet?): Boolean

    /**
     * 获取当前正在使用的钱包
     *
     * @param user  用户
     */
    fun getCurrentWallet(user: String = ""): PWallet?

    /**
     * 设置当前正在使用的钱包id
     *
     * @param user  用户
     * @param id    钱包id
     */
    fun setCurrentWalletId(user: String, id: Long)

    /**
     * 导入钱包
     *
     * @param user          用户唯一标识符
     * @param mnem          助记词
     * @param mnemType      助记词类型
     * @param walletName    钱包名称
     * @param password      钱包密码
     * @param coins         币种列表
     */
    @Throws(Exception::class)
    suspend fun importNormalWallet(user: String, mnem: String, mnemType: Int, walletName: String, password: String, coins: List<Coin>): String

    /**
     * 删除当前钱包
     *
     * @param password  钱包密码
     */
    suspend fun deleteWallet(password: suspend ()->String)

    /**
     * 添加币种
     *
     * @param coins     要添加的币种
     */
    suspend fun addCoins(coins: List<Coin>, password: suspend () -> String)

    /**
     * 删除币种
     *
     * @param coins     要删除的币种
     */
    suspend fun deleteCoins(coins: List<Coin>)

    /**
     * 获取资产余额列表
     *
     * @param initialDelay      初始延迟
     * @param period            查询间隔
     * @param requireQuotation  是否查询市场行情
     */
    fun getCoinBalance(initialDelay: Long, period: Long, requireQuotation: Boolean): Flow<List<Coin>>

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