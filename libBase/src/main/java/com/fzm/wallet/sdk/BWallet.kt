package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.alpha.Wallet
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
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

    val current: Flow<Wallet<Coin>>

    /**
     * SDK初始化方法
     *
     * @param context       Context
     * @param module        用于Koin依赖注入
     * @param platformId    平台码，用于php后端区别不同业务
     * @param appSymbol     应用标识符，用于账户网关后端区别不同业务
     * @param appId         平台码，用于账户网关后端区别不同业务
     * @param appKey        appKey，用于验证平台
     * @param device        设备，用于账户网关后端
     */
    fun init(context: Context, module: Module?, platformId: String, appSymbol: String, appId: String, appKey: String, device: String)


    fun setUrls(baseUrl:String,goUrl:String)

    /**
     * 切换账户
     */
    fun changeWallet(wallet: WalletBean?): Boolean

    /**
     * 切换账户
     */
    fun changeWallet(id: String): Boolean

    /**
     * 获取当前正在使用的账户
     *
     */
    fun getCurrentWallet(): WalletBean?

    /**
     * 获取用户所有的账户
     */
    suspend fun getAllWallet(user: String): List<WalletBean>

    /**
     * 获取指定id的账户
     *
     * @param id    账户id
     */
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "外部调用不需要用到PWallet内部类，PWallet内部类未来会不对外暴露",
        replaceWith = ReplaceWith("this.getWallet(id)")
    )
    fun findWallet(id: String?): PWallet?

    /**
     * 获取指定id的账户
     *
     * @param id    账户id
     */
    fun getWallet(id: String?): WalletBean?

    /**
     * 导入账户
     *
     * @param configuration     导入账户参数配置
     * @param switch            是否自动切换到新账户
     */
    @Throws(Exception::class)
    suspend fun importWallet(configuration: WalletConfiguration, switch: Boolean): String

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
     * 删除当前账户
     *
     * @param password  账户密码
     */
    suspend fun deleteWallet(password: String, confirmation: suspend () -> Boolean = { true })

    suspend fun addCoins(coins: List<Coin>, password: suspend () -> String)

    suspend fun deleteCoins(coins: List<Coin>)

    /**
     * 转账
     *
     * @param toAddress 目标地址
     * @param amount    转账金额
     * @param fee       矿工费
     * @param password  账户密码
     */
    @Throws(Exception::class)
    suspend fun transfer(coin: Coin, toAddress: String, amount: Double, fee: Double, note: String?, password: String): String

    /**
     * 获取资产余额列表
     *
     * @param initialDelay      初始延迟
     * @param period            查询间隔
     * @param requireQuotation  是否查询市场行情
     */
    fun getCoinBalance(initialDelay: Long, period: Long, requireQuotation: Boolean,
                       predicate: ((Coin) -> Boolean)? = null): Flow<List<Coin>>

    /**
     *
     * @param requireQuotation  是否查询市场行情
     */
    suspend fun getCoinBalance(coin: Coin, requireQuotation: Boolean): Coin

    /**
     * 获取交易列表
     *
     * @param type      账单类型
     * @param index     查询索引
     * @param size      查询数量
     */
    suspend fun getTransactionList(coin: Coin, type: Long, index: Long, size: Long): List<Transactions>

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
    suspend fun getAddress(chain: String): String

    suspend fun getAllCoins(): List<Coin>
    fun getCoinsFlow(): Flow<List<Coin>>

    /**
     * 获取区块链浏览器链接地址
     *
     * @param platform  平台码（平行链）
     */
    suspend fun getBrowserUrl(platform: String): String

    suspend fun getExploreList(): List<ExploreBean>
    suspend fun getExploreCategory(id:Int): List<ExploreBean>

    suspend fun getChainAssets(): List<AddCoinTabBean>

    /**
     *
     * @param page      页数
     * @param limit     每页数据条数
     * @param keywords  搜索关键词
     * @param chain     主链
     * @param platform  平台码
     */
    suspend fun searchCoins(page: Int, limit: Int, keywords: String, chain: String, platform: String): List<Coin>

    /**
     *
     * @param sort  排序权重
     */
    fun changeCoinOrder(coin: Coin, sort: Int)

    /**
     * 获取推荐手续费
     *
     * @param chain  主链
     */
    suspend fun getRecommendedFee(chain: String): Miner?

    /**
     *
     * @param chain     链名
     */
    suspend fun getMainCoin(chain: String): Coin?

    /**
     * 获取红包合约中的资产
     *
     * @param address   地址
     */
    suspend fun getRedPacketAssets(address: String): List<Coin>

    /**
     * 关闭账户
     */
    fun close()

    /**
     * 获取临时私钥
     */

     fun getBtyPrikey(): String?

}