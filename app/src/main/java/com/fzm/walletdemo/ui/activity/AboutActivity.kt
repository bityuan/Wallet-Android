package com.fzm.walletdemo.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityAboutBinding
import com.fzm.walletmodule.ui.base.BaseActivity
@Route(path = RouterPath.APP_ABOUT)
class AboutActivity : BaseActivity() {

    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}