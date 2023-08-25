package com.fzm.walletdemo.ui.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.Gravity
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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.ext.toPlainStr
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.walletdemo.BuildConfig
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityDappBinding
import com.fzm.walletdemo.ui.JsApi
import com.fzm.walletdemo.ui.JsWCApi
import com.fzm.walletdemo.wcv2.CreateTran
import com.fzm.walletdemo.web3.URLLoadInterface
import com.fzm.walletdemo.web3.Web3ViewClient
import com.fzm.walletdemo.web3.bean.Address
import com.fzm.walletdemo.web3.bean.Web3Call
import com.fzm.walletdemo.web3.bean.Web3Transaction
import com.fzm.walletdemo.web3.listener.JsListener
import com.fzm.walletmodule.utils.ClipboardUtils
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import timber.log.Timber
import walletapi.Walletapi
import wendu.dsbridge.DWebView
import java.util.concurrent.TimeUnit
import kotlin.math.pow


@Route(path = RouterPath.APP_DAPP)
class DappActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDappBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var url: String? = null

    @JvmField
    @Autowired
    var chainNet:Int = -1

    private var address = Address.EMPTY
    private var nodeUrl = GoWallet.WEB3_BNB
    private var chainId: Long = GoWallet.CHAIN_ID_BNB_L

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
        title = getString(R.string.exp_str)
        ARouter.getInstance().inject(this)
        configChainNet()
        doBar()
        binding.xbar.tvToolbar.text = name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initWebView()
        }
        url?.let {
            binding.webDapp.loadUrl(it)
        }
    }

    private fun configChainNet(){
        when(chainNet){
            0 -> {
                val addr = GoWallet.getChain("ETH")?.address
                addr?.let {
                    address = Address(it)
                    nodeUrl = GoWallet.WEB3_BTY
                    chainId = GoWallet.CHAIN_ID_BTY_L
                }
            }
            1 -> {
                val addr = GoWallet.getChain("ETH")?.address
                addr?.let {
                    address = Address(it)
                    nodeUrl = GoWallet.WEB3_ETH
                    chainId = GoWallet.CHAIN_ID_ETH_L
                }
            }
            2 -> {
                val addr = GoWallet.getChain("BNB")?.address
                addr?.let {
                    address = Address(it)
                    nodeUrl = GoWallet.WEB3_BNB
                    chainId = GoWallet.CHAIN_ID_BNB_L
                }
            }
        }
    }

    private fun doBar() {
        setSupportActionBar(binding.xbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.xbar.toolbar.setNavigationOnClickListener { onBackPressed() }
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        view.fitsSystemWindows = true
        StatusBarUtil.StatusBarLightMode(this)
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.xbar.toolbar.title = ""
    }


    override fun onBackPressed() {
        if (binding.webDapp.canGoBack()) {
            binding.webDapp.goBack()
        } else {
            super.onBackPressed()
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun initWebView() {
        DWebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        binding.webDapp.addJavascriptObject(JsApi(binding.webDapp, this), "null")
        binding.webDapp.addJavascriptInterface(JsWCApi(binding.webDapp, this, jsListener), "alpha")
        binding.webDapp.settings.javaScriptEnabled = true
        binding.webDapp.settings.domStorageEnabled = true
        //解决http图片不显示
        binding.webDapp.settings.blockNetworkImage = false
        val userAgentString = binding.webDapp.settings.userAgentString
        val resultAgent = "$userAgentString;wallet;1.0"
        binding.webDapp.settings.userAgentString = resultAgent

        setupWeb3(chainId, nodeUrl, address)

    }

    private fun setupWeb3(chainId: Long, nodeUrl: String, address: Address) {
        val web3ViewClient = Web3ViewClient(this)
        binding.webDapp.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressWeb.progress = newProgress
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.webDapp.webViewClient =
                WrapWebViewClient(
                    binding.webDapp,
                    binding.progressWeb,
                    web3ViewClient,
                    binding.webDapp.webViewClient
                )
        }
        web3ViewClient.jsInjectorClient.chainId = chainId
        web3ViewClient.jsInjectorClient.rpcUrl = nodeUrl
        web3ViewClient.jsInjectorClient.walletAddress = address

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.add(0, 1, 0, getString(R.string.ref_str))
        val menuItem1 = menu.add(0, 2, 0, getString(R.string.copy_url_str))
        val menuItem2 = menu.add(0, 3, 0, getString(R.string.ex_open))
        val menuItem3 = menu.add(0, 4, 0, getString(R.string.exit_str))
        //menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            binding.webDapp.reload()
        }
        when (item.itemId) {
            1 -> {
                binding.webDapp.reload()
            }

            2 -> {
                ClipboardUtils.clip(this, url)
            }

            3 -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }

            4 -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    //---------------------------------------web3-------------------------------------------

    private class WrapWebViewClient(
        val dWebView: DWebView,
        val progressBar: ProgressBar,
        val internalClient: Web3ViewClient,
        val externalClient: WebViewClient?
    ) :
        WebViewClient() {
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
            return (externalClient!!.shouldOverrideUrlLoading(view, url)
                    || internalClient.shouldOverrideUrlLoading(view, url))
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            loadingError = true
            externalClient?.onReceivedError(view, request, error)
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            redirect = true
            return (externalClient!!.shouldOverrideUrlLoading(view, request)
                    || internalClient.shouldOverrideUrlLoading(view, request))
        }
    }


    //-------------------------web3 listener--------------------------------
    private val JS_CALLBACK = "AlphaWallet.executeCallback(%1\$s, null, %2\$s)"
    private val JS_CALLBACK_ON = "AlphaWallet.executeCallback(%1\$s, null, \"%2\$s\")"

    var gasPrice = 0L
    var count = 0L
    private var payDialog: AlertDialog? = null

    private val jsListener = object : JsListener {
        override fun onRequestAccounts(callbackId: Long) {

            val callback = String.format(
                JS_CALLBACK,
                callbackId,
                "[\"$address\"]"
            )

            binding.webDapp.evaluateJavascript(callback) { message: String? ->
                Timber.d(message)
            }
        }


        override fun onEthCall(call: Web3Call) {
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
                val value = web3j?.ethCall(tran, call.blockParam)?.send()?.value
                val callback: String = String.format(JS_CALLBACK_ON, call.leafPosition, value)
                withContext(Dispatchers.Main) {
                    //All WebView methods must be called on the same thread
                    //所以都放在主线程
                    binding.webDapp.evaluateJavascript(callback) { message: String? ->
                        Timber.d(message)
                    }
                }

            }

        }

        override fun onSignTransaction(transaction: Web3Transaction?) {
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
                val tvWCFee = view.findViewById<TextView>(R.id.tv_wc_fee)
                val tvCancel = view.findViewById<TextView>(R.id.tv_cancel)
                val btnNext = view.findViewById<Button>(R.id.btn_next)
                transaction?.let { tran ->
                    val va18 = 10.0.pow(18.0)
                    val pValue = "${tran.value.toLong() / va18}".toPlainStr(8)
                    val chainName = GoWallet.CHAIN_ID_MAPS_L[chainId]
                    val netName = GoWallet.NET_MAPS[chainId]
                    tvDappName.text = "$netName"
                    tvDappUrl.text = url
                    tvValue.text = "$pValue $chainName"
                    tvOutAddress.text = address.toString()
                    tvInAddress.text = tran.recipient.toString()
                    tvPayMsg.text = "$chainName ${getString(R.string.home_transfer)}"

                    if (chainName == "BTY") {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val gasPriceResult = walletRepository.getGasPrice()
                            val countResult =
                                walletRepository.getTransactionCount(address.toString())
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

                                showGasUI(
                                    tvWCFee,
                                    gasPrice,
                                    transaction.gasLimit.toLong(),
                                    chainName
                                )
                            }
                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val web3Url = GoWallet.getWeb3UrlL(chainId)
                            val web3j = Web3j.build(HttpService(web3Url))
                            val gasPriceResult = web3j.ethGasPrice().send()
                            val countResult = web3j.ethGetTransactionCount(
                                address.toString(), DefaultBlockParameterName.LATEST
                            ).send()
                            withContext(Dispatchers.Main) {
                                gasPrice = gasPriceResult.gasPrice.toLong()
                                count = countResult.transactionCount.toLong()
                                showGasUI(
                                    tvWCFee,
                                    gasPrice,
                                    transaction.gasLimit.toLong(),
                                    chainName
                                )
                            }
                        }
                    }


                    payDialog =
                        AlertDialog.Builder(this@DappActivity).setView(view).create().apply {
                            window?.setBackgroundDrawableResource(android.R.color.transparent)
                        }
                    payDialog?.setCancelable(false)
                    payDialog?.show()

                    tvCancel.setOnClickListener {
                        payDialog?.dismiss()
                    }
                    btnNext.setOnClickListener {
                        val input = tran.payload?.substringAfter("0x")
                        val input64 =
                            Base64.encodeToString(Walletapi.hexTobyte(input), Base64.DEFAULT)
                        val createTran = CreateTran(
                            address.toString(),
                            tran.gasLimit.toLong(),
                            gasPrice,
                            input64,
                            count,
                            tran.recipient.toString(),
                            tran.value.toLong()
                        )
                        showPWD(createTran, chainName)

                    }
                    //
                }

            }
        }
    }


    private fun showGasUI(tvWCFee: TextView, gasPrice: Long, gas: Long, chainName: String?) {
        try {
            val va9 = 10.0.pow(9.0)
            val va18 = 10.0.pow(18.0)
            val newGasPirce = "${gasPrice / va9}".toPlainStr(2)

            val dGas = (gasPrice * gas) / va18
            val newGas = "$dGas".toPlainStr(6)

            tvWCFee.text = "$newGas $chainName = Gas($gas)*GasPrice($newGasPirce GWEI)"
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
                                logDebug("构造数据 == $createJson")
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
                                                    dis(sendHash)
                                                }
                                            }
                                        }
                                    } else {
                                        val send = GoWallet.sendTran(chainName, signed, "")
                                        val sendHash = JSONObject(send!!).getString("result")
                                        withContext(Dispatchers.Main) {
                                            dis(sendHash)
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

    private val gson = GsonBuilder().serializeNulls().create()

    private fun dis(sendHash: String) {
        loading.dismiss()
        pwdDialog?.dismiss()
        payDialog?.dismiss()
        toast(getString(R.string.send_suc_str))
    }

}