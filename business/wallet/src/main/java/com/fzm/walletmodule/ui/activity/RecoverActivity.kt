package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityNewRecoverAddressBinding
import com.fzm.walletmodule.databinding.ActivityRecoverBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ListUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.*

@Route(path = RouterPath.WALLET_RECOVER)
class RecoverActivity : BaseActivity() {

    @JvmField
    @Autowired(name = PWallet.PWALLET_ID)
    var walletid: Long = 0

    private val binding by lazy { ActivityRecoverBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = "找回资产"
        initView()
    }

    override fun initView() {
        super.initView()
    }

}