package com.fzm.walletmodule.utils

import android.util.Log
import com.fzm.walletmodule.api.ApiEnv
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.db.entity.PWallet
import org.litepal.LitePal.select
import walletapi.*
import java.lang.Exception

/**
 *@author zx
 *@since 2021/12/9
 */
object GoUtils {
    fun getBTY(): Coin? {
        val id: Long = PWallet.getUsingWalletId()
        val coinList: List<Coin> =
            select().where("chain = ? and pwallet_id = ?", Walletapi.TypeBtyString, id.toString())
                .find(Coin::class.java, true)

        return if (!ListUtils.isEmpty(coinList)) {
            coinList[0]
        } else null
    }

    fun getETH(): Coin? {
        val id = PWallet.getUseWalletId()
        val coinList =
            select().where("name = ? and pwallet_id = ?", Walletapi.TypeETHString, id.toString())
                .find(
                    Coin::class.java, true)
        return if (!ListUtils.isEmpty(coinList)) {
            coinList[0]
        } else null
    }


    fun checkPasswd(password: String, passwdHash: String): Boolean {
        var checked = false
        try {
            checked = Walletapi.checkPasswd(password, passwdHash)
            return checked
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return checked
    }


    //密码加密
    fun encPasswd(password: String): ByteArray? {
        try {
            return Walletapi.encPasswd(password)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    //对助记词进行解密
    fun seedDecKey(encPasswd: ByteArray, seed: String): String {
        try {
            val bSeed = Walletapi.hexTobyte(seed)
            val seedDecKey = Walletapi.seedDecKey(encPasswd, bSeed)
            return Walletapi.byteTostring(seedDecKey)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }


    //签名交易
    fun signRawTransaction(cointype: String, bytes: ByteArray, priv: String): String {
        try {
            val signRawTransaction = Walletapi.signRawTransaction(cointype, bytes, priv)
            return signRawTransaction
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun signTxGroup(
        execer: String,
        createTx: String,
        tsPriv: String,
        feePriv: String,
        btyfee: Double,
    ): String {
        try {
            val gWithoutTx = GWithoutTx()
            gWithoutTx.noneExecer = execer
            gWithoutTx.feepriv = feePriv //代扣手续费的BTY私钥
            gWithoutTx.txpriv = tsPriv
            gWithoutTx.rawTx = createTx
            //bty的推荐手续费设置
            gWithoutTx.fee = btyfee
            val txResp = Walletapi.coinsWithoutTxGroup(gWithoutTx)
            return txResp.signedTx
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    //发送交易
    fun sendRawTransaction(cointype: String, signtx: String, tokenSymbol: String): String {
        try {
            val sendTx = WalletSendTx()
            sendTx.cointype = cointype
            sendTx.signedTx = signtx
            sendTx.tokenSymbol = tokenSymbol
            sendTx.util = GoUtils.getUtil()
            val sendRawTransaction = Walletapi.byteTostring(Walletapi.sendRawTransaction(sendTx))
            Log.v("tag", "发送交易: $sendRawTransaction")
            return sendRawTransaction
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun getbalance(cointype: String, addresss: String, tokenSymbol: String): String {
        try {
            val balance = WalletBalance()
            balance.cointype = cointype
            balance.address = addresss
            balance.tokenSymbol = tokenSymbol
            balance.util = getUtil()
            val getbalance = Walletapi.getbalance(balance)
            val balanceStr = Walletapi.byteTostring(getbalance)
            return balanceStr
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0"
    }

    fun getUtil(): Util? {
        val util = Util()
        util.node = ApiEnv.getGoURL()
        return util
    }

    fun hexTobyte(s: String?): ByteArray? {
        try {
            return Walletapi.hexTobyte(s)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun byteTohex(b: ByteArray?): String? {
        try {
            return Walletapi.byteTohex(b)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}