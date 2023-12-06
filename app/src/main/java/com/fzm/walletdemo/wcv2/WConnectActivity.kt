package com.fzm.walletdemo.wcv2

import android.app.Dialog
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.FEE_CUSTOM_POSITION
import com.fzm.wallet.sdk.base.LIVE_KEY_FEE
import com.fzm.wallet.sdk.base.LIVE_WC_MODEL
import com.fzm.wallet.sdk.base.LIVE_WC_STATUS
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.ext.extractHost
import com.fzm.wallet.sdk.ext.toPlainStr
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.GoWallet.Companion.CHAIN_ID_MAPS
import com.fzm.wallet.sdk.utils.GoWallet.Companion.CHAIN_MAPS_LL
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityWconnectBinding
import com.fzm.walletmodule.bean.DGear
import com.fzm.walletmodule.ui.base.BaseActivity
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import timber.log.Timber
import walletapi.Walletapi
import java.math.BigInteger
import kotlin.math.pow

@Route(path = RouterPath.APP_WCONNECT)
class WConnectActivity : BaseActivity() {

    private var chainId: Long = GoWallet.CHAIN_ID_BNB_L
    private var feePosition = 2
    private lateinit var cGas: BigInteger

    //原始gas
    private lateinit var origGas: BigInteger
    private lateinit var cGasPrice: BigInteger
    private var chainName: String? = ""

    private val binding by lazy { ActivityWconnectBinding.inflate(layoutInflater) }

    //private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private val walletRepository: WalletRepository by inject(walletQualifier)

    private val loading by lazy {
        val view =
            LayoutInflater.from(this).inflate(com.fzm.walletmodule.R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    //from = 1 is main, from = 2 is InitWCV2
    @JvmField
    @Autowired(name = RouterPath.PARAM_FROM)
    var from: Int = 0

    @JvmField
    @Autowired(name = RouterPath.PARAM_WC_URL)
    var wcUrl: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
    }

    override fun initObserver() {
        super.initObserver()
        if (from == 1) {
            WCParam.sessionProposal?.let { sp ->
                showProposaled(sp, WCParam.address, WCParam.chooseChain)
            }

        } else if (from == 2) {
            WCParam.sessionRequest?.let { sr ->
                showRequest(sr)
            }
        }
        LiveEventBus.get<Wallet.Model?>(LIVE_WC_MODEL).observe(this, Observer { walletModel ->
            when (walletModel) {
                is Wallet.Model.SessionProposal -> {
                    logDebug("activity - SessionProposal ")
                    gotoSessionProposal(walletModel)
                }

                is Wallet.Model.SessionRequest -> {
                    logDebug("activity - SessionRequest ")
                    gotoSessionRequest(walletModel)
                }

                is Wallet.Model.SettledSessionResponse -> {
                    logDebug("activity - SettledSessionResponse ")
                    gotoSettleResponse(walletModel)
                }

                is Wallet.Model.SessionDelete -> {
                   //暂不处理

                }

                else -> {}
            }
        })
        initWCV2()


        LiveEventBus.get<DGear>(LIVE_KEY_FEE).observe(this, Observer { dGear ->
            feePosition = dGear.position
            cGas = dGear.gas
            cGasPrice = dGear.gasPrice
            showGasUI(cGasPrice.toLong(), cGas.toLong(), chainName)
            setLevel(binding.incRequest.tvLevel)
        })
    }


    //---------------------------------WCV2-----------------------------------

    private fun initWCV2() {
        //projectId 每天都连接限制，到达限制次数后就超时
        //从InitWCV2跳转到这里，传null即不用二次连接
        InitWCV2
        wcUrl?.let { wurl ->
            //连接
            pair(wurl)

        }

    }

    private fun pair(pairingUri: String) {
        val pairingParams = Core.Params.Pair(pairingUri)
        CoreClient.Pairing.pair(pairingParams) { error ->
            Timber.e("Error: ${error.throwable.stackTraceToString()}")
        }
    }


    private fun gotoSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
        showProposal(sessionProposal)
    }


