package com.fzm.walletdemo.wcv2

import android.app.Dialog
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityWconnectBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import kotlin.math.pow

@Route(path = RouterPath.APP_WCONNECT)
class WConnectActivity : BaseActivity() {

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

    @JvmField
    @Autowired(name = RouterPath.PARAM_WC_URL)
    var wcUrl: String? = null
    private var settledTopic: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        logDebug("欢迎。。")
        ARouter.getInstance().inject(this)
        initObserver()
    }


    override fun initObserver() {
        super.initObserver()
        InitWCV2.signModel.observe(this@WConnectActivity, Observer { walletModel ->
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
                    //web主动断开，监听到后关闭当前activity即可
                    finish()
                }

                else -> {}
            }
        })
        initWCV2()
    }


    //---------------------------------WCV2-----------------------------------

    private fun initWCV2() {
        //projectId 每天都连接限制，到达限制次数后就超时
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
                settledTopic = settleSessionResponse.session.topic
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
            binding.incProposaled.tvWalletState.text = "${sessionProposal.name} ${getString(R.string.wc_coned)}"
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
        settledTopic?.let { topic ->
            val sessionDisconnect = Wallet.Params.SessionDisconnect(topic)
            Web3Wallet.disconnectSession(sessionDisconnect) { error ->
                logDebug("disconnectSession = $error")
            }
        }

    }

    private val gson = GsonBuilder().serializeNulls().create()

    //------------------------------------------请求-----------------------------------------------
    private var requestTopic = ""
    private var requestId = 0L
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
                showUI(incRequest = false, incProposaled = true)
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
            binding.incRequest.tvValue.text = "$pValue $chainName"
            binding.incRequest.tvPayMsg.text = "$chainName ${getString(R.string.home_transfer)}"
            binding.incRequest.tvOutAddress.text = param.from
            binding.incRequest.tvInAddress.text = param.to

            //handle fee
            val gas = param.gas.substringAfter("0x").toLong(16)

            //handle count(nonce) and gasPrice
            var gasPrice = 0L
            var count = 0L
            if (chainName == "BTY") {
                lifecycleScope.launch(Dispatchers.IO) {
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
                }
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
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
                }
            }


            binding.incRequest.btnNext.setOnClickListener {

                val input = param.data.substringAfter("0x")
                val input64 = Base64.encodeToString(Walletapi.hexTobyte(input), Base64.DEFAULT)
                val createTran = CreateTran(
                    param.from, gas.toBigInteger(), gasPrice.toBigInteger(), input64, count, param.to, value.toBigInteger()
                )


                showPWD(createTran, chainName)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun showGasUI(gasPrice: Long, gas: Long, chainName: String?) {
        try {
            val va9 = 10.0.pow(9.0)
            val va18 = 10.0.pow(18.0)
            val newGasPirce = "${gasPrice / va9}".toPlainStr(2)

            val dGas = (gasPrice * gas) / va18
            val newGas = "$dGas".toPlainStr(6)

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
                                            if (send.isSucceed()) {
                                                send.data()?.let { sendHash ->
                                                    responseWC(sendHash)
                                                    dis()
                                                }
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
        toast(getString(R.string.send_suc_str))
        showUI(incRequest = false, incProposaled = true)
    }

    private fun responseWC(sendHash: String) {
        val sessionRequestResponse = Wallet.Params.SessionRequestResponse(
            requestTopic, Wallet.Model.JsonRpcResponse.JsonRpcResult(
                requestId, sendHash
            )
        )

        Web3Wallet.respondSessionRequest(sessionRequestResponse) { error ->
            logDebug("respondSessionRequest error  = $error")
        }
    }

    override fun onDestroy() {
        disConnect()
        super.onDestroy()
    }

}
