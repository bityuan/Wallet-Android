package com.fzm.wallet.sdk.base

import com.alibaba.android.arouter.facade.template.IProvider

const val Q_BWallet = "Q_BWallet"
var FZM_PLATFORM_ID = "1"

const val ROUTE_APP_TYPE = "/app/route_app_type"
interface IAppTypeProvider: IProvider {
    fun getAppType(): Int
}