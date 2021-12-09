package com.fzm.walletmodule.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
        val url = intent.getStringExtra(URL)
        initWebView(dweb_view)
        dweb_view.loadUrl(url!!)
    }

    companion object {
        val URL = "url"
    }
}