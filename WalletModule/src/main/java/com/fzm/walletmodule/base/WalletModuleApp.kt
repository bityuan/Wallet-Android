package com.fzm.walletmodule.base

import android.content.Context
import com.bumptech.glide.Glide
import com.fzm.walletmodule.net.appModule
import com.tencent.mmkv.MMKV
import org.jetbrains.anko.doAsync
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

import org.litepal.LitePal

class WalletModuleApp {
    companion object {
        var context: Context? = null
        fun init(context: Context) {
            this.context = context
            MMKV.initialize(context)
            LitePal.initialize(context)

        }
    }

}