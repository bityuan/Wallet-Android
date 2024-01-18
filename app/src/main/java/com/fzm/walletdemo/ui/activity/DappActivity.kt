package com.fzm.walletdemo.ui.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.RouterPath.PARAM_CHAIN_ID
import com.fzm.wallet.sdk.RouterPath.PARAM_FEE_POSITION
import com.fzm.wallet.sdk.RouterPath.PARAM_GAS
import com.fzm.wallet.sdk.RouterPath.PARAM_GAS_PRICE
import com.fzm.wallet.sdk.RouterPath.PARAM_ORIG_GAS
import com.fzm.wallet.sdk.RouterPath.PARAM_ORIG_GAS_PRICE
import com.fzm.wallet.sdk.base.FEE_CUSTOM_POSITION
import com.fzm.wallet.sdk.base.LIVE_KEY_FEE
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.ext.toPlainStr
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.walletdemo.BuildConfig
import com.fzm.walletmodule.bean.DGear
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityDappBinding
import com.fzm.walletdemo.databinding.DialogDappBottomBinding
import com.fzm.walletdemo.databinding.DialogDappWalletsBinding
import com.fzm.walletdemo.ui.JsApi
import com.fzm.walletdemo.ui.JsWCApi
import com.fzm.walletdemo.wcv2.CreateTran
import com.fzm.walletdemo.web3.URLLoadInterface
import com.fzm.walletdemo.web3.Web3ViewClient
import com.fzm.walletdemo.web3.bean.Address
import com.fzm.walletdemo.web3.bean.Web3Call
import com.fzm.walletdemo.web3.bean.Web3Transaction
import com.fzm.walletdemo.web3.listener.JsListener
import com.fzm.walletmodule.ui.widget.configWindow
import com.fzm.walletmodule.utils.ClipboardUtils
import com.google.gson.GsonBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findAll
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import timber.log.Timber
import walletapi.Walletapi
import wendu.dsbridge.DWebView
import java.math.BigInteger
import kotlin.math.pow


