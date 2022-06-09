package com.fzm.walletdemo

import android.app.Application
import android.os.Build
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.nftModule
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.fzm.walletmodule.net.walletModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class IApplication : Application() {


    companion object {
        const val APP_SYMBOL = "open_wallet"
        const val APP_KEY = "0425823a38b591b104ca0c3fcf1f3d9d"
        //const val BASE_URL = "https://wiki.bitfeel.cn"
        const val BASE_URL = "http://8.218.140.119:8082"
        const val GO_URL = "https://8.218.140.119:8083"
    }

    override fun onCreate() {
        super.onCreate()
        BWallet.get().setUrls(BASE_URL,GO_URL)
        WalletModuleApp.init(this)
        startKoin {
            androidContext(this@IApplication)
            modules(module {
                BWallet.get().init(this@IApplication, this, "1", APP_SYMBOL, "", APP_KEY, "${Build.MANUFACTURER} ${Build.MODEL}")
            })
            modules(walletModule)
            modules(nftModule)
        }
        ARouter.openLog()
        ARouter.openDebug()
        ARouter.init(this)
    }
}