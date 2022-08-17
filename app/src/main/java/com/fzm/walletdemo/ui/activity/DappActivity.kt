package com.fzm.walletdemo.ui.activity

import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.WalletBean
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.databinding.DialogLoadingBinding
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.ext.jsonToMap
import com.fzm.wallet.sdk.ext.toJSONStr
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.wallet.sdk.utils.ToolUtils
import com.fzm.walletdemo.BuildConfig
import com.fzm.walletdemo.databinding.ActivityDappBinding
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.NetWorkUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.Walletapi
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
            setCancelable(false)
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
        //binding.webDapp.addJavascriptObject(JSApi(this), null)
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

    // JS调用

    inner class JsApi {
        private var cointype = ""
        private var createHash = ""
        private var exer = ""
        private var withhold = -1

        //addressID 比特格式的地址传0， 以太坊格式的地址发送的时候addressID 传2
        private var addressid = -1


        @JavascriptInterface
        fun getCurrentBTYAddress(msg: Any, handler: CompletionHandler<String?>) {
            val chain = GoWallet.getChain("BTY")
            handler.complete(chain?.address)
        }

        @JavascriptInterface
        fun getAddress(msg: Any, handler: CompletionHandler<String?>) {
            val map = msg.toString().jsonToMap<String>()
            val chain = map["cointype"]
            chain?.let {
                val chain = GoWallet.getChain(it)
                handler.complete(toJSONStr("address" to chain?.address))
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
            //val map = msg.toString().jsonToMap<String>()
            val jsTitle = Gson().fromJson(msg.toString(), JsTitle::class.java)
            val title = jsTitle.title
            binding.xbar.tvToolbar.text = title
            handler?.complete()
        }

        @JavascriptInterface
        fun getCurrentWifi(msg: Any?, handler: CompletionHandler<String?>) {
            val wifiInfo: WifiInfo = NetWorkUtils.getWifi(this@DappActivity)
            handler.complete(toJSONStr("name" to wifiInfo.ssid))
        }


        @JavascriptInterface
        fun sign(msg: Any?, handler: CompletionHandler<String?>?) {
            val jsSign = Gson().fromJson(msg.toString(), JsSign::class.java)
            cointype = jsSign.cointype
            createHash = jsSign.createHash
            exer = jsSign.exer
            withhold = jsSign.withhold
            addressid = getAddressId()
            showPwdDialog(1, handler)
        }

        inner class JsTitle {
            var title = ""
        }

        inner class JsSign {
            var cointype = ""
            var createHash = ""
            var exer = ""
            var withhold = -1
        }

        @JavascriptInterface
        fun signTxGroup(msg: Any?, handler: CompletionHandler<String?>?) {
            val jsSign = Gson().fromJson(msg.toString(), JsSign::class.java)
            cointype = jsSign.cointype
            createHash = jsSign.createHash
            exer = jsSign.exer
            withhold = jsSign.withhold
            addressid = getAddressId()
            showPwdDialog(2, handler)
        }

        private fun getAddressId(): Int {
            return when (cointype) {
                "BTY" -> {
                    0
                }
                "YCC" -> {
                    2
                }
                else -> -1
            }
        }


        @JavascriptInterface
        fun importSeed(msg: Any?, handler: CompletionHandler<String?>?) {
            showPwdDialog(3, handler)
        }

        @JavascriptInterface
        fun scanQRCode(msg: Any?, handler: CompletionHandler<String?>?) {
            LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this@DappActivity, Observer { scan ->
                handler?.complete(scan)
            })
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }

        private fun showPwdDialog(from: Int, handler: CompletionHandler<String?>?) {
            val bindingDialog = DialogPwdBinding.inflate(layoutInflater)
            val dialog =
                AlertDialog.Builder(this@DappActivity).setView(bindingDialog.root).create().apply {
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    show()
                }
            bindingDialog.ivClose.setOnClickListener {
                dialog.dismiss()
                handler?.complete(toJSONStr("error" to "取消"))
            }
            bindingDialog.btnOk.setOnClickListener {
                val password = bindingDialog.etInput.text.toString()
                if (password.isEmpty()) {
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

                            when (from) {
                                1 -> {
                                    val priKey = getPrikey(it, password, cointype)
                                    val signTx = GoWallet.signTran(
                                        cointype,
                                        Walletapi.hexTobyte(createHash),
                                        priKey,
                                        addressid
                                    )
                                    handler?.complete(toJSONStr("signHash" to signTx))
                                    loading.dismiss()

                                }
                                2 -> {
                                    val priKey = getPrikey(it, password, cointype)
                                    val signTx = GoWallet.signTxGroup(
                                        exer,
                                        createHash,
                                        priKey,
                                        priKey,
                                        0.03,
                                        addressid
                                    )
                                    handler?.complete(toJSONStr("signHash" to signTx))
                                    loading.dismiss()

                                }
                                3 -> {
                                    val bPassword = GoWallet.encPasswd(password)!!
                                    val mnem: String = GoWallet.decMenm(bPassword, it.mnem)
                                    handler?.complete(
                                        toJSONStr(
                                            "passwd" to password,
                                            "seed" to mnem
                                        )
                                    )
                                    loading.dismiss()
                                }
                                else -> {}
                            }

                        }


                    }

                }
            }
        }

    }

    private fun getPrikey(walletBean: WalletBean, password: String, cointype: String): String {
        val bPassword = GoWallet.encPasswd(password)!!
        val priKey: String = when (walletBean.type) {
            2 -> {
                var thisCointype = ""
                val mnem: String = GoWallet.decMenm(bPassword, walletBean.mnem)
                if (cointype == "YCC") {
                    thisCointype = Walletapi.TypeETHString
                } else {
                    thisCointype = Walletapi.TypeBtyString
                }

                val priKey = GoWallet.getPrikey(thisCointype, mnem)
                priKey
            }
            //私钥
            4 -> {
                val priKey = LitePal.find<PWallet>(
                    walletBean.id,
                    true
                ).coinList[0].getPrivkey(password)
                priKey

            }
            else -> {
                ""
            }
        }
        return priKey
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
        val menuItem1 = menu.add(0, 2, 0, "复制链接")
        val menuItem2 = menu.add(0, 3, 0, "在浏览器中打开")
        val menuItem3 = menu.add(0, 4, 0, "退出")
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
}