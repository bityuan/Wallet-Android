package com.fzm.walletdemo.wconnect

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.fzm.walletmodule.ui.base.BaseActivity

@Route(path = RouterPath.APP_WCONNECT)
class WConnectActivity : BaseActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wconnect)


    }

}
