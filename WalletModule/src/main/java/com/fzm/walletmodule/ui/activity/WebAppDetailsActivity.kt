package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseWebActivity
import kotlinx.android.synthetic.main.activity_web_app_details.*

class WebAppDetailsActivity : BaseWebActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_app_details)
        initView()
    }

    override fun initView() {
        super.initView()
        val title = intent.getStringExtra(TITLE)
        val url = intent.getStringExtra(URL)
        setTitle(title)
        initWebView(dweb_view)
        dweb_view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    webViewProgressBar.visibility = View.GONE
                } else {
                    webViewProgressBar.visibility = View.VISIBLE
                    webViewProgressBar.progress = newProgress
                }
            }
        }
        dweb_view.loadUrl(url!!)
    }

    companion object {
        val TITLE = "title"
        val URL = "url"
    }
}