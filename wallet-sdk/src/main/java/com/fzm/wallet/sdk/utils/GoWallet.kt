package com.fzm.wallet.sdk.utils

import android.text.TextUtils
import android.util.Log
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.BWalletImpl
import com.fzm.wallet.sdk.MnemonicManager
import com.fzm.wallet.sdk.bean.response.BalanceResponse
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.UrlConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal.saveAll
import walletapi.*
import java.util.*

class GoWallet {
    companion object {
        const val BTY = "BTY"
        const val PLATFORM_BTY = "bty"
        const val APPSYMBOL_P = "p"

        private val gson = Gson()

        private val util = Util()

        private var lastRefreshSessionTime: Long = 0L

        fun getUtil(goNoderUrl: String): Util {
            util.node = goNoderUrl
            return util
        }


        /**
         *  创建助记词
         * @param mnemLangType Int  1 中文  2 英文
         * @return String
         */
        fun createMnem(mnemLangType: Int): String {
            return when (mnemLangType) {
                1 -> Walletapi.newMnemonicString(1, 160)
                2 -> Walletapi.newMnemonicString(0, 128)
                else -> Walletapi.newMnemonicString(1, 160)
            }
        }

        /**
         *
         * @param chain String   主链
         * @param mnem String    助记词
         * @return HDWallet?
         */
        @JvmStatic
        fun getHDWallet(chain: String, mnem: String): HDWallet? {
            try {
                return Walletapi.newWalletFromMnemonic_v2(chain, mnem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 公钥转地址
         * @param chain String   主链
         * @param pub  String  公钥
         * @return String?
         */
        fun pubToAddr(chain: String, pub: String): String? {
            try {
                return Walletapi.pubToAddress_v2(chain, Walletapi.hexTobyte(pub))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 私钥转地址
         * @param chain String   主链
         * @param priv String    私钥
         * @return String
         */
        fun privToAddr(chain: String, priv: String): String {
            val pub = Walletapi.privkeyToPub_v2(chain, Walletapi.hexTobyte(priv))
            return Walletapi.pubToAddress_v2(chain, pub)
        }

        fun byteTohex(byteArray: ByteArray): String {
            return Walletapi.byteTohex(byteArray)
        }

        private var session: WalletSession? = null

        fun setSessionInfo(session: WalletSession) {
            this.session = session
        }

        fun checkSessionID(force: Boolean = false) {
            if (System.currentTimeMillis() - lastRefreshSessionTime < 29 * 60 * 1000 && !force) {
                // sessionID半小时过期，提前1分钟刷新
                return
            }
            return try {
                Walletapi.setSessionID(Walletapi.getSessionId(session, getUtil(UrlConfig.GO_URL)))
                lastRefreshSessionTime = System.currentTimeMillis()
            } catch (e: Exception) {

            }
        }

        /**
         * 获取余额
         * @param addresss String  币种地址
         * @param chain String    主链名称，例如：“BTC”
         * @param tokenSymbol String   token名称，例如ETH下的“YCC”
         * @param goNoderUrl String    服务器节点
         * @return String?   币种余额data数据
         * 服务器挂掉{"id":1,"result":null,"error":"cointype EEE no support"}
         */
        fun getbalance(
            addresss: String,
            chain: String,
            tokenSymbol: String,
            goNoderUrl: String
        ): String? {
            try {
                checkSessionID()
                val balance = WalletBalance()
                balance.cointype = chain
                balance.address = addresss
                balance.tokenSymbol = if (chain == tokenSymbol) "" else tokenSymbol
                balance.util = getUtil(goNoderUrl)
                val getbalance = Walletapi.getbalance(balance)
                return Walletapi.byteTostring(getbalance)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 获取余额数据
         * @param addresss String  币种地址
         * @param chain String    主链名称，例如：“BTC”
         * @param tokenSymbol String   token名称，例如ETH下的“YCC”
         * @return String?   币种余额data数据
         * {"id": 1,"result": {"address": "0x632d8B07CDE8B2dcc3645148d2fa76647565664","balance": "0.02091716"},"error": null}
         */
        fun getbalance(addresss: String, chain: String, tokenSymbol: String): String? {
            return getbalance(addresss, chain, tokenSymbol, UrlConfig.GO_URL)
        }


        /**
         * 获取余额
         * @param lCoin Coin   币种
         * @return String?  余额
         */
        fun handleBalance(lCoin: Coin): String {
            var tokensymbol = lCoin.tokenSymbol/*if (lCoin.name == lCoin.chain) "" else lCoin.name
            if (!TextUtils.isEmpty(lCoin.platform) && !TextUtils.isEmpty(lCoin.chain)) {
                if (isBTYChild(lCoin)) {
                    if ("1" == lCoin.treaty) {
                        tokensymbol = lCoin.platform + "." + lCoin.name
                    } else if ("2" == lCoin.treaty) {
                        tokensymbol = lCoin.platform + ".coins"
                    }
                }
            }*/
            val balanceStr = getbalance(lCoin.address, lCoin.chain, tokensymbol)
            if (!TextUtils.isEmpty(balanceStr)) {
                val balanceResponse = gson.fromJson(balanceStr, BalanceResponse::class.java)
                if (balanceResponse != null) {
                    val balance = balanceResponse.result
                    if (balance != null) {
                        return balance.balance
                    }
                }
            }
            return lCoin.balance
        }

        fun handleRedPacketBalance(coin: Coin): String {
            if (!coin.isBty) return "0"
            var tokensymbol = if (coin.name == coin.chain) "" else coin.name
            if (coin.isBtyChild) {
                if ("1" == coin.treaty) {
                    tokensymbol = coin.platform + "." + coin.name
                } else if ("2" == coin.treaty) {
                    tokensymbol = coin.platform + ".coins"
                }
            }
            val balanceStr = getRedPacketBalance(coin, tokensymbol)
            if (!TextUtils.isEmpty(balanceStr)) {
                val balanceResponse = gson.fromJson(balanceStr, BalanceResponse::class.java)
                if (balanceResponse != null) {
                    val balance = balanceResponse.result
                    if (balance != null) {
                        return balance.balance
                    }
                }
            }
            return "0"
        }

        fun getRedPacketBalance(coin: Coin, symbol: String): String? {
            try {
                checkSessionID()
                val balance = WalletBalance().apply {
                    cointype = coin.chain
                    address = coin.address
                    tokenSymbol = symbol
                    util = getUtil(UrlConfig.GO_URL)
                    extendInfo = ExtendInfo().apply {
                        execer = if (coin.isBtyChild) "user.p.${coin.platform}.redpacket" else "redpacket"
                        assetExec = coin.assetExec
                        assetSymbol = coin.name
                    }
                }
                val getbalance = Walletapi.getbalance(balance)
                return Walletapi.byteTostring(getbalance)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun isBTYChild(coin: Coin): Boolean {
            return BTY == coin.chain && PLATFORM_BTY != coin.platform
        }

        /**
         * 获取交易记录
         * @param addr String    币种地址
         * @param chain String  主链名称，例如：“BTC”
         * @param tokenSymbol String   token名称，例如ETH下的“YCC”
         * @param type Long         交易账单类型（0全部 1入账，2出账）
         * @param page Long         页数
         * @param count Long        一页请求的条数
         * @param goNoderUrl String
         * @return String
         */
        fun getTranList(
            addr: String,
            chain: String,
            tokenSymbol: String,
            type: Long,
            page: Long,
            count: Long,
            goNoderUrl: String
        ): String? {
            try {
                checkSessionID()
                val walletQueryByAddr = WalletQueryByAddr()
                val queryByPage = QueryByPage()
                queryByPage.cointype = chain
                queryByPage.tokenSymbol = if (chain == tokenSymbol) "" else tokenSymbol
                queryByPage.address = addr
                queryByPage.count = count
                queryByPage.direction = 0
                queryByPage.index = page
                if (!type.equals(0)) {
                    queryByPage.type = type
                }
                walletQueryByAddr.queryByPage = queryByPage
                walletQueryByAddr.util = getUtil(goNoderUrl)
                val transaction = Walletapi.queryTransactionsByaddress(walletQueryByAddr)
                return Walletapi.byteTostring(transaction)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            return null
        }

        /**
         * 获取交易记录
         * @param addr String    币种地址
         * @param chain String  主链名称，例如：“BTC”
         * @param tokenSymbol String   token名称，例如ETH下的“YCC”
         * @param type Long        交易账单类型（0全部 1入账，2出账）
         * @param page Long        页数
         * @param count Long      一页请求的条数
         * @return String
         */
        fun getTranList(
            addr: String,
            chain: String,
            tokenSymbol: String,
            type: Long,
            page: Long,
            count: Long
        ): String? {
            return getTranList(addr, chain, tokenSymbol, type, page, count, UrlConfig.GO_URL!!)
        }

        /**
         *获取单笔交易详情
         * @param chain String   主链名称，例如：“BTC”
         * @param tokenSymbol String   token名称，例如ETH下的“YCC”
         * @param txid String   交易txid
         * @param goNoderUrl String   服务器节点
         * @return String?
         */
        fun getTranByTxid(
            chain: String,
            tokenSymbol: String,
            txid: String,
            goNoderUrl: String
        ): String? {
            try {
                checkSessionID()
                val walletQueryByTxid = WalletQueryByTxid()
                walletQueryByTxid.cointype = chain
                walletQueryByTxid.tokenSymbol = if (chain == tokenSymbol) "" else tokenSymbol
                walletQueryByTxid.txid = txid
                walletQueryByTxid.util = getUtil(goNoderUrl)
                val transaction =
                    Walletapi.queryTransactionByTxid(walletQueryByTxid)
                return Walletapi.byteTostring(transaction)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         *获取单笔交易详情
         * @param chain String   主链名称，例如：“BTC”
         * @param tokenSymbol String   token名称，例如ETH下的“YCC”
         * @param txid String   交易txid
         * @return String?
         */
        fun getTranByTxid(chain: String, tokenSymbol: String, txid: String): String? {
            return getTranByTxid(chain, tokenSymbol, txid, UrlConfig.GO_URL)
        }

        /**
         * 创建交易
         * @param cointype String  主链名称，例如：“BTC”
         * @param from String
         * @param to String
         * @param amount Double
         * @param fee Double
         * @param note String
         * @param tokensymbol String
         * @return String?
         */
        fun createTran(
            chain: String, fromAddr: String, toAddr: String, amount: Double, fee: Double,
            note: String, tokensymbol: String, goNoderUrl: String
        ): String? {
            try {
                checkSessionID()
                val walletTx = WalletTx()
                walletTx.cointype = chain
                walletTx.tokenSymbol = if (chain == tokensymbol) "" else tokensymbol
                val txdata = Txdata()
                txdata.amount = amount
                txdata.fee = fee
                txdata.from = fromAddr
                txdata.note = note
                txdata.to = toAddr
                walletTx.tx = txdata
                walletTx.util = getUtil(goNoderUrl)
                val createRawTransaction = Walletapi.createRawTransaction(walletTx)
                val createRawTransactionStr = Walletapi.byteTostring(createRawTransaction)
                Log.v("tag", "创建交易: $createRawTransactionStr")
                return createRawTransactionStr
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 创建交易(构造)
         * @param cointype String  主链名称，例如：“BTC”
         * @param from String
         * @param to String
         * @param amount Double
         * @param fee Double
         * @param note String
         * @param tokensymbol String
         * @return String?
         */
        fun createTran(
            chain: String, fromAddr: String, toAddr: String, amount: Double, fee: Double,
            note: String, tokensymbol: String
        ): String? {
            return createTran(chain, fromAddr, toAddr, amount, fee, note, tokensymbol, UrlConfig.GO_URL!!)
        }


        /**
         * 签名交易
         * @param chain String     主链名称，例如：“BTC”
         * @param unSignData String   创建交易后的数据（result）
         * @param priv String     私钥
         * @return String?
         */
        fun signTran(chain: String, unSignData: String, priv: String): String? {
            try {
                val signRawTransaction =
                    Walletapi.signRawTransaction(chain, Walletapi.stringTobyte(unSignData), priv)
                Log.v("tag", "签名交易: $signRawTransaction")
                return signRawTransaction
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 发送交易
         * @param chain String   主链名称，例如：“BTC”
         * @param signData String    token名称，例如ETH下的“YCC”
         * @param tokenSymbol String   签名后的数据
         * @param goNoderUrl String    服务器节点
         * @return String?
         */
        fun sendTran(
            chain: String,
            signData: String,
            tokenSymbol: String,
            goNoderUrl: String
        ): String? {
            try {
                checkSessionID()
                val sendTx = WalletSendTx()
                sendTx.cointype = chain
                sendTx.signedTx = signData
                sendTx.tokenSymbol = tokenSymbol
                sendTx.util = getUtil(goNoderUrl)
                val sendRawTransaction =
                    Walletapi.byteTostring(Walletapi.sendRawTransaction(sendTx))
                Log.v("tag", "发送交易: $sendRawTransaction")
                return sendRawTransaction
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 发送交易
         * @param chain String   主链名称，例如：“BTC”
         * @param signData String    token名称，例如ETH下的“YCC”
         * @param tokenSymbol String   签名后的数据
         * @return String?
         */
        fun sendTran(chain: String, signData: String, tokenSymbol: String): String? {
            return sendTran(chain, signData, tokenSymbol, UrlConfig.GO_URL!!)
        }

        /**
         * 平行链构造+签名
         * @param to String             入币地址
         * @param tokenSymbol String    coins币：链名.coins（xx.coins）；token币：链名.币名(xx.x)
         * @param execer String         执行器：coins币：user.p.链名.coins ；token币：user.p.链名.token
         * @param txpriv String         本地BTY的私钥
         * @param amount Double         数量
         * @param note String           备注
         * @param feePriv String      	代扣手续费的私钥
         * @param coinsForFee Boolean   coinsForFee 为true,代扣coins币作为手续费
         * @param tokenfee Double       代扣多少coins作为手续费，例如：0.001
         * @param tokenfeeAddr String   代扣的手续费接收地址
         * @param fee Double            代扣BTY作为整个交易的手续费，单笔交易最低0.001，交易组建议0.003
         * @return GsendTxResp
         */
        fun pcTran(
            to: String,
            tokenSymbol: String,
            execer: String,
            txpriv: String,
            amount: Double,
            note: String,
            feePriv: String,
            coinsForFee: Boolean,
            tokenfee: Double,
            tokenfeeAddr: String,
            fee: Double
        ): GsendTxResp {
            val gsendTx = GsendTx()
            gsendTx.to = to
            gsendTx.tokenSymbol = tokenSymbol
            gsendTx.execer = execer
            gsendTx.txpriv = txpriv
            gsendTx.amount = amount
            gsendTx.note = note
            gsendTx.feepriv = feePriv
            gsendTx.coinsForFee = coinsForFee
            gsendTx.tokenFee = tokenfee
            gsendTx.tokenFeeAddr = tokenfeeAddr
            gsendTx.fee = fee
            val gsendTxResp = Walletapi.coinsTxGroup(gsendTx)
            return gsendTxResp
        }

        /**
         * 校验密码(true :密码校验成功)
         * @param password String    没加密的密码
         * @param passwdHash String  加密后的哈希密码
         * @return Boolean
         */
        fun checkPasswd(password: String, passwdHash: String): Boolean {
            var checked = false
            try {
                checked = Walletapi.checkPasswd(password, passwdHash)
                return checked
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return checked
        }

        /**
         *  密码加密
         * @param password String   密码
         * @return ByteArray?
         */
        fun encPasswd(password: String): ByteArray? {
            try {
                return Walletapi.encPasswd(password)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 把加密的密码转换成哈希密码
         * @param password ByteArray   加密后的密码
         * @return String?
         */

        fun passwdHash(password: ByteArray): String? {
            try {
                return Walletapi.passwdHash(password)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }


        /**
         * 加密助记词
         * @param password ByteArray  加密后的密码
         * @param mnem String    助记词
         * @return String?
         */
        fun encMenm(encPasswd: ByteArray, seed: String): String? {
            try {
                val bSeed = Walletapi.stringTobyte(seed)
                val seedEncKey = Walletapi.seedEncKey(encPasswd, bSeed)
                return Walletapi.byteTohex(seedEncKey)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 解密助记词
         * @param password ByteArray  加密后的密码
         * @param mnem String      助记词
         * @return String?
         */
        fun decMenm(encPasswd: ByteArray, seed: String): String {
            try {
                val bSeed = Walletapi.hexTobyte(seed)
                val seedDecKey = Walletapi.seedDecKey(encPasswd, bSeed)
                return Walletapi.byteTostring(seedDecKey)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return ""
        }

        //byte[]转string 16进制

        fun encodeToStrings(b: ByteArray): String {
            return Walletapi.byteTohex(b)
        }

        fun deleteMulAddress(appId: String, appSymbol: String, mulAddress: String): Boolean? {
            checkSessionID()
            val mulAddr = WalletMulAddr()
            mulAddr.util = getUtil(UrlConfig.GO_URL!!)
            mulAddr.appid = appId
            mulAddr.appSymbol = appSymbol
            mulAddr.mulAddr = mulAddress
            return Walletapi.deleteMulAddress(mulAddr)
        }


        fun imortMulAddress(appId: String, appSymbol: String, mulAddress: String): Boolean? {
            checkSessionID()
            val mulAddr = WalletMulAddr()
            mulAddr.util = getUtil(UrlConfig.GO_URL!!)
            mulAddr.appid = appId
            mulAddr.appSymbol = appSymbol
            mulAddr.mulAddr = mulAddress
            return Walletapi.imortMulAddress(mulAddr)
        }


        internal suspend fun createWallet(wallet: PWallet, coinList: List<Coin>): PWallet {
            return withContext(Dispatchers.IO) {
                coinList.forEachIndexed { index, coin ->
                    val hdWallet = getHDWallet(coin.chain, wallet.mnem)
                    val pubkey = hdWallet!!.newKeyPub(0)
                    val address = hdWallet.newAddress_v2(0)
                    val pubkeyStr = encodeToStrings(pubkey)
                    coin.sort = index
                    coin.status = Coin.STATUS_ENABLE
                    coin.pubkey = pubkeyStr
                    coin.address = address
                    if (Walletapi.TypeBtyString == coin.chain) {
                        val bWalletImpl = BWallet.get() as BWalletImpl
                        bWalletImpl.setBtyPrivkey(encodeToStrings(hdWallet.newKeyPriv(0)))
                    }
                }
                saveAll(coinList)
                wallet.coinList.addAll(coinList)
                if (MnemonicManager.DEFAULT_STORE == MnemonicManager.store) {
                    // 如果是默认实现，则保存到wallet数据库中
                    val bpassword = encPasswd(wallet.password)
                    wallet.mnem = encMenm(bpassword!!, wallet.mnem)
                    wallet.password = passwdHash(bpassword)
                } else {
                    MnemonicManager.saveMnemonicWords(wallet.mnem, wallet.password)
                    wallet.mnem = null
                    wallet.password = null
                }
                wallet.save()
                return@withContext wallet
            }
        }
    }


    interface CoinListener {
        fun onSuccess()
    }

}