package com.fzm.walletmodule.utils

import android.util.Log
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.utils.GoWallet
import walletapi.*

class WalletRecoverUtils {

    public fun test(){

        try {

            //找回钱包
            val hdWallet1 =
                Walletapi.newWalletFromMnemonic_v2("BTY", "")
            val hdWallet2 =
                Walletapi.newWalletFromMnemonic_v2("ETH", "")

            val pub1 = Walletapi.byteTohex(hdWallet1.newKeyPub(0))
            val pub2 = Walletapi.byteTohex(hdWallet2.newKeyPub(0))

            val privkey1 = Walletapi.byteTohex(hdWallet1.newKeyPriv(0))
            val privkey2 = Walletapi.byteTohex(hdWallet2.newKeyPriv(0))


            val address1 = hdWallet1.newAddress_v2(0)//控制地址
            val address2 = hdWallet2.newAddress_v2(0)//找回地址
            //val X = "1to3VXGj7DkhyXEzxygmau6F379biWk8x"
            val X = "1FEAhVvtyaNjRqh3NLGC7s9z5goeodBHDW"


            val walletRecover = WalletRecover().apply {
                param = WalletRecoverParam().apply {
                    ctrPubKey = pub1
                    backupPubKey = pub2
                    addressID = 0
                    chainID = 0
                    relativeDelayTime = 30
                }
            }
            val recoverAddress = walletRecover.walletRecoverAddr
            Log.v("zx", recoverAddress)

            //使用控制地址提取X资产
            val walletTx = WalletTx().apply {
                cointype = "BTY"
                tokenSymbol = ""
                tx = Txdata().apply {
                    from = address2
                    amount = 0.2
                    fee = 0.01
                    note = "找回test"
                    to = "1P7P4v3kL39zugQgDDLRqxzGjQd7aEbfKs"
                }

                util = Util().apply {
                    node = "https://183.129.226.77:8083"
                }
            }


            val create = Walletapi.createRawTransaction(walletTx)
            val stringResult =
                JSON.parseObject(Walletapi.byteTostring(create), StringResult::class.java)
            val result = stringResult.result
            //ckrSend(walletRecover, result!!, privkey1)
            backSend(walletRecover, result!!, privkey2)


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //控制地址提取资产
    private fun ckrSend(walletRecover: WalletRecover, result: String, privkey: String) {
        val signtx =
            walletRecover.signRecoverTxWithCtrKey(Walletapi.stringTobyte(result), privkey)
        val sendRawTransaction = GoWallet.sendTran("BTY", signtx, "")
    }

    //备份地址（找回地址）提取资产
    private fun backSend(walletRecover: WalletRecover, result: String, privkey: String) {
        //找回地址提取资产
        val noneDelayTxParam = NoneDelayTxParam().apply {
            execer = "none"
            addressID = 0
            chainID = 0
            fee = 0.01
        }
        val noneDelaytx = walletRecover.createNoneDelayTx(noneDelayTxParam)
        val signtx2 = walletRecover.signRecoverTxWithBackupKey(
            Walletapi.stringTobyte(result),
            privkey,
            noneDelaytx
        )
        val sendRawTransaction2 = GoWallet.sendTran("BTY", signtx2, "")
    }
}