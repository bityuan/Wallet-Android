package com.fzm.walletdemo

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.fzm.walletmodule.base.WalletModuleApp
import com.fzm.walletmodule.net.appModule
import com.fzm.walletmodule.net.httpBaseModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WalletModuleApp.init(this)
        startKoin {
            androidContext(this@MyApplication)
            modules(httpBaseModules)
            modules(appModule)

        }
    }
    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)

    }
}