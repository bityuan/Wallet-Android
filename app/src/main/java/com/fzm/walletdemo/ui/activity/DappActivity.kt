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
import com.fzm.wallet.sdk.base.MyWallet
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
import com.fzm.walletdemo.ui.JsApi
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
        binding.webDapp.addJavascriptObject(JsApi(binding.webDapp,this), null)
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