package com.fzm.walletmodule.ui.base

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.fzm.walletmodule.R
import com.fzm.walletmodule.api.AndroidWebBridge
import com.fzm.walletmodule.bean.*
import com.fzm.walletmodule.bean.response.BalanceResponse
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.db.entity.PWallet
import com.fzm.walletmodule.utils.*
import com.fzm.walletmodule.vm.OutViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.dialog_dapp.view.*
import kotlinx.android.synthetic.main.dialog_put_password.view.*
import kotlinx.android.synthetic.main.dialog_put_password.view.btn_ok
import kotlinx.android.synthetic.main.dialog_put_password.view.et_input
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal.select
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import walletapi.*
import wendu.dsbridge.CompletionHandler
import wendu.dsbridge.DWebView
import java.lang.Exception
import java.lang.NullPointerException

/**
 *@author zx
 *@since 2021/12/9
 */
open class BaseWebActivity : BaseActivity() {

    //    //签名
    private var mHandleSignFunction: CompletionHandler<String>? = null
    private var mHandleSignTxGroupFunction: CompletionHandler<String>? = null
    private var mConfigPrivFunction: CompletionHandler<String>? = null

    private val mImportSeedFunction: CompletionHandler<String>? = null
    private val mGetDeviceFunction: CompletionHandler<String>? = null
    private val mScanFunction: CompletionHandler<String>? = null
    private val mCheckPayPwdFunction: CompletionHandler<String>? = null


    private val outViewModel: OutViewModel by inject()


    fun initWebView(dWebView: DWebView) {
        initData()
        dWebView.addJavascriptObject(AndroidWebBridge(dWebView, this), "")
    }

    private var mWithHold: WithHold? = null

    //代扣私钥
    private var withHoldPriv: String? = null

    //交易组BTY代扣手续费
    private var signGroupFee = 0.005


    override fun initData() {
        super.initData()
        outViewModel.getWithHold.observe(this, Observer {
            if (it.isSucceed()) {
                mWithHold = it.data()
                withHoldPriv = mWithHold?.getPrivate_key()
                signGroupFee = mWithHold?.getBtyFee()?.times(5)!!
            }
        })
    }


    open fun handleSign(msg: Any, handler: CompletionHandler<String>) {
        mHandleSignFunction = handler
        val hashMap = Gson().fromJson<HashMap<String, String>>(msg.toString(), HashMap::class.java)
        val createHash = hashMap.get("createHash") as String
        if (mCachePriv == 1) {
            doSign(createHash)
        } else {
            val view = initDialog(R.layout.dialog_put_password)
            view.btn_ok.setOnClickListener {
                val password = view.et_input.text.toString()
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                decPrivkey(password, createHash, "", 8)
            }

        }
    }


    open fun handleSignTxGroup(msg: Any, handler: CompletionHandler<String>) {
        mHandleSignTxGroupFunction = handler
        val hashMap = Gson().fromJson<HashMap<String, Any>>(msg.toString(), HashMap::class.java)
        val createHash = hashMap.get("createHash") as String
        val exer = hashMap.get("exer") as String
        val withhold = hashMap.get("withhold") as Int

        if (mCachePriv == 1) {
            doSignGroup(createHash, exer, withhold)
        } else {
            val view = initDialog(R.layout.dialog_put_password)
            view.btn_ok.setOnClickListener {
                val password = view.et_input.text.toString()
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                decPrivkey(password, createHash, exer, withhold)
            }
        }
    }


    private fun doSign(createHash: String) {
        val bcreateRaw: ByteArray = GoUtils.hexTobyte(createHash)!!
        val signHash = GoUtils.signRawTransaction("BTY", bcreateRaw, Coin.webPriv)
        val map = mapOf("signHash" to signHash)
        mHandleSignFunction!!.complete(Gson().toJson(map))
    }