    private fun gotoSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        showRequest(sessionRequest)
    }


    private fun gotoSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        when (settleSessionResponse) {
            is Wallet.Model.SettledSessionResponse.Result -> {
                //断开连接需要这个topic
                WCParam.settledTopic = settleSessionResponse.session.topic
                //showProposaled(settleSessionResponse)
            }

            is Wallet.Model.SettledSessionResponse.Error -> Wallet.Model.SettledSessionResponse.Error(
                "SettleResponse 连接失败"
            )
        }


    }


    //------------------------------------------授权-------------------------------------------------
    private fun showProposal(sessionProposal: Wallet.Model.SessionProposal) {
        try {
            showUI(incProposal = true)
            var chain: String? = ""
            var namespaces: Map<String, Wallet.Model.Namespace.Session>? = null
            var address: String? = ""
            sessionProposal.requiredNamespaces["eip155"]?.let {
                chain = it.chains[0]
            }
            val chooseChain = CHAIN_ID_MAPS[chain]
            chainId = CHAIN_MAPS_LL[chain]!!
            chooseChain?.let { choose ->
                address = GoWallet.getChain(if (choose == "BTY") "ETH" else choose)?.address
                namespaces = configNamespaces(chain, address)
            }



            binding.incProposal.tvDappName.text = sessionProposal.name
            binding.incProposal.tvDappUrl.text = sessionProposal.url
            binding.incProposal.btnRefuse.setOnClickListener {
                //拒绝的原因，随便填写
                val rejectionReason = "Reject Session"
                val reject =
                    Wallet.Params.SessionReject(sessionProposal.proposerPublicKey, rejectionReason)
                Web3Wallet.rejectSession(reject) { error ->
                    logDebug("$error")
                }
                finish()
            }
            binding.incProposal.btnGrant.setOnClickListener {
                namespaces?.let { spaces ->
                    val approve =
                        Wallet.Params.SessionApprove(sessionProposal.proposerPublicKey, spaces)
                    Web3Wallet.approveSession(approve) { error ->
                        logDebug("$error")
                    }
                }
                WCParam.sessionProposal = sessionProposal
                WCParam.address = address
                WCParam.chooseChain = chooseChain
                showProposaled(sessionProposal, address, chooseChain)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configNamespaces(
        chain: String?, address: String?
    ): Map<String, Wallet.Model.Namespace.Session> {
        return mapOf(
            "eip155" to Wallet.Model.Namespace.Session(
                listOf("$chain:$address"),
                listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                listOf("chainChanged", "accountsChanged"),
                null
            )
        )
    }

    private fun showUI(
        incLoading: Boolean = false,
        incProposal: Boolean = false,
        incProposaled: Boolean = false,
        incRequest: Boolean = false
    ) {
        logDebug("incLoading = $incLoading,incProposal = $incProposal,incProposaled = $incProposaled,incRequest = $incRequest")
        binding.incLoading.root.visibility = if (incLoading) View.VISIBLE else View.GONE
        binding.incProposal.root.visibility = if (incProposal) View.VISIBLE else View.GONE
        binding.incProposaled.root.visibility = if (incProposaled) View.VISIBLE else View.GONE
        binding.incRequest.root.visibility = if (incRequest) View.VISIBLE else View.GONE
    }


    //------------------------------------------已授权-----------------------------------------------

    /*  private fun showProposaled(settleSessionResponse: Wallet.Model.SettledSessionResponse.Result) {
          try {
              showUI(incProposaled = true)
              var account = ""
              settleSessionResponse.session.namespaces["eip155"]?.let {
                  account = it.accounts[0]
              }
              val acccountSplit = account.split(":")
              val chooseChain = CHAIN_ID_MAPS["${acccountSplit[0]}:${acccountSplit[1]}"]

              binding.incProposaled.tvWalletState.text =
                  "${settleSessionResponse.session.metaData?.name} 已经与下列钱包建立连接"
              binding.incProposaled.tvDappUrl.text = settleSessionResponse.session.metaData?.url
              binding.incProposaled.tvAddress.text = acccountSplit[2]
              binding.incProposaled.tvChain.text = chooseChain
              binding.incProposaled.btnDis.setOnClickListener {
                  disConnect()
                  finish()
              }
          } catch (e: Exception) {
              e.printStackTrace()
          }
      }*/
    private fun showProposaled(
        sessionProposal: Wallet.Model.SessionProposal,
        address: String?,
        chooseChain: String?
    ) {
        try {
            showUI(incProposaled = true)
            binding.incProposaled.tvWalletState.text =
                "${sessionProposal.name} ${getString(R.string.wc_coned)}"
            binding.incProposaled.tvDappUrl.text = sessionProposal.url
            binding.incProposaled.tvAddress.text = address
            binding.incProposaled.tvChain.text = chooseChain
            binding.incProposaled.btnDis.setOnClickListener {
                disConnect()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disConnect() {
        WCParam.settledTopic?.let { topic ->
            val sessionDisconnect = Wallet.Params.SessionDisconnect(topic)
            Web3Wallet.disconnectSession(sessionDisconnect) { error ->
                logDebug("disconnectSession = $error")
            }
            postToMain(false)

        }

    }

    private fun postToMain(state: Boolean) {
        LiveEventBus.get<Boolean>(LIVE_WC_STATUS).post(state)
    }

    private val gson = GsonBuilder().serializeNulls().create()

    //------------------------------------------请求-----------------------------------------------
    private var requestTopic = ""
    private var requestId = 0L
    private var coinBalance: Double = 0.0;
    private fun showRequest(sessionRequest: Wallet.Model.SessionRequest) {
        try {
            requestTopic = sessionRequest.topic
            requestId = sessionRequest.request.id
            showUI(incRequest = true)

            val params = gson.fromJson<List<WCEthereumTransaction>>(sessionRequest.request.params)
            val param = params[0]
            val va18 = 10.0.pow(18.0)
            binding.incRequest.tvCancel.setOnClickListener {
                val sessionRequestResponse = Wallet.Params.SessionRequestResponse(
                    requestTopic,
                    Wallet.Model.JsonRpcResponse.JsonRpcError(
                        requestId,
                        500,
                        "User rejected"
                    )
                )
                Web3Wallet.respondSessionRequest(sessionRequestResponse) { error ->
                    logDebug("respondSessionRequest error  = $error")
                }
                if (from == 2) {
                    finish()
                } else {
                    showUI(incRequest = false, incProposaled = true)
                }
            }
            binding.incRequest.llSetFee.setOnClickListener {
                gotoSetFee()
            }
            binding.incRequest.tvWcFee.setOnClickListener {
                gotoSetFee()
            }
            binding.incRequest.tvDappName.text = sessionRequest.peerMetaData?.name
            binding.incRequest.tvDappUrl.text = sessionRequest.peerMetaData?.url?.extractHost()
            Glide.with(this).load(sessionRequest.peerMetaData?.icons?.firstOrNull())
                .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
                .into(binding.incRequest.ivDappIcon)

            //handle value
            var value = 0L
            param.value?.let {
                value = it.substringAfter("0x").toLong(16)
            }
            val pValue = "${value / va18}".toPlainStr(8)
            val chainName = CHAIN_ID_MAPS[sessionRequest.chainId]
            lifecycleScope.launch(Dispatchers.IO) {
                val balance = GoWallet.getWCBalance(param.from, chainName!!, "")
                withContext(Dispatchers.Main) {
                    coinBalance = balance.toDouble()
                }
            }

            binding.incRequest.tvValue.text = "$pValue $chainName"
            binding.incRequest.tvPayMsg.text = "$chainName ${getString(R.string.home_transfer)}"
            binding.incRequest.tvOutAddress.text = param.from
            binding.incRequest.tvInAddress.text = param.to

            //handle fee
            val gas = param.gas.substringAfter("0x").toLong(16)
            setLevel(binding.incRequest.tvLevel)
            origGas = gas.toBigInteger()
            cGas = gas.toBigInteger()

            //handle count(nonce) and gasPrice
            var gasPrice = 0L
            var count = 0L
            if (chainName == "BTY") {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val gasPriceResult = walletRepository.getGasPrice()
                        val countResult = walletRepository.getTransactionCount(param.from)
                        withContext(Dispatchers.Main) {
                            if (gasPriceResult.isSucceed()) {
                                gasPriceResult.data()?.let {
                                    gasPrice = it.substringAfter("0x").toLong(16)
                                }
                            }
                            if (countResult.isSucceed()) {
                                countResult.data()?.let {
                                    count = it.substringAfter("0x").toLong(16)
                                }
                            }

                            showGasUI(gasPrice, gas, chainName)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val web3Url = GoWallet.getWeb3Url(sessionRequest.chainId)
                        val web3j = Web3j.build(HttpService(web3Url))
                        val gasPriceResult = web3j.ethGasPrice().send()
                        val countResult = web3j.ethGetTransactionCount(
                            param.from, DefaultBlockParameterName.LATEST
                        ).send()
                        withContext(Dispatchers.Main) {
                            gasPrice = gasPriceResult.gasPrice.toLong()
                            count = countResult.transactionCount.toLong()
                            showGasUI(gasPrice, gas, chainName)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }


            binding.incRequest.btnNext.setOnClickListener {
                if (::cGas.isInitialized && ::cGasPrice.isInitialized) {
                    if (coinBalance < dGas) {
                        toast(getString(R.string.fee_not_enough))
                        return@setOnClickListener
                    }
                    val input = param.data.substringAfter("0x")
                    val input64 = Base64.encodeToString(Walletapi.hexTobyte(input), Base64.DEFAULT)
                    val createTran = CreateTran(
                        param.from, cGas, cGasPrice, input64, count, param.to, value.toBigInteger()
                    )

                    showPWD(createTran, chainName)
                }

            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun gotoSetFee() {
        if (::cGas.isInitialized && ::cGasPrice.isInitialized) {
            ARouter.getInstance().build(RouterPath.APP_SETFEE)
                .withInt(RouterPath.PARAM_FEE_POSITION, feePosition)
                .withLong(RouterPath.PARAM_CHAIN_ID, chainId)
                .withLong(RouterPath.PARAM_ORIG_GAS, origGas.toLong())
                .withLong(RouterPath.PARAM_GAS, cGas.toLong())
                .withLong(RouterPath.PARAM_GAS_PRICE, cGasPrice.toLong())
                .navigation()
        }

    }

    private var dGas: Double = 0.0;
    private fun showGasUI(gasPrice: Long, gas: Long, chainName: String?) {
        try {
            cGasPrice = gasPrice.toBigInteger()
            val va9 = 10.0.pow(9.0)
            val va18 = 10.0.pow(18.0)
            val newGasPirce = "${gasPrice / va9}".toPlainStr(2)

            dGas = (gasPrice * gas) / va18
            val newGas = "$dGas".toPlainStr(6)
            binding.incRequest.tvFee.text = "$newGas $chainName"
            binding.incRequest.tvWcFee.text =
                "$newGas $chainName = Gas($gas)*GasPrice($newGasPirce GWEI)"
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private var pwdDialog: Dialog? = null
    private fun showPWD(createTran: CreateTran, chainName: String?) {
        try {
            val view =
                LayoutInflater.from(this).inflate(com.fzm.walletmodule.R.layout.dialog_pwd, null)
            pwdDialog = AlertDialog.Builder(this).setView(view).create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }
            val bindingDialog = DialogPwdBinding.bind(view)
            bindingDialog.ivClose.setOnClickListener {
                pwdDialog?.dismiss()
            }
            bindingDialog.btnOk.setOnClickListener {

                val password = bindingDialog.etInput.text.toString()
                if (password.isEmpty()) {
                    toast(getString(R.string.my_wallet_password_tips))
                    return@setOnClickListener
                }
                loading.show()
                lifecycleScope.launch(Dispatchers.IO) {
                    val wallet = LitePal.find<PWallet>(MyWallet.getId())
                    wallet?.let { w ->
                        val check = GoWallet.checkPasswd(password, w.password)
                        if (!check) {
                            withContext(Dispatchers.Main) {
                                toast(getString(R.string.pwd_fail_str))
                                loading.dismiss()
                            }
                        } else {
                            val bPassword = GoWallet.encPasswd(password)!!
                            val mnem: String = GoWallet.decMenm(bPassword, w.mnem)
                            chainName?.let { name ->
                                val privKey =
                                    GoWallet.getPrikey(if (name == "BTY") "ETH" else name, mnem)
                                val createJson = gson.toJson(createTran)
                                val bCreate = Walletapi.stringTobyte(createJson)

                                val signed = GoWallet.signTran(
                                    if (name == "BTY") "BTYETH" else name, bCreate, privKey, 2
                                )
                                signed?.let {
                                    if (name == "BTY") {
                                        val send = walletRepository.sendRawTransaction(signed)
                                        withContext(Dispatchers.Main) {
                                            dis()
                                            if (send.isSucceed()) {
                                                send.data()?.let { sendHash ->
                                                    responseWC(sendHash)
                                                }
                                            } else {
                                                longToast(send.error())
                                            }

                                        }
                                    } else {
                                        val send = GoWallet.sendTran(chainName, signed, "")
                                        val sendHash = JSONObject(send!!).getString("result")
                                        withContext(Dispatchers.Main) {
                                            responseWC(sendHash)
                                            dis()
                                        }
                                    }
                                }

                            }

                        }

                    }

                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dis() {
        loading.dismiss()
        pwdDialog?.dismiss()
        showUI(incRequest = false, incProposaled = true)
    }

    private fun responseWC(sendHash: String) {
        toast(getString(R.string.send_suc_str))
        val sessionRequestResponse = Wallet.Params.SessionRequestResponse(
            requestTopic, Wallet.Model.JsonRpcResponse.JsonRpcResult(
                requestId, sendHash
            )
        )

        Web3Wallet.respondSessionRequest(sessionRequestResponse) { error ->
            logDebug("respondSessionRequest error  = $error")
        }
    }


    //custom fee
    private fun setLevel(tvLevel: TextView?) {
        val level = when (feePosition) {
            0 -> getString(R.string.high_str)
            1 -> getString(R.string.standard_str)
            2 -> getString(R.string.low_str)
            FEE_CUSTOM_POSITION -> getString(R.string.custom_str)
            else -> getString(R.string.low_str)
        }
        tvLevel?.text = level
    }


}
