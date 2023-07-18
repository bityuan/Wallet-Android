package com.fzm.walletdemo.wcv2

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.databinding.ActivityWconnectBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.google.gson.GsonBuilder

@Route(path = RouterPath.APP_WCONNECT)
class WConnectActivity : BaseActivity() {

    private val binding by lazy { ActivityWconnectBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired(name = RouterPath.PARAM_WC_URL)
    var wcUrl: String? = null

    private var myPriv = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initData()

    }

    override fun initData() {
        super.initData()
        wcUrl?.let {
            val bnbAddress = GoWallet.getChain("BNB")?.address

        }

    }



    //---------------------------------WCV2-----------------------------------

    private val gson = GsonBuilder()
        .serializeNulls()
        .create()






}