    private fun doSignGroup(createHash: String, exer: String, withHold: Int) {
        if (null == mWithHold) {
            Toast.makeText(this, "配置错误", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(createHash)) {
            Toast.makeText(this, "构造错误", Toast.LENGTH_SHORT).show()
            return
        }
        var length = 3
        if (createHash.contains(",")) {
            val split = createHash.split(",")
            length = split.size + 2
        }

        val privKey = if (withHold == -1) Coin.webPriv else withHoldPriv

        val signHash = GoUtils.signTxGroup(exer,
            createHash,
            Coin.webPriv,
            privKey,
            mWithHold?.getBtyFee()!!.times(length))
        val map = mapOf("signHash" to signHash)
        mHandleSignTxGroupFunction!!.complete(Gson().toJson(map))
    }

    //输入密码，解密私钥
    private fun decPrivkey(password: String, createHash: String, exer: String, withHold: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            var btyCoin: Coin? = null
            withContext(Dispatchers.IO) {
                btyCoin = GoUtils.getBTY()
            }
            if (btyCoin == null) {
                Toast.makeText(this@BaseWebActivity, "BTY地址获取失败", Toast.LENGTH_SHORT).show()
            } else {
                var result = false
                showLoading()
                withContext(Dispatchers.IO) {
                    result = GoUtils.checkPasswd(password, btyCoin?.getpWallet()?.password)
                }
                if (result) {
                    dismiss()
                    withContext(Dispatchers.IO) {
                        val bPassword = GoUtils.encPasswd(password)
                        val mnem = GoUtils.seedDecKey(bPassword!!, btyCoin?.getpWallet()?.mnem!!)
                        Coin.webPriv = btyCoin?.getPrivkey(btyCoin?.chain, mnem)
                    }
                    dismiss()
                    if (mCachePriv == 1) {
                        val map = mapOf("status" to 1)
                        mConfigPrivFunction?.complete(Gson().toJson(map))
                    } else {
                        when (withHold) {
                            8 -> doSign(createHash)
                            else -> doSignGroup(createHash, exer, withHold)
                        }
                    }

                } else {
                    Toast.makeText(this@BaseWebActivity, "密码输入错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private var mCachePriv = 0

    //配置私钥方法：configPriv
    // 入参：cachePriv;//0:默认 1：缓存私钥 -1：清除私钥
    // 返回参数 status  1：操作成功 -1：操作失败
    open fun handleConfigPri(msg: Any, handler: CompletionHandler<String>) {
        mConfigPrivFunction = handler
        try {
            val hashMap = Gson().fromJson<HashMap<String, Any>>(msg.toString(), HashMap::class.java)
            mCachePriv = hashMap.get("cachePriv") as Int

        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (mCachePriv == 1) {
            if (TextUtils.isEmpty(Coin.webPriv)) {
                val view = initDialog(R.layout.dialog_put_password)
                view.btn_ok.setOnClickListener {
                    val password = view.et_input.text.toString()
                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    decPrivkey(password, "", "", 0)
                }
            } else {
                val map = mapOf("status" to 1)
                mConfigPrivFunction?.complete(Gson().toJson(map))
            }
        } else if (mCachePriv == -1) {
            Coin.webPriv = ""
        }
    }

    fun browserOpen(msg: Any, handler: CompletionHandler<String>) {
        val hashMap = Gson().fromJson<HashMap<String, Any>>(msg.toString(), HashMap::class.java)
        val url = hashMap["url"] as String
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    fun getAddress(msg: Any, handler: CompletionHandler<String>){
        val hashMap = Gson().fromJson<HashMap<String, Any>>(msg.toString(), HashMap::class.java)
        val cointype = hashMap["cointype"] as String
        val coinList = select("address").where("chain = ? and name = ? and pwallet_id = ?",
            cointype,
            cointype,
            java.lang.String.valueOf(PWallet.getUseWalletId())).find(
            Coin::class.java)

        val map = mapOf("address" to coinList[0].address)
        handler.complete(Gson().toJson(map))
    }

    private fun initDialog(layout: Int): View {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(layout, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)
        dialog.show()
        return view
    }



    //---------------------------------跨链桥---------------------------------


    val ETH_LOCK = 5
    val ETH_UNLOCK = 6
    val EVM_TO_EXCHANGE = 7
    val EXCHANGE_TO_EVM = 8
    val MAIN_TO_EXCHANGE = 9
    val EXCHANGE_TO_MAIN = 10

    //-----------------------------------------------------------------------------------
    val noneExecer = "user.p.para.none"
    val EvmExecer = "user.p.para.evm"
    val XgoExecer = "user.p.para.evmxgo"
    val ExchangeExecer = "user.p.para.exchange"

fun mainToExchange(msg: Any, handler: CompletionHandler<String>) {
        doPara(msg, MAIN_TO_EXCHANGE, handler)
    }

    fun exchangeToMain(msg: Any, handler: CompletionHandler<String>) {
        doPara(msg, EXCHANGE_TO_MAIN, handler)
    }

    //扣除ETH作为手续费
    private var ethFee = false
    private var ethBalance = 0.0
    fun ethLock(msg: Any, handler: CompletionHandler<String>) {
        ethFee = true
        doChain33ETH(msg, ETH_LOCK, handler)
    }

    fun ethUnlock(msg: Any, handler: CompletionHandler<String>) {
        doChain33ETH(msg, ETH_UNLOCK, handler)
    }

    fun evmToExchange(msg: Any, handler: CompletionHandler<String>) {
        doChain33ETH(msg, EVM_TO_EXCHANGE, handler)
    }

    fun exchangeToEvm(msg: Any, handler: CompletionHandler<String>) {
        doChain33ETH(msg, EXCHANGE_TO_EVM, handler)
    }

    internal class Paracross {
        var amount = 0.0
        var fromExecer: String? = null
        var toExecer: String? = null
        var noneExecer: String? = null
        var fee = 0.0
        var groupFee = 0.0
        var chainId = 0
        var note: String? = null
    }

    private lateinit var paraHandle: CompletionHandler<String>
    private var handleType = 0
    private var paracross: Paracross? = null
    private fun doPara(msg: Any, type: Int, handler: CompletionHandler<String>) {
        if (null == mWithHold) {
            Toast.makeText(this@BaseWebActivity, "配置错误", Toast.LENGTH_SHORT).show()
            return
        }
        handleType = 1
        paraHandle = handler
        localBty = GoUtils.getBTY()
        if (localBty != null) {
            Log.v("doPara", msg.toString())
            paracross = Gson().fromJson(msg.toString(), Paracross::class.java)
            showCrossChainDialog(localBty, type, handler)
        }
    }

    private var localBty: Coin? = null
    private var localEth: Coin? = null
    private lateinit var chain33ETH: Chain33ETH
    private lateinit var chain33EthHandle: CompletionHandler<String>
    private fun doChain33ETH(msg: Any, type: Int, handler: CompletionHandler<String>) {
        try {
            if (null == mWithHold) {
                Toast.makeText(this@BaseWebActivity, "配置错误", Toast.LENGTH_SHORT).show()
                return
            }
            handleType = 2
            chain33EthHandle = handler
            localBty = GoUtils.getBTY()
            localEth = GoUtils.getETH()
            if (localBty != null && localEth != null) {
                chain33ETH = Gson().fromJson<Chain33ETH>(msg.toString(), Chain33ETH::class.java)
                if (type == ETH_LOCK) {
                    showCrossChainDialog(localEth, type, handler)
                } else if (type == ETH_UNLOCK) {
                    showCrossChainDialog(localBty, type, handler)
                } else if (type == EVM_TO_EXCHANGE) {
                    showCrossChainDialog(localBty, type, handler)
                } else if (type == EXCHANGE_TO_EVM) {
                    showCrossChainDialog(localBty, type, handler)
                }
            } else {
                Toast.makeText(this@BaseWebActivity, "请添加BTY和ETH主链币种", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cacheSendParacrossTx(crossType: Int, signedRawTxs: String) {
        //不直接发送
        try {
            val sendParacrossTxsReq = SendParacrossTxsReq()
            sendParacrossTxsReq.setCointype(Walletapi.TypeBtyString)
            sendParacrossTxsReq.setAddress(localBty!!.address)
            sendParacrossTxsReq.setCrossType(crossType.toLong())
            sendParacrossTxsReq.setParaName(paracross!!.toExecer)
            sendParacrossTxsReq.setSignedRawTxs(signedRawTxs)
            Log.v("cacheSendParacrossTx", sendParacrossTxsReq.toString())
            val bytes: ByteArray =
                Walletapi.cacheSendParacrossTx(sendParacrossTxsReq, GoUtils.getUtil())
            complete(Walletapi.byteTostring(bytes))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doMainToExchange() {
        try {
            val req = ParaCrossReq()
            req.setAddr(localBty!!.address)
            req.setAmount(paracross!!.amount)
            req.setNote(paracross!!.note)
            req.setFromExecer(paracross!!.fromExecer)
            req.setToExecer(paracross!!.toExecer)
            req.setChainID(paracross!!.chainId)
            req.setFee(0.0)
            Log.v("maintoexchange", req.getAmount().toString() + "," + req.getFee())
            val createTx: String = Walletapi.paracrossMain2ParaExec(req)
            val split = createTx.split("\\|").toTypedArray()
            if (!ArrayUtils.isEmpty(split)) {
                val coinsTx = split[0]
                val groupTx = split[1]
                val sign = sendGroup(coinsTx, "none")
                val signGroup = sendGroup(groupTx, paracross!!.noneExecer)
                cacheSendParacrossTx(0, "$sign,$signGroup")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doExchangeToMain() {
        try {
            val req = ParaCrossReq()
            req.setAddr(localBty!!.address)
            req.setAmount(paracross!!.amount)
            req.setNote(paracross!!.note)
            req.setFromExecer(paracross!!.fromExecer)
            req.setToExecer(paracross!!.toExecer)
            req.setChainID(paracross!!.chainId)
            req.setFee(0.0)
            Log.v("exchangetomain", req.getAmount().toString() + "," + req.getFee())
            val createTx: String = Walletapi.paracrossParaExec2Main(req)
            val split = createTx.split("\\|").toTypedArray()
            if (!ArrayUtils.isEmpty(split)) {
                val groupTx = split[0]
                val coinsTx = split[1]
                val signGroup = sendGroup(groupTx, paracross!!.noneExecer)
                val sign = sendGroup(coinsTx, "none")
                cacheSendParacrossTx(1, "$signGroup,$sign")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doEthLock() {
        try {
            val data = Txdata()
            data.setFrom(localEth!!.address)
            data.setTo(chain33ETH.getBridgeBankContractAddr())
            data.setAmount(chain33ETH.getAmount())
            data.setFee(crossFee)
            data.setNote("测试备注")
            val extend = Extend()
            extend.setMethod("lock")
            //如果是lock YCC，那么改变这里的值,如果是ETH，填""
            extend.setTokenAddr(chain33ETH.getContractTokenAddr())
            extend.setChain33Reciver(localBty!!.address)
            data.setExtend(extend)
            val tx = WalletTx()
            tx.setUtil(GoUtils.getUtil())
            tx.setCointype(chain33ETH.getCointype())
            tx.setTokenSymbol(chain33ETH.getTokenSymbol())
            tx.setExecer(chain33ETH.getExecer())
            tx.setTx(data)
            Log.v("doEthLock", tx.toString())
            val bCreate: ByteArray = Walletapi.ethLock(tx)
                ?: throw NullPointerException("ethLock返回为null")
            val create = parseCreateResult(Walletapi.byteTostring(bCreate))
            if (!TextUtils.isEmpty(extend.getTokenAddr())) {
                val split = create.split("@").toTypedArray()
                val sign: String =
                    GoUtils.signRawTransaction(chain33ETH.getCointype(), Walletapi.hexTobyte(
                        split[0]), Coin.webPriv)
                val sign2: String =
                    GoUtils.signRawTransaction(chain33ETH.getCointype(), Walletapi.hexTobyte(
                        split[1]), Coin.webPriv)
                //第一个交易是approve，不扣币，第二笔失败了也没事儿
                val send: String = GoUtils.sendRawTransaction(chain33ETH.getCointype(),
                    sign,
                    chain33ETH.getTokenSymbol())
                Thread.sleep(20000)
                val send2: String = GoUtils.sendRawTransaction(chain33ETH.getCointype(),
                    sign2,
                    chain33ETH.getTokenSymbol())
                complete(send2)
                return
            }
            val sign: String = GoUtils.signRawTransaction(chain33ETH.getCointype(),
                Walletapi.hexTobyte(create),
                Coin.webPriv)
            val send: String = GoUtils.sendRawTransaction(chain33ETH.getCointype(),
                sign,
                chain33ETH.getTokenSymbol())
            complete(send)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun doEthUnlock() {
        try {
            val burnParam = BurnParam()
            burnParam.setChainID(chain33ETH.getChainID())
            burnParam.setExecer(chain33ETH.getExecer())
            burnParam.setBridgeBankContractAddr(chain33ETH.getBridgeBankContractAddr())
            burnParam.setAmount(chain33ETH.getAmount())
            burnParam.setEthReceiverAddr(localEth!!.address)
            burnParam.setDecimal(chain33ETH.getDecimal().toLong())
            burnParam.setTokenAddr(chain33ETH.getContractTokenAddr())
            val create: String = Walletapi.ethUnlock(burnParam)
            val tx = GWithoutTx()
            tx.setFeepriv(withHoldPriv)
            tx.setTxpriv(Coin.webPriv)
            tx.setNoneExecer("user.p.para.none")
            tx.setRawTx(create)
            tx.setFee(signGroupFee)
            val resp: GsendTxResp = Walletapi.coinsWithoutTxGroup(tx)
            val sendTx = WalletSendTx()
            sendTx.setCointype(chain33ETH.getCointype())
            sendTx.setUtil(GoUtils.getUtil())
            sendTx.setTokenSymbol("para.token")
            sendTx.setSignedTx(resp.getSignedTx())
            val send: String = Walletapi.byteTostring(Walletapi.sendRawTransaction(sendTx))
            Log.v("zx", send)
            complete(send)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun doEvmToExchange() {
        try {
            val tokenInfo = CoinTokenInfo()
            tokenInfo.setCoinToken(if (TextUtils.isEmpty(chain33ETH.getTokenSymbol())) chain33ETH.getCointype() else chain33ETH.getTokenSymbol())
            tokenInfo.setCoinTokenContractAddr(chain33ETH.getCoinTokenContractAddr())
            tokenInfo.setDecimal(chain33ETH.getDecimal().toLong())
            tokenInfo.setXgoOracleAddr(chain33ETH.getXgoOracleAddr())
            tokenInfo.setXgoBridgeBankContractAddr(chain33ETH.getXgoBridgeBankContractAddr())
            val execers = Execers()
            execers.setEvmExecer(EvmExecer)
            execers.setExchangeExecer(ExchangeExecer)
            execers.setXgoExecer(XgoExecer)
            execers.setNoneExecer(noneExecer)
            val req = EvmLockToExchangeReq()
            req.setToAddr(localBty!!.address)
            req.setAmount(chain33ETH.getAmount())
            req.setNote("evm->xgo->exchange")
            req.setChainID(chain33ETH.getChainID())
            req.setCoinTokenInfo(tokenInfo)
            req.setExecers(execers)
            val xgo = Xgo()
            val result: String = xgo.createEvmToExchangeTx(req)
            val gWithoutTx = GWithoutTx()
            gWithoutTx.setFeepriv(withHoldPriv)
            gWithoutTx.setTxpriv(Coin.webPriv)
            gWithoutTx.setNoneExecer(noneExecer)
            gWithoutTx.setRawTx(result)
            gWithoutTx.setFee(signGroupFee)
            val resp: GsendTxResp = Walletapi.coinsWithoutTxGroup(gWithoutTx)
            val bytes: ByteArray = xgo.transportSendSignedTx("DEX2",
                "",
                resp.getSignedTx(),
                "http://172.16.100.116:8083")
            val send: String = Walletapi.byteTostring(bytes)
            Log.v("evmToExchange", send)
            complete(send)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun doExchangeToEvm() {
        try {
            val tokenInfo = CoinTokenInfo()
            tokenInfo.setCoinToken(if (TextUtils.isEmpty(chain33ETH.getTokenSymbol())) chain33ETH.getCointype() else chain33ETH.getTokenSymbol())
            tokenInfo.setCoinTokenContractAddr(chain33ETH.getCoinTokenContractAddr())
            tokenInfo.setDecimal(chain33ETH.getDecimal().toLong())
            tokenInfo.setXgoOracleAddr(chain33ETH.getXgoOracleAddr())
            tokenInfo.setXgoBridgeBankContractAddr(chain33ETH.getXgoBridgeBankContractAddr())
            val execers = Execers()
            execers.setEvmExecer(EvmExecer)
            execers.setExchangeExecer(ExchangeExecer)
            execers.setXgoExecer(XgoExecer)
            execers.setNoneExecer(noneExecer)
            val req = ExchangeWithdrawToEvmReq()
            req.setFrom(localBty!!.address)
            req.setTo(localBty!!.address)
            req.setAmount(chain33ETH.getAmount())
            req.setNote("evmxgo withdraw")
            req.setChainID(chain33ETH.getChainID())
            req.setCoinTokenInfo(tokenInfo)
            req.setExecers(execers)
            val xgo = Xgo()
            val result: String = xgo.createExchangeToEvmTx(req)
            val gWithoutTx = GWithoutTx()
            gWithoutTx.setFeepriv(withHoldPriv)
            gWithoutTx.setTxpriv(Coin.webPriv)
            gWithoutTx.setNoneExecer(noneExecer)
            gWithoutTx.setRawTx(result)
            gWithoutTx.setFee(signGroupFee)
            val resp: GsendTxResp = Walletapi.coinsWithoutTxGroup(gWithoutTx)
            val commonParam = CommonParam()
            commonParam.setChainID(0)
            commonParam.setExecer(EvmExecer)
            val withdrawParam = WithdrawParam()
            withdrawParam.setFrom(localBty!!.address)
            withdrawParam.setTo(localBty!!.address)
            withdrawParam.setSignedTxs(resp.getSignedTx())
            withdrawParam.setCoinTokenInfo(tokenInfo)
            withdrawParam.setCommonParam(commonParam)
            val bytes: ByteArray = xgo.transportSendExchangeToEvmTx("DEX2",
                "",
                withdrawParam,
                "http://172.16.100.116:8083")
            val send: String = Walletapi.byteTostring(bytes)
            complete(send)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun showCrossChainDialog(coin: Coin?, type: Int, handler: CompletionHandler<String>) {


        val view = initDialog(R.layout.dialog_dapp)

        val seekBar: SeekBar = view.seekbar_money
        val tvFee: TextView = view.tv_fee
        val tvFeeCoinName: TextView = view.tv_fee_coin_name
        val llMiner: LinearLayout = view.ll_out_miner
        tvFeeCoinName.setText(coin!!.chain)
        llMiner.setVisibility(if (ethFee) View.VISIBLE else View.GONE)
        //只有扣除ETH才需要去获取推荐手续费
        if (ethFee) {
            ethFee = false
            var chain = coin.chain
            if (Walletapi.TypeETHString != coin.name && Walletapi.TypeETHString == coin.chain) {
                chain = "ETHTOKEN"
            }
            outViewModel.getMiner.observe(this, Observer {
                if(it.isSucceed()) {
                    it.data().let {
                        handleFee(it!!, seekBar, tvFee)
                    }
                }
            })
            outViewModel.getMiner(chain)
            val balanceStr: String =
                GoUtils.getbalance(Walletapi.TypeETHString, localEth!!.address, "")
            if (!TextUtils.isEmpty(balanceStr)) {
                val gson = Gson()
                val balanceResponse: BalanceResponse? =
                    gson.fromJson<BalanceResponse>(balanceStr, BalanceResponse::class.java)
                if (balanceResponse != null) {
                    ethBalance = balanceResponse.result?.balance?.toDouble()!!
                }
            }
        }

        view.btn_ok.setOnClickListener {
            if (type == ETH_LOCK) {
                if (chain33ETH.getAmount() + crossFee > ethBalance) {
                    Toast.makeText(this@BaseWebActivity, "矿工费不足", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val password: String = view.et_input.text.toString()
            if (password.isEmpty()) {
                Toast.makeText(this@BaseWebActivity, getString(R.string.my_wallet_detail_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                showLoading(false)
                var result = false
                withContext(Dispatchers.IO) {
                    result = GoUtils.checkPasswd(password, coin.getpWallet().password)
                }
                if (result) {
                    dismiss()
                    val mnem: String = GoUtils.seedDecKey(GoUtils.encPasswd(password)!!,
                        coin.getpWallet().mnem)
                    Coin.webPriv = coin.getPrivkey(coin.chain, mnem)
                    if (type == MAIN_TO_EXCHANGE) {
                        doMainToExchange()
                    } else if (type == EXCHANGE_TO_MAIN) {
                        doExchangeToMain()
                    } else if (type == ETH_LOCK) {
                        doEthLock()
                    } else if (type == ETH_UNLOCK) {
                        doEthUnlock()
                    } else if (type == EVM_TO_EXCHANGE) {
                        doEvmToExchange()
                    } else if (type == EXCHANGE_TO_EVM) {
                        doExchangeToEvm()
                    }
                } else {
                    Toast.makeText(this@BaseWebActivity, "密码输入错误", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }

    private var crossFee = 0.0
    fun handleFee(miner: Miner, seekbarMoney: SeekBar, tvFee: TextView) {
        val min: String = miner.low
        val max: String = miner.high
        val average: String = miner.average
        val minLength = DoubleUtils.dotLength(min)
        val averageLength = DoubleUtils.dotLength(average)
        val blength = if (minLength > averageLength) minLength else averageLength
        val length = if (blength > 8) 8 else blength
        val minInt = DoubleUtils.doubleToInt(min, length)
        val maxInt = DoubleUtils.doubleToInt(max, length)
        seekbarMoney.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                crossFee = DoubleUtils.intToDouble(progress + minInt, length)
                tvFee.setText(DecimalUtils.subZero(DecimalUtils.formatDouble(crossFee)))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekbarMoney.setMax(maxInt)
        //初始进度（推荐款工费）
        seekbarMoney.setProgress(maxInt / 2)
    }
    lateinit var cHandle: CompletionHandler<String>
    private fun complete(send: String) {
        if (handleType == 1) {
            cHandle = paraHandle
        } else if (handleType == 2) {
            cHandle = chain33EthHandle
        }
        Log.v("跨链结果：", send)
        val stringResult: StringResult = parseResult(send)
        if (!TextUtils.isEmpty(stringResult.error)) {
            completeError(cHandle, stringResult.error!!)
        } else {
            val map = mapOf("txid" to stringResult.result)
            cHandle.complete(Gson().toJson(map))
        }
    }

    private fun sendGroup(createTx: String, noneExecer: String?): String? {
        try {
            val withoutTx = GWithoutTx()
            withoutTx.setFeepriv(withHoldPriv)
            withoutTx.setTxpriv(Coin.webPriv)
            withoutTx.setNoneExecer(noneExecer)
            withoutTx.setRawTx(createTx)
            withoutTx.setFee(signGroupFee)
            Log.v("exchange", "GWithoutFee " + withoutTx.getFee())
            val gsendTxResp: GsendTxResp = Walletapi.coinsWithoutTxGroup(withoutTx)
            return gsendTxResp.getSignedTx()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseResult(json: String): StringResult {
        return Gson().fromJson<StringResult>(json, StringResult::class.java)
    }

    private fun parseCreateResult(json: String): String {
        if (TextUtils.isEmpty(json)) {
            return json
        }
        val stringResult: StringResult =
            JsonUtils.toObject<StringResult>(json, StringResult::class.java)
        return stringResult.result!!
    }

    private fun completeError(handler: CompletionHandler<String>, str: String) {
        val map = mapOf("error" to str)
        handler!!.complete(Gson().toJson(map))
    }


}