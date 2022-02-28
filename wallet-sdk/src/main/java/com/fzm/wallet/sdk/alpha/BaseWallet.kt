package com.fzm.wallet.sdk.alpha

import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.MnemonicManager
import com.fzm.wallet.sdk.WalletBean
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.bean.response.TransactionResponse
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.toWalletBean
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.litepal.LitePal
import org.litepal.extension.find
import java.util.*

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
abstract class BaseWallet(protected val wallet: PWallet) : Wallet<Coin> {

    protected val gson by lazy { Gson() }
    protected val walletRepository by lazy { rootScope.get<WalletRepository>(walletQualifier) }

    override fun getId(): String {
        return wallet.id.toString()
    }

    override val walletInfo: WalletBean
        get() = wallet.toWalletBean()

    override suspend fun changeWalletName(name: String): Boolean {
        val wallets = LitePal.where("name = ?", name).find(PWallet::class.java)
        if (!wallets.isNullOrEmpty()) {
            throw Exception("账户名称重复")
        }
        wallet.name = name
        return wallet.update(wallet.id) != 0
    }

    override suspend fun changeWalletPassword(old: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (wallet.password.isNullOrEmpty()) {
                setPassword(password)
            } else {
                changePassword(old, password)
            }
        }
    }

    private suspend fun setPassword(password: String): Boolean {
        val mnem = MnemonicManager.getMnemonicWords(wallet.password)
        return MnemonicManager.saveMnemonicWords(mnem, password)
    }

    private suspend fun changePassword(old: String, password: String): Boolean {
        if (!MnemonicManager.checkPassword(old)) {
            throw Exception("密码错误")
        }
        val mnem = MnemonicManager.getMnemonicWords(old)
        return MnemonicManager.saveMnemonicWords(mnem, password)
    }

    override suspend fun delete(password: String, confirmation: suspend () -> Boolean): Boolean {
        val verified = withContext(Dispatchers.IO) {
            MnemonicManager.checkPassword(password)
        }
        if (verified) {
            if (confirmation()) {
                withContext(Dispatchers.IO) {
                    LitePal.delete(PWallet::class.java, wallet.id)
                }
                return true
            }
            return false
        } else {
            throw IllegalArgumentException("密码输入错误")
        }
    }

    override suspend fun transfer(
        coin: Coin,
        toAddress: String,
        amount: Double,
        fee: Double,
        note: String?,
        password: String
    ): String {
        return withContext(Dispatchers.IO) {
            val result = MnemonicManager.checkPassword(password)
            if (!result) {
                throw Exception("密码输入错误")
            }
            val mnem = MnemonicManager.getMnemonicWords(password)
            val privateKey = coin.getPrivkey(coin.chain, mnem)?: throw Exception("私钥获取失败")

            val tokenSymbol = if (coin.name == coin.chain) "" else coin.name
            // 构造交易
            val rawTx = GoWallet.createTran(
                coin.chain,
                coin.address,
                toAddress,
                amount,
                fee,
                note ?: "",
                tokenSymbol
            )
            val createRawResult = JSON.parseObject(rawTx, StringResult::class.java)
            if (createRawResult?.result == null) {
                throw Exception("创建交易失败")
            }

            // 签名交易
            val signTx = GoWallet.signTran(coin.chain, createRawResult.result ?: "", privateKey)
                ?: throw Exception("签名交易失败")

            // 发送交易
            val sendTx = GoWallet.sendTran(coin.chain, signTx, tokenSymbol)
            val sendResult = gson.fromJson(sendTx, StringResult::class.java)
            val txId = sendResult.result
            if (sendResult == null) {
                throw Exception("获取结果失败，请至区块链浏览器查看")
            }
            if (!sendResult.error.isNullOrEmpty()) {
                throw Exception(sendResult.error)
            }
            if (txId.isNullOrEmpty()) {
                throw Exception("获取结果失败，请至区块链浏览器查看")
            }
            txId
        }
    }

    override suspend fun addCoins(coins: List<Coin>, password: suspend () -> String) {
        withContext(Dispatchers.IO) {
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
        }
    }

    @Throws(Exception::class)
    private suspend fun checkCoin(coin: Coin, password: suspend () -> String) {
        if (coin.chain == null) return
        val existNum = LitePal.where("pwallet_id = ?", wallet.id.toString()).count(Coin::class.java)
        val sameChainCoin =
            LitePal.select().where("chain = ? and pwallet_id = ?", coin.chain, wallet.id.toString())
                .findFirst(Coin::class.java)
        if (sameChainCoin != null) {
            coin.address = sameChainCoin.address
            coin.pubkey = sameChainCoin.pubkey
            coin.sort = existNum
            coin.setPrivkey(sameChainCoin.encPrivkey)
            coin.setpWallet(wallet)
            if (coin.id != 0L) {
                coin.update(coin.id)
            } else {
                coin.save()
            }
        } else {
            val pass = password()
            if (!MnemonicManager.checkPassword(pass)) {
                throw Exception("密码输入错误")
            }
            val mnem = MnemonicManager.getMnemonicWords(pass)
            if (mnem.isEmpty()) {
                throw Exception("助记词解密失败")
            }
            val hdWallet = GoWallet.getHDWallet(coin.chain, mnem) ?: throw Exception("创建主链失败")
            coin.address = hdWallet.newAddress_v2(0)
            coin.pubkey = GoWallet.encodeToStrings(hdWallet.newKeyPub(0))
            coin.sort = existNum
            coin.setpWallet(wallet)
            coin.save()
        }
    }

    override suspend fun deleteCoins(coins: List<Coin>) = withContext(Dispatchers.IO) {
        for (c in coins) {
            c.status = Coin.STATUS_DISABLE
            c.update(c.id)
        }
    }

    override fun getCoinBalance(
        initialDelay: Long,
        period: Long,
        requireQuotation: Boolean,
        predicate: ((Coin) -> Boolean)?
    ): Flow<List<Coin>> = flow {
        if (initialDelay > 0) delay(initialDelay)
        var initEmit = true
        while (true) {
            supervisorScope {
                val coins = LitePal.where(
                    "pwallet_id = ? and status = ?",
                    wallet.id.toString(),
                    Coin.STATUS_ENABLE.toString()
                ).find(Coin::class.java, true).let {
                    if (predicate == null) it else it.filter(predicate)
                }
                if (coins.isEmpty()) {
                    emit(emptyList())
                    return@supervisorScope
                }
                val deferred = ArrayDeque<Deferred<Unit>>()
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

    override suspend fun getCoinBalance(coin: Coin, requireQuotation: Boolean): Coin {
        return withContext(Dispatchers.IO) {
            try {
                coin.balance = GoWallet.handleBalance(coin)
            } catch (e: Exception) {
                // 资产获取异常
            }
            if (requireQuotation || coin.nickname.isNullOrEmpty()) {
                // 查询资产行情等
                val result = walletRepository.getCoinList(listOf("${coin.name},${coin.platform}"))
                result.dataOrNull()?.firstOrNull()?.also { meta ->
                    coin.rmb = meta.rmb
                    coin.icon = meta.icon
                    coin.nickname = meta.nickname
                }
            }
            coin.update(coin.id)
            coin
        }
    }

    override suspend fun getTransactionList(
        coin: Coin,
        type: Long,
        index: Long,
        size: Long
    ): List<Transactions> {
        return withContext(Dispatchers.IO) {
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
                return@withContext gson.fromJson(local, TransactionResponse::class.java).result ?: emptyList()
            }
            if (index == 0L) {
                // 缓存第一页数据
                MMkvUtil.encode(getKey(coin, type), data)
            }
            return@withContext gson.fromJson(data, TransactionResponse::class.java).result ?: emptyList()
        }
    }

    override suspend fun getTransactionByHash(
        chain: String,
        tokenSymbol: String,
        hash: String
    ): Transactions {
        return withContext(Dispatchers.IO) {
            val data = GoWallet.getTranByTxid(chain, tokenSymbol, hash)
            if (data.isNullOrEmpty()) throw Exception("查询数据为空")
            val response = gson.fromJson<GoResponse<Transactions>>(
                data,
                object : TypeToken<GoResponse<Transactions>>() {}.type
            )
            if (response.error == null) {
                response.result ?: throw Exception("查询结果为空")
            } else {
                throw Exception(response.error)
            }
        }
    }

    override suspend fun getAddress(chain: String): String? {
        val coinList = LitePal.select()
            .where("chain = ? and pwallet_id = ?", chain, wallet.id.toString())
            .find<Coin>(true)
        return coinList.let { it.firstOrNull()?.address }
    }

    override fun close() {
        wallet.password = null
        wallet.isPutpassword = false
        wallet.mnem = null
        wallet.save()
    }

    protected fun getChineseMnem(mnem: String): String {
        val afterString = mnem.replace(" ", "")
        val afterString2 = afterString.replace("\n", "")
        return afterString2.replace("", " ").trim()
    }

    protected fun getKey(coin: Coin, type: Long): String =
        "${coin.chain}${coin.address}${coin.name}$type"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseWallet

        if (wallet != other.wallet) return false

        return true
    }

    override fun hashCode(): Int {
        return wallet.hashCode()
    }


}