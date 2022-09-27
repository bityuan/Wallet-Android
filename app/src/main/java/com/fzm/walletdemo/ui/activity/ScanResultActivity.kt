package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.databinding.ActivityScanResultBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils

@Route(path = RouterPath.APP_SCAN_RESULT)
class ScanResultActivity : BaseActivity() {


    @JvmField
    @Autowired(name = RouterPath.PARAM_SCAN)
    var scan: String? = null

    private val binding by lazy { ActivityScanResultBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        binding.tvResult.text = scan
        binding.tvResult.setOnClickListener {
            scan?.let {
                ClipboardUtils.clip(this@ScanResultActivity, it)
            }

        }
    }
}