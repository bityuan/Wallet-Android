package com.fzm.walletdemo

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.walletdemo.databinding.ActivityDappBinding
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import wendu.dsbridge.CompletionHandler
import wendu.dsbridge.DWebView


@Route(path = RouterPath.EX_DAPP)
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


    private fun initWebView() {
        binding.webDapp.settings.domStorageEnabled = true
        binding.webDapp.settings.javaScriptEnabled = true
        binding.webDapp.addJavascriptObject(JsApi(), "jsapi")
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

            handler.complete("$msg [ asyn call]")
        }
    }
}