package com.fzm.walletdemo

import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.databinding.DialogLoadingBinding
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.ext.jsonToMap
import com.fzm.wallet.sdk.ext.maptoJsonStr
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.wallet.sdk.utils.ToolUtils
import com.fzm.walletdemo.databinding.ActivityDappBinding
import com.fzm.walletmodule.utils.NetWorkUtils
import com.fzm.walletmodule.utils.WalletRecoverUtils
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import wendu.dsbridge.CompletionHandler
import wendu.dsbridge.DWebView
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.Charset


@Route(path = RouterPath.APP_DAPP)
class DappActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDappBinding.inflate(layoutInflater) }

    private val loading by lazy {
        val loadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        return@lazy AlertDialog.Builder(this).setView(loadingBinding.root).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        doBar()
        binding.xbar.tvToolbar.text = name
        initWebView()
        url?.let {
            binding.webDapp.loadUrl(it)
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


    private fun initWebView() {
        DWebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        binding.webDapp.addJavascriptObject(JsApi(), null)
        binding.webDapp.settings.javaScriptEnabled = true
        binding.webDapp.settings.domStorageEnabled = true
        //解决http图片不显示
        binding.webDapp.settings.blockNetworkImage = false
        val userAgentString = binding.webDapp.settings.userAgentString
        val resultAgent = "$userAgentString;wallet;1.0"
        binding.webDapp.settings.userAgentString = resultAgent

        binding.webDapp.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressWeb.progress = newProgress
            }
        }
        binding.webDapp.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressWeb.visibility = View.GONE
            }
        }

    }


    inner class JsApi {

        @JavascriptInterface
        fun getCurrentBTYAddress(msg: Any, handler: CompletionHandler<String?>) {
            val chain = GoWallet.getChain("BTY")
            handler.complete(chain?.address)
        }

        @JavascriptInterface
        fun getAddress(msg: Any, handler: CompletionHandler<String?>) {
            val map = msg.toString().jsonToMap<String>()
            val cointype = map["cointype"]
            cointype?.let {
                val chain = GoWallet.getChain(it)
                handler.complete(chain?.address)
            }
        }

        @JavascriptInterface
        fun getDeviceId(msg: Any, handler: CompletionHandler<String?>) {
            handler.complete(ToolUtils.getMyUUID(this@DappActivity))
        }

        @JavascriptInterface
        fun getDeviceList(msg: Any, handler: CompletionHandler<String?>) {
            this@DappActivity.lifecycleScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(1024)
                /*在这里同样使用约定好的端口*/
                val port = 8804
                var server: DatagramSocket? = null
                try {
                    server = DatagramSocket(port)
                    val packet = DatagramPacket(buffer, buffer.size)
                    val list = mutableListOf<ArrayMap<String, String>>()
                    while (true) {
                        server.receive(packet)
                        val data =
                            String(packet.data, 0, packet.length, Charset.defaultCharset())
                        val ip = packet.address.toString()

                        if (ip.isNotEmpty()) {
                            val map = arrayMapOf<String, String>()
                            map["ip"] = ip.substring(1)
                            map["serial"] = data

                            if (isHaved(list, ip.substring(1))) {
                                val json = Gson().toJson(list)
                                withContext(Dispatchers.Main) {
                                    Log.v("dao", json)
                                    handler.complete(json)
                                }

                                break
                            } else {
                                list.add(map)
                            }

                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    server?.close()
                }
            }

        }

        @JavascriptInterface
        fun closeCurrentWebview(msg: Any?, handler: CompletionHandler<String?>?) {
            if (binding.webDapp.canGoBack()) {
                binding.webDapp.goBack()
            } else {
                finish()
            }
        }

        @JavascriptInterface
        fun setTitle(msg: Any, handler: CompletionHandler<String?>?) {
            val map = msg.toString().jsonToMap<String>()
            val title = map["title"]
            title?.let {
                setTitle(it)
            }
        }

        @JavascriptInterface
        fun getCurrentWifi(msg: Any?, handler: CompletionHandler<String?>) {
            val wifiInfo: WifiInfo = NetWorkUtils.getWifi(this@DappActivity)
            val map = arrayMapOf<String, String>()
            map["name"] = wifiInfo.ssid
            handler.complete(map.maptoJsonStr())
        }

        private var importSeedHandler: CompletionHandler<String?>? = null

        @JavascriptInterface
        fun importSeed(msg: Any?, handler: CompletionHandler<String?>?) {
            importSeedHandler = handler
            showPwdDialog()
        }

        @JavascriptInterface
        fun configPriv(msg: Any?, handler: CompletionHandler<String?>?) {
            val map = msg.toString().jsonToMap<Any?>()
            val cachePriv = map["cachePriv"]
            if (cachePriv == 1) {

            }
        }

        @JavascriptInterface
        fun sign(msg: Any?, handler: CompletionHandler<String?>?) {
            val map = msg.toString().jsonToMap<Any?>()
            val cointype = map["cointype"]
            val createHash = map["createHash"]
            val exer = map["exer"]
            val withhold = map["withhold"]
        }

        @JavascriptInterface
        fun signTxGroup(msg: Any?, handler: CompletionHandler<String?>?) {
            val map = msg.toString().jsonToMap<Any?>()
            val cointype = map["cointype"]
            val createHash = map["createHash"]
            val exer = map["exer"]
            val withhold = map["withhold"]
        }


        private fun showPwdDialog() {
            val bindingDialog = DialogPwdBinding.inflate(layoutInflater)
            val dialog =
                AlertDialog.Builder(this@DappActivity).setView(bindingDialog.root).create().apply {
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    show()
                }
            bindingDialog.ivClose.setOnClickListener {
                dialog.dismiss()
                val map: ArrayMap<String, String> = ArrayMap()
                map["error"] = "取消"
                importSeedHandler?.complete(map.maptoJsonStr())
            }
            bindingDialog.btnOk.setOnClickListener {
                val password = bindingDialog.etInput.text.toString()
                if (password.isNullOrEmpty()) {
                    toast("请输入密码")
                    return@setOnClickListener
                }
                this@DappActivity.lifecycleScope.launch(Dispatchers.IO) {
                    BWallet.get().getCurrentWallet()?.let {
                        withContext(Dispatchers.Main) {
                            loading.show()
                        }
                        val check = GoWallet.checkPasswd(password, it.password)
                        if (!check) {
                            withContext(Dispatchers.Main) {
                                toast("密码错误")
                                loading.dismiss()
                            }
                            return@let
                        }
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            if (!loading.isShowing) {
                                loading.show()
                            }

                        }
                        val bPassword = GoWallet.encPasswd(password)!!
                        val mnem: String = GoWallet.decMenm(bPassword, it.mnem)
                        val map: ArrayMap<String, String> = ArrayMap()
                        map["passwd"] = password
                        map["seed"] = mnem
                        importSeedHandler?.complete(map.maptoJsonStr())

                    }

                }
            }
        }

    }


    private fun isHaved(list: List<ArrayMap<String, String>>, ip: String): Boolean {
        for (arrayMap in list) {
            if (arrayMap["ip"] == ip) {
                return true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.add(0, 1, 0, "刷新")
        menuItem.setIcon(R.mipmap.ic_refresh)
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            //binding.webDapp.reload()
            //openMetaMask()
            WalletRecoverUtils().test()
        }
        return super.onOptionsItemSelected(item)
    }


    private fun openMetaMask() {
        //https://github.com/WalletConnect/kotlin-walletconnect-lib/issues/59
        intent = Intent(Intent.ACTION_VIEW)
        intent?.setPackage("io.metamask")
        //intent?.data = Uri.parse(config.toWCUri())
        startActivity(intent)

        //钱包的trustwallet/wallet-connect-kotlin
        //

    }
}