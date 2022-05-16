package com.fzm.walletdemo

import android.app.Application
import android.content.Context
import android.os.Build
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.fzm.walletmodule.net.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApplication : Application() {

    companion object {
        const val APP_SYMBOL = "open_wallet"
        const val APP_KEY = "0425823a38b591b104ca0c3fcf1f3d9d"
        const val BASE_URL = "https://wiki.bitfeel.cn"
        const val GO_URL = "https://183.129.226.77:8083"
    }

    override fun onCreate() {
        super.onCreate()
        BWallet.get().setUrls(BASE_URL,GO_URL)
        WalletModuleApp.init(this)
        startKoin {
            androidContext(this@MyApplication)
            modules(module {
                BWallet.get().init(this@MyApplication, this, "86", APP_SYMBOL, "", APP_KEY, "${Build.MANUFACTURER} ${Build.MODEL}")
            })
            modules(viewModelModule)
        }
    }
}