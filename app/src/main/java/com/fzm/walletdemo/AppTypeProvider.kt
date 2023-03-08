package com.fzm.walletdemo

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.base.IAppTypeProvider
import com.fzm.wallet.sdk.base.ROUTE_APP_TYPE

@Route(path = ROUTE_APP_TYPE)
class AppTypeProvider : IAppTypeProvider {
    override fun getAppType(): Int {
        return W.appType
    }

    override fun init(context: Context?) {
    }
}