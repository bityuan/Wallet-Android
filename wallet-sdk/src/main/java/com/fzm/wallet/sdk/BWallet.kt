package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.alpha.Wallet
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
     * @param appSymbol     应用标识符，用于钱包网关后端区别不同业务
     * @param appId         平台码，用于钱包网关后端区别不同业务
     * @param appKey        appKey，用于验证平台
     * @param device        设备，用于钱包网关后端
     */
    fun init(context: Context, module: Module?, platformId: String, appSymbol: String, appId: String, appKey: String, device: String)

    /**
     * 切换钱包
     */
    fun changeWallet(wallet: WalletBean?): Boolean

    /**
     * 切换钱包
     */
    fun changeWallet(id: String): Boolean

    /**
     * 获取当前正在使用的钱包
     *
     */
    fun getCurrentWallet(): WalletBean?

    /**
     * 获取用户所有的钱包
     */
    suspend fun getAllWallet(user: String): List<WalletBean>

    /**
     * 获取指定id的钱包
     *
     * @param id    钱包id
     */
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "外部调用不需要用到PWallet内部类，PWallet内部类未来会不对外暴露",
        replaceWith = ReplaceWith("this.getWallet(id)")
    )
    fun findWallet(id: String?): PWallet?

    /**
     * 获取指定id的钱包
     *
     * @param id    钱包id
     */
    fun getWallet(id: String?): WalletBean?

    /**
     * 导入钱包
     *
     * @param configuration     导入钱包参数配置
     * @param switch            是否自动切换到新钱包
     */
    @Throws(Exception::class)
    suspend fun importWallet(configuration: WalletConfiguration, switch: Boolean): String

    /**
     * 修改钱包名称
     */
    @Throws(Exception::class)
    suspend fun changeWalletName(name: String): Boolean

    /**
     * 修改钱包密码
     */
    @Throws(Exception::class)
    suspend fun changeWalletPassword(old: String, password: String): Boolean

    /**
     * 删除当前钱包
     *
     * @param password  钱包密码
     */
    suspend fun deleteWallet(password: String, confirmation: suspend () -> Boolean = { true })

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
     * 转账
     *
     * @param coin      币种
     * @param toAddress 目标地址
     * @param amount    转账金额
     * @param fee       矿工费
     * @param password  钱包密码
     */
    @Throws(Exception::class)
    suspend fun transfer(coin: Coin, toAddress: String, amount: Double, fee: Double, note: String?, password: String): String

    /**
     * 获取资产余额列表
     *
     * @param initialDelay      初始延迟
     * @param period            查询间隔
     * @param requireQuotation  是否查询市场行情
     * @param predicate         币种过滤
     */
    fun getCoinBalance(initialDelay: Long, period: Long, requireQuotation: Boolean,
                       predicate: ((Coin) -> Boolean)? = null): Flow<List<Coin>>

    /**
     * 获取单个币种的资产余额与行情
     *
     * @param coin              币种
     * @param requireQuotation  是否查询市场行情
     */
    suspend fun getCoinBalance(coin: Coin, requireQuotation: Boolean): Coin

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
    @Throws(Exception::class)
    suspend fun getTransactionByHash(chain: String, tokenSymbol: String, hash: String): Transactions

    /**
     * 根据主链获取地址
     *
     * @param chain         链名
     */
    suspend fun getAddress(chain: String): String

    /**
     * 获取所有币种
     */
    suspend fun getAllCoins(): List<Coin>

    /**
     * 获取所有币种
     */
    fun getCoinsFlow(): Flow<List<Coin>>

    /**
     * 获取区块链浏览器链接地址
     *
     * @param platform  平台码（平行链）
     */
    suspend fun getBrowserUrl(platform: String): String

    /**
     * 获取各主链的币种
     */
    suspend fun getChainAssets(): List<AddCoinTabBean>

    /**
     * 搜索币种
     *
     * @param page      页数
     * @param limit     每页数据条数
     * @param keywords  搜索关键词
     * @param chain     主链
     * @param platform  平台码
     */
    suspend fun searchCoins(page: Int, limit: Int, keywords: String, chain: String, platform: String): List<Coin>

    /**
     * 修改币种排序
     *
     * @param coin  币种
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
     * 获取所在链的主代币
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
     * 关闭钱包
     */
    fun close()

    /**
     * 获取临时私钥
     */

     fun getBtyPrikey(): String?

}