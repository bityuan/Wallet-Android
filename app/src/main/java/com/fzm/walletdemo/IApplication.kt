package com.fzm.walletdemo

import android.app.Application
import android.os.Build
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.nftModule
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.BuildConfig
import com.fzm.wallet.sdk.ProConfig
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.fzm.walletmodule.net.walletModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class IApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //开源环境
        BWallet.get().setUrls(ProConfig.BASE_URL,ProConfig.GO_URL)
        WalletModuleApp.init(this)
        startKoin {
            androidContext(this@IApplication)
            modules(module {
                //开源环境
                //BWallet.get().init(this@IApplication, this, "1", APP_SYMBOL, "", APP_KEY, "${Build.MANUFACTURER} ${Build.MODEL}")
                BWallet.get().init(
                    this@IApplication,
                    this,
                    "1",
                    "",
                    "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            })
            modules(walletModule)
            modules(nftModule)
        }
        if (BuildConfig.DEBUG) {
            ARouter.openLog()
            ARouter.openDebug()
        }
        ARouter.init(this)
    }

}