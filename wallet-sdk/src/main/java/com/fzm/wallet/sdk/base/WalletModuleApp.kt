package com.fzm.wallet.sdk.base

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.tencent.mmkv.MMKV
import org.litepal.LitePal

@SuppressLint("StaticFieldLeak")
object WalletModuleApp {

    val context: Context
        get() = checkNotNull(mContext) { "please call init() first" }

    private var mContext: Context? = null

    fun init(context: Application) {
        mContext = context
        MMKV.initialize(context)
        LitePal.initialize(context)
    }
}