package com.fzm.walletdemo

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.fzm.walletmodule.base.WalletModuleApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WalletModuleApp.init(this)
    }
    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)

    }
}