package com.fzm.walletdemo

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.multidex.MultiDex
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.fzm.walletmodule.net.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WalletModuleApp.init(this)
        startKoin {
            androidContext(this@MyApplication)
            modules(module {
                BWallet.get().init(this@MyApplication, this, "", "", "", "", "${Build.MANUFACTURER} ${Build.MODEL}")
            })
            modules(viewModelModule)
        }
    }
    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)

    }
}