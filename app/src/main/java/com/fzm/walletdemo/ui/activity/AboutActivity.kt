package com.fzm.walletdemo.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.ext.oneClick
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityAboutBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import org.jetbrains.anko.sdk27.coroutines.onClick

@Route(path = RouterPath.APP_ABOUT)
class AboutActivity : BaseActivity() {

    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.ivAbout.onClick {

        }
        binding.ivAbout.setOnClickListener {
            if (doMore6()) {
                ARouter.getInstance().build(RouterPath.APP_WEBTEST).navigation()
            }
        }
    }

    private var lastClickTime: Long = 0
    private var count: Long = 0
    private fun doMore6(): Boolean {
        val curClickTime = System.currentTimeMillis()
        if (curClickTime - lastClickTime < 3000) {
            count++
            if (count >= 6) {
                return true
            }
        } else {
            count = 0
        }
        lastClickTime = curClickTime
        return false
    }
}