@Route(path = RouterPath.APP_DAPP)
class DappActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDappBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_URL)
    var url: String? = null


    private var address = Address.EMPTY
    private var nodeUrl = GoWallet.WEB3_BNB
    private var chainId: Long = GoWallet.CHAIN_ID_BNB_L

    //private var feePosition = 2
    private var feePosition = 0
    private lateinit var cGas: BigInteger

    //原始gas
    private lateinit var origGas: BigInteger
    private lateinit var origGasPirce: BigInteger
    private lateinit var cGasPrice: BigInteger
    private var dGas: Double = 0.0
    private var tvFee: TextView? = null
    private var tvWCFee: TextView? = null
    private var tvLevel: TextView? = null
    private var chainName: String? = ""
    private var currentAddress = ""

    private val walletRepository: WalletRepository by inject(walletQualifier)
    private val loading by lazy {
        val view =
            LayoutInflater.from(this).inflate(com.fzm.walletmodule.R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserve()
        configChainNet()
        chainName = GoWallet.CHAIN_ID_MAPS_L[chainId]
        doBar()
        binding.tvToolbar.text = name ?: getString(R.string.exp_str)
        initWebView()
        url?.let {
            binding.webDapp.loadUrl(it)
        }

    }

    private fun initObserve() {
        LiveEventBus.get<DGear>(LIVE_KEY_FEE).observe(this, Observer { dGear ->
            feePosition = dGear.position
            cGas = dGear.gas
            cGasPrice = dGear.gasPrice
            showGasUI(cGasPrice.toLong(), cGas.toLong(), chainName)
            setLevel(tvLevel)
        })
    }

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

    private fun configChainNet() {
        val chainNet = MMkvUtil.decodeInt(GoWallet.CHAIN_NET)
        when (chainNet) {
            0 -> {
                val addr = GoWallet.getChain("ETH")?.address
                addr?.let {
                    currentAddress = it
                    address = Address(it)
                    nodeUrl = GoWallet.WEB3_BTY
                    chainId = GoWallet.CHAIN_ID_BTY_L
                }
            }

            1 -> {
                val addr = GoWallet.getChain("ETH")?.address
                addr?.let {
                    currentAddress = it
                    address = Address(it)
                    nodeUrl = GoWallet.WEB3_ETH
                    chainId = GoWallet.CHAIN_ID_ETH_L
                }
            }

            2 -> {
                val addr = GoWallet.getChain("BNB")?.address
                addr?.let {
                    currentAddress = it
                    address = Address(it)
                    nodeUrl = GoWallet.WEB3_BNB
                    chainId = GoWallet.CHAIN_ID_BNB_L
                }
            }
        }
    }

    private fun doBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        view.fitsSystemWindows = true
        StatusBarUtil.StatusBarLightMode(this)

        binding.ivMenu.setOnClickListener {
            showBottomDialog()
        }
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.toolbar.title = ""
    }


    override fun onBackPressed() {
        if (binding.webDapp.canGoBack()) {
            binding.webDapp.goBack()
        } else {
            super.onBackPressed()
        }

    }


    private fun initWebView() {
        DWebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        binding.webDapp.addJavascriptObject(JsApi(binding.webDapp, this), null)
        binding.webDapp.addJavascriptInterface(JsWCApi(binding.webDapp, this, jsListener), "alpha")
        binding.webDapp.settings.javaScriptEnabled = true
        binding.webDapp.settings.domStorageEnabled = true
        //解决http图片不显示
        binding.webDapp.settings.blockNetworkImage = false
        val userAgentString = binding.webDapp.settings.userAgentString
        val resultAgent = "$userAgentString;wallet;1.0"
        binding.webDapp.settings.userAgentString = resultAgent
        //https://www.cnblogs.com/ufreedom/p/4229590.html
        //禁用_blank打开新窗口，解决打开外部超链接失效问题
        binding.webDapp.settings.setSupportMultipleWindows(false)
        setupWeb3(chainId, nodeUrl, address)

    }

    private fun setupWeb3(chainId: Long, nodeUrl: String, address: Address) {
        try {
            val web3ViewClient = Web3ViewClient(this)
            binding.webDapp.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressWeb.progress = newProgress
                }

            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                binding.webDapp.webViewClient = WrapWebViewClient(
                    binding.webDapp,
                    binding.progressWeb,
                    web3ViewClient,
                    binding.webDapp.webViewClient
                )
            }
            web3ViewClient.jsInjectorClient.chainId = chainId
            web3ViewClient.jsInjectorClient.rpcUrl = nodeUrl
            web3ViewClient.jsInjectorClient.walletAddress = address
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }


    //---------------------------------------web3-------------------------------------------

    private class WrapWebViewClient(
        val dWebView: DWebView,
        val progressBar: ProgressBar,
        val internalClient: Web3ViewClient,
        val externalClient: WebViewClient?
    ) : WebViewClient() {
        private var loadInterface: URLLoadInterface? = null
        private var loadingError = false
        private var redirect = false


        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            dWebView.clearCache(true)
            if (!redirect) {
                internalClient.let {
                    view.evaluateJavascript(it.getProviderString(view), null)
                    view.evaluateJavascript(it.getInitString(view), null)
                    it.resetInject()
                }

            }
            redirect = false
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            if (!redirect && !loadingError) {
                if (loadInterface != null) {
                    loadInterface!!.onWebpageLoaded(url, view.title)
                }
            } else if (!loadingError && loadInterface != null) {
                loadInterface!!.onWebpageLoadComplete()
            }
            redirect = false
            loadingError = false
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            redirect = true
            return (externalClient!!.shouldOverrideUrlLoading(
                view,
                url
            ) || internalClient.shouldOverrideUrlLoading(view, url))
        }

        override fun onReceivedError(
            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
        ) {
            loadingError = true
            externalClient?.onReceivedError(view, request, error)
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?, request: WebResourceRequest?
        ): Boolean {
            redirect = true
            return (externalClient!!.shouldOverrideUrlLoading(
                view,
                request
            ) || internalClient.shouldOverrideUrlLoading(view, request))
        }
    }


    //-------------------------web3 listener--------------------------------
    private val JS_CALLBACK = "AlphaWallet.executeCallback(%1\$s, null, %2\$s)"
    private val JS_CALLBACK_ON = "AlphaWallet.executeCallback(%1\$s, null, \"%2\$s\")"
    private val JS_CALLBACK_ON_FAILURE = "AlphaWallet.executeCallback(%1\$s, \"%2\$s\", null)"
    private val JS_CANCELLED = "cancelled"
    private val JS_FAIL = "Fail"

    var gasPrice = 0L
    var count = 0L
    private var payDialog: AlertDialog? = null

    private val jsListener = object : JsListener {
        override fun onRequestAccounts(callbackId: Long) {
            try {
                val callback = String.format(
                    JS_CALLBACK, callbackId, "[\"$address\"]"
                )

                binding.webDapp.evaluateJavascript(callback) { message: String? ->
                    Timber.d(message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


        override fun onEthCall(call: Web3Call) {
            try {
                lifecycleScope.launch(Dispatchers.IO) {
                    val httpService = HttpService(nodeUrl)
                    val web3j = Web3j.build(httpService)
                    val tran = Transaction.createFunctionCallTransaction(
                        address.toString(),
                        null,
                        null,
                        call.gasLimit,
                        call.to.toString(),
                        call.value,
                        call.payload
                    )
                    var value: String? = ""
                    try {
                        value = web3j?.ethCall(tran, call.blockParam)?.send()?.value
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            //优化报错提示，去掉自动刷新
                            //longToast("${getString(R.string.basic_error_all)}  $e")
                            toast("${getString(R.string.tp_error)}")
                            dis()
                            //binding.webDapp.reload()
                            e.printStackTrace()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val callback: String =
                            String.format(JS_CALLBACK_ON, call.leafPosition, value)
                        //All WebView methods must be called on the same thread
                        //所以都放在主线程
                        binding.webDapp.evaluateJavascript(callback) { message: String? ->
                            Timber.d(message)
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


        override fun onSignTransaction(transaction: Web3Transaction?) {
            try {
                lifecycleScope.launch(Dispatchers.Main) {
                    val view = LayoutInflater.from(this@DappActivity)
                        .inflate(R.layout.fragment_session_request, null)
                    val ivDappIcon = view.findViewById<ImageView>(R.id.iv_dapp_icon)
                    val tvDappName = view.findViewById<TextView>(R.id.tv_dapp_name)
                    val tvDappUrl = view.findViewById<TextView>(R.id.tv_dapp_url)
                    ivDappIcon.visibility = View.GONE
                    val tvPayMsg = view.findViewById<TextView>(R.id.tv_pay_msg)
                    val tvValue = view.findViewById<TextView>(R.id.tv_value)
                    val tvOutAddress = view.findViewById<TextView>(R.id.tv_out_address)
                    val tvInAddress = view.findViewById<TextView>(R.id.tv_in_address)
                    tvFee = view.findViewById<TextView>(R.id.tv_fee)
                    tvWCFee = view.findViewById<TextView>(R.id.tv_wc_fee)
                    tvLevel = view.findViewById<TextView>(R.id.tv_level)
                    val tvCancel = view.findViewById<TextView>(R.id.tv_cancel)
                    val btnNext = view.findViewById<Button>(R.id.btn_next)
                    val llSetFee = view.findViewById<LinearLayout>(R.id.ll_set_fee)
                    transaction?.let { tran ->
                        try {
                            setLevel(tvLevel)
                            origGas = tran.gasLimit
                            cGas = tran.gasLimit


                            var pValue = "0"
                            val valStr = tran.value.toString()
                            if (valStr != "0") {
                                val subvalStr = valStr.substring(0, valStr.length - 10)
                                val va8 = 10.0.pow(8.0)
                                pValue = "${subvalStr.toLong() / va8}".toPlainStr(8)
                            }
                            val netName = GoWallet.NET_MAPS[chainId]
                            tvDappName.text = "$netName"
                            tvDappUrl.text = url
                            tvValue.text = "$pValue $chainName"
                            tvOutAddress.text = address.toString()
                            tvInAddress.text = tran.recipient.toString()
                            tvPayMsg.text = "$chainName ${getString(R.string.home_transfer)}"

                            if (chainName == "BTY") {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val gasPriceResult = walletRepository.getGasPrice()
                                        val countResult =
                                            walletRepository.getTransactionCount(address.toString())
                                        withContext(Dispatchers.Main) {
                                            if (gasPriceResult.isSucceed()) {
                                                gasPriceResult.data()?.let {
                                                    gasPrice = it.substringAfter("0x").toLong(16)
                                                    origGasPirce = gasPrice.toBigInteger()
                                                }
                                            }
                                            if (countResult.isSucceed()) {
                                                countResult.data()?.let {
                                                    count = it.substringAfter("0x").toLong(16)
                                                }
                                            }

                                            showGasUI(
                                                gasPrice, transaction.gasLimit.toLong(), chainName
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }
                            } else {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val web3Url = GoWallet.getWeb3UrlL(chainId)
                                        val web3j = Web3j.build(HttpService(web3Url))
                                        val gasPriceResult = web3j.ethGasPrice().send()
                                        val countResult = web3j.ethGetTransactionCount(
                                            address.toString(), DefaultBlockParameterName.LATEST
                                        ).send()
                                        withContext(Dispatchers.Main) {
                                            gasPrice = gasPriceResult.gasPrice.toLong()
                                            origGasPirce = gasPriceResult.gasPrice
                                            count = countResult.transactionCount.toLong()
                                            showGasUI(
                                                gasPrice, transaction.gasLimit.toLong(), chainName
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }
                            }


                            payDialog =
                                AlertDialog.Builder(this@DappActivity).setView(view).create()
                                    .apply {
                                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                                    }
                            payDialog?.setCancelable(false)
                            payDialog?.show()

                            tvCancel.setOnClickListener {
                                feePosition = 2
                                payDialog?.dismiss()
                                val callback: String = String.format(
                                    JS_CALLBACK_ON_FAILURE, tran.leafPosition, JS_CANCELLED
                                )
                                binding.webDapp.evaluateJavascript(callback) { value: String? ->
                                    Timber.tag("WEB_VIEW").d(value)
                                }
                            }
                            btnNext.setOnClickListener {
                                try {
                                    //普通转账input64就传null
                                    var input64: String? = null
                                    tran.payload?.let { data ->
                                        val input = data.substringAfter("0x")
                                        input64 = Base64.encodeToString(
                                            Walletapi.hexTobyte(input), Base64.DEFAULT
                                        )
                                    }
                                    val createTran = CreateTran(
                                        address.toString(),
                                        cGas,
                                        cGasPrice,
                                        input64,
                                        count,
                                        tran.recipient.toString(),
                                        tran.value,
                                        tran.leafPosition
                                    )
                                    showPWD(createTran, chainName)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }



                            llSetFee.setOnClickListener {
                                gotoSetFee()
                            }
                            tvWCFee?.setOnClickListener {
                                gotoSetFee()
                            }
                            //
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun gotoSetFee() {
        if (::cGas.isInitialized && ::cGasPrice.isInitialized) {
            ARouter.getInstance().build(RouterPath.APP_SETFEE)
                .withInt(PARAM_FEE_POSITION, feePosition)
                .withLong(PARAM_CHAIN_ID, chainId)
                .withLong(PARAM_ORIG_GAS, origGas.toLong())
                .withLong(PARAM_GAS, cGas.toLong())
                .withLong(PARAM_GAS_PRICE, cGasPrice.toLong())
                .withLong(PARAM_ORIG_GAS_PRICE, origGasPirce.toLong())
                .navigation()
        }

    }


    private fun showGasUI(gasPrice: Long, gas: Long, chainName: String?) {
        try {
            cGasPrice = gasPrice.toBigInteger()
            val va9 = 10.0.pow(9.0)
            val va18 = 10.0.pow(18.0)
            val newGasPirce = "${gasPrice / va9}".toPlainStr(2)

            dGas = (gasPrice * gas) / va18
            val newGas = "$dGas".toPlainStr(6)
            tvFee?.text = "$newGas $chainName"
            tvWCFee?.text = "$newGas $chainName = Gas($gas)*GasPrice($newGasPirce GWEI)"
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun customChain(): Boolean {
        chainName?.let {
            if (it == "ETH" || it == "BNB") {
                return true
            }
        }
        return false
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
                try {
                    if (customChain()) {
                        if (dGas > 0.1) {
                            toast(getString(com.fzm.walletmodule.R.string.tip_fee_high))
                            return@setOnClickListener
                        }
                        if (!::cGasPrice.isInitialized || !::cGas.isInitialized) {
                            toast(getString(R.string.tip_init_fee))
                            return@setOnClickListener
                        }
                    }

                    val password = bindingDialog.etInput.text.toString()
                    if (password.isEmpty()) {
                        toast(getString(R.string.my_wallet_password_tips))
                        return@setOnClickListener
                    }
                    loading.show()
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val wallet = LitePal.find<PWallet>(MyWallet.getId(), true)
                            wallet?.let { w ->
                                val check = GoWallet.checkPasswd(password, w.password)
                                if (!check) {
                                    withContext(Dispatchers.Main) {
                                        toast(getString(R.string.pwd_fail_str))
                                        loading.dismiss()
                                    }
                                } else {
                                    when (w.type) {
                                        PWallet.TYPE_NOMAL -> {
                                            val bPassword = GoWallet.encPasswd(password)!!
                                            val mnem: String = GoWallet.decMenm(bPassword, w.mnem)
                                            chainName?.let { name ->
                                                val privKey = GoWallet.getPrikey(
                                                    if (name == "BTY") "ETH" else name, mnem
                                                )
                                                signAndSend(name, privKey, createTran)
                                            }
                                        }

                                        PWallet.TYPE_PRI_KEY -> {
                                            val priCoin = w.coinList[0]
                                            val privKey = priCoin.getPrivkey(password)
                                            chainName?.let { name ->
                                                signAndSend(name, privKey, createTran)
                                            }

                                        }

                                        else -> {}
                                    }

                                }

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }


                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun signAndSend(name: String, privKey: String, createTran: CreateTran) {
        try {
            //发送后重置为经济
            feePosition = 2
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
                                sendSuccess(sendHash, createTran.leafPosition)
                            }
                        } else {
                            //给H5一个反馈来刷新按钮
                            //弹出发送失败，数据解析失败，一般是发送到主网超时，或者主网卡住了
                            sendFail(send.error(), createTran.leafPosition);
                        }
                    }
                } else {
                    val send = GoWallet.sendTran(name, signed, "")
                    val sendHash = JSONObject(send!!).getString("result")
                    withContext(Dispatchers.Main) {
                        sendSuccess(sendHash, createTran.leafPosition)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private val gson = GsonBuilder().serializeNulls().create()

    private fun sendSuccess(sendHash: String, leafPosition: Long) {
        dis()
        toast(getString(R.string.send_suc_str))
        val callback = String.format(JS_CALLBACK_ON, leafPosition, sendHash)
        binding.webDapp.evaluateJavascript(callback) { value: String? ->
            Timber.tag("WEB_VIEW").d(value)
        }
    }

    private fun sendFail(error: String, leafPosition: Long) {
        dis()
        toast("${getString(R.string.basic_error_send)}")
        val callback: String = String.format(
            JS_CALLBACK_ON_FAILURE, leafPosition, JS_FAIL
        )
        binding.webDapp.evaluateJavascript(callback) { value: String? ->
            Timber.tag("WEB_VIEW").d(value)
        }
    }

    private fun dis() {
        loading.dismiss()
        pwdDialog?.dismiss()
        payDialog?.dismiss()
    }


    private fun showBottomDialog() {
        val bottomDialog = AlertDialog.Builder(this).create()
        val bottomBinding = DialogDappBottomBinding.inflate(layoutInflater)
        bottomBinding.tvSwitchAccount.setOnClickListener {
            try {
                bottomDialog.dismiss()
                val wallets = LitePal.where("type = ?", "${PWallet.TYPE_NOMAL}").find<PWallet>(true)
                val cName = if (chainName == "BTY") "ETH" else chainName
                val walletsDialog = AlertDialog.Builder(this).create()
                val walletsBinding = DialogDappWalletsBinding.inflate(layoutInflater)
                walletsDialog.setView(walletsBinding.root)
                walletsBinding.rvList.layoutManager = LinearLayoutManager(this)
                val adapter =
                    object : CommonAdapter<PWallet>(this, R.layout.item_dapp_wallet, wallets) {
                        override fun convert(holder: ViewHolder, t: PWallet, position: Int) {
                            val coin = t.coinList.find { it.name == cName }
                            holder.setVisible(
                                R.id.tv_current_wallet,
                                currentAddress == "${coin?.address}"
                            )
                            holder.setText(R.id.tv_wallet_name, t.name)
                            holder.setText(R.id.tv_address, "${coin?.address}")
                        }

                    }
                walletsBinding.rvList.adapter = adapter
                walletsBinding.rvList.setOnItemClickListener { viewHolder, i ->
                    val coin = wallets[i].coinList.find { it.name == cName }
                    coin?.let {
                        currentAddress = it.address
                        address = Address(currentAddress)
                        setupWeb3(chainId, nodeUrl, address)
                        binding.webDapp.reload()
                        MyWallet.setId(wallets[i].id)
                        walletsDialog.dismiss()
                    }

                }
                configWindow(walletsDialog)
                walletsDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }


        }
        bottomBinding.tvRefresh.setOnClickListener {
            binding.webDapp.reload()
            bottomDialog.dismiss()
        }
        bottomBinding.tvBrowserOpen.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(url)
            startActivity(intent)
            bottomDialog.dismiss()
        }
        bottomBinding.tvCopyUrl.setOnClickListener {
            ClipboardUtils.clip(this, url)
            bottomDialog.dismiss()
        }
        bottomBinding.tvExit.setOnClickListener {
            bottomDialog.dismiss()
            finish()
        }
        configWindow(bottomDialog)
        bottomDialog.setView(bottomBinding.root)
        bottomDialog.show()
    }

}