package com.fzm.walletdemo.wcv2

import android.content.DialogInterface
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.ext.extractHost
import com.fzm.wallet.sdk.ext.toPlainStr
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.databinding.FragmentSessionRequestBinding
import com.fzm.walletmodule.R
import com.github.salomonbrys.kotson.fromJson
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import walletapi.Walletapi
import kotlin.math.pow

class SessionRequestFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSessionRequestBinding
    private var args: SessionRequestData? = null

    private val loading by lazy {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(requireContext()).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logDebug("欢迎onCreateView")
        binding = FragmentSessionRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //根据chainid  来判断是哪个链，比如56 就是BSC
        logDebug("欢迎来到SessionRequestFragment")
        arguments?.let {
            args = it.getParcelable("SessionRequestData")
        }

        args?.let { sessionRequestData ->
            showPayMsg(sessionRequestData)
        }


    }


    private var gas:Long = 0
    private var gasPrice:Long = 0
    private var value:Long = 0
    private var nonce:Long = 0
    private lateinit var web3j:Web3j
    private fun showPayMsg(sessionRequestData: SessionRequestData) {
        val params = gson.fromJson<List<WCEthereumTransaction>>(sessionRequestData.params)
        val param = params[0]
        gas = param.gas.substringAfter("0x").toLong(16)
        value = param.value.substringAfter("0x").toLong(16)
        val va18 = 10.0.pow(18.0)
        val pValue = "${value / va18}".toPlainStr(8)

        val chainName = GoWallet.getChainName(sessionRequestData.chain)
        val web3Url = GoWallet.getWeb3Url(sessionRequestData.chain)

        binding.tvDappName.text = sessionRequestData.appName
        binding.tvDappUrl.text = sessionRequestData.appUri?.extractHost()
        Glide.with(this)
            .load(sessionRequestData.appIcon)
            .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
            .into(binding.ivDappIcon)
        binding.tvValue.text = "$pValue $chainName"
        binding.tvPayMsg.text = "$chainName 转账"
        binding.tvOutAddress.text = param.from
        binding.tvInAddress.text = param.to


        lifecycleScope.launch(Dispatchers.IO) {
            web3j = Web3j.build(HttpService(web3Url))
            val gasPriceResult = web3j.ethGasPrice().send()
            val nonceResult = web3j.ethGetTransactionCount(
                param.from,
                DefaultBlockParameterName.LATEST
            ).send()
            withContext(Dispatchers.Main) {
                 gasPrice = gasPriceResult.gasPrice.toLong()
                 nonce = nonceResult.transactionCount.toLong()
                logDebug("gasprice  = $gasPrice , gas = $gas")
                val dGas = (gasPrice * gas) / va18
                val newGas = "$dGas".toPlainStr(8)
                binding.tvWcFee.text = "$newGas $chainName"
            }

        }



        binding.tvCancel.setOnClickListener {
            val sessionRequestResponse = Wallet.Params.SessionRequestResponse(
                sessionRequestData.topic,
                Wallet.Model.JsonRpcResponse.JsonRpcError(
                    sessionRequestData.requestId,
                    500,
                    "User rejected"
                )
            )
            Web3Wallet.respondSessionRequest(sessionRequestResponse) { error ->
                logDebug("respondSessionRequest error  = $error")
            }
        }

        binding.btnNext.setOnClickListener {
            showPWD(chainName,param, sessionRequestData)
        }

    }

    private val gson = GsonBuilder()
        .serializeNulls()
        .create()


    private fun showPWD(
        chainName:String,
        param: WCEthereumTransaction,
        sessionRequestData: SessionRequestData,
    ) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pwd, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
        val bindingDialog = DialogPwdBinding.bind(view)
        bindingDialog.ivClose.setOnClickListener {
            dialog.dismiss()
        }
        bindingDialog.btnOk.setOnClickListener {
            val password = bindingDialog.etInput.text.toString()
            if (password.isEmpty()) {
                toast("请输入密码")
                return@setOnClickListener
            }
            loading.show()
            lifecycleScope.launch(Dispatchers.IO) {
                val wallet = LitePal.find<PWallet>(MyWallet.getId())
                wallet?.let { w ->
                    val check = GoWallet.checkPasswd(password, w.password)
                    if (!check) {
                        withContext(Dispatchers.Main) {
                            toast("密码错误")
                            loading.dismiss()
                        }
                    } else {
                        val bPassword = GoWallet.encPasswd(password)!!
                        val mnem: String = GoWallet.decMenm(bPassword, w.mnem)
                        val privKey = GoWallet.getPrikey(chainName, mnem)
                        createAndSign(chainName,param, privKey, sessionRequestData)
                    }

                }

            }
        }
    }

    private suspend fun createAndSign(
        chainName:String,
        param: WCEthereumTransaction,
        privKey: String,
        sessionRequestData: SessionRequestData,
    ) {

        val input = param.data.substringAfter("0x")
        val input64 = Base64.encodeToString(input.toByteArray(), Base64.DEFAULT)

        val createTran = CreateTran(
            param.from,
            gas,
            gasPrice,
            input64,
            nonce,
            param.to,
            value
        )
        val createJson = gson.toJson(createTran)
        logDebug("构造数据 == $createJson")

        val bCreate = Walletapi.stringTobyte(createJson)
        val signed =
            GoWallet.signTran(chainName, bCreate, privKey, 2)
        logDebug("签名数据： $signed")

        //val  send = web3j.ethSendRawTransaction(signed).send()
        //val sendHash = send.transactionHash
        val sendHash = GoWallet.sendTran(chainName, signed!!, "")
        logDebug("发送数据： $sendHash")
        withContext(Dispatchers.Main){
            loading.dismiss()
            toast("发送成功!")
        }

        val sessionRequestResponse = Wallet.Params.SessionRequestResponse(
            sessionRequestData.topic,
            Wallet.Model.JsonRpcResponse.JsonRpcResult(
                sessionRequestData.requestId,
                sendHash!!
            )
        )

        Web3Wallet.respondSessionRequest(sessionRequestResponse) { error ->
            logDebug("respondSessionRequest error  = $error")
        }


    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        //viewModel.reject(args.data.topic, args.data.requestId)
    }
}