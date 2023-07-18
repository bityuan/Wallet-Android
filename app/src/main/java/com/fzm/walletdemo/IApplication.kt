package com.fzm.walletdemo

import android.app.Application
import android.os.Build
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.nftModule
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.fzm.walletmodule.net.walletModule
import com.umeng.commonsdk.UMConfigure
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class IApplication : Application() {

    companion object {
        const val APP_SYMBOL = "open_wallet"
        const val APP_KEY = "0425823a38b591b104ca0c3fcf1f3d9d"
        const val BASE_URL = "http://8.218.140.119:8082"
        const val GO_URL = "https://8.218.140.119:8083"
    }

    override fun onCreate() {
        super.onCreate()
        //开源环境
        //BWallet.get().setUrls(BASE_URL,GO_URL)
        val baseUrl = when (W.appType) {
            IPConfig.APP_MY_DAO -> IPConfig.BASE_URL
            IPConfig.APP_YBC -> IPConfig.BASE_URL_YBC
            IPConfig.APP_YBS -> IPConfig.BASE_URL_YBS
            IPConfig.APP_YJM -> IPConfig.BASE_URL_YJM
            else -> IPConfig.BASE_URL
        }
        val goUrl = when (W.appType) {
            IPConfig.APP_MY_DAO -> IPConfig.GO_URL
            IPConfig.APP_YBC -> IPConfig.GO_URL_YBC
            IPConfig.APP_YBS -> IPConfig.GO_URL_YBS
            IPConfig.APP_YJM -> IPConfig.GO_URL_YJM
            else -> IPConfig.GO_URL
        }
        BWallet.get().setUrls(baseUrl, goUrl)
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
                    IPConfig.APP_SYMBOL,
                    "",
                    IPConfig.APP_KEY,
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
        initUmeng()
    }


    private fun initUmeng() {
        UMConfigure.setLogEnabled(BuildConfig.DEBUG);
        UMConfigure.init(this, IPConfig.UMENG_APP_KEY, "test", UMConfigure.DEVICE_TYPE_PHONE, "");
    }

    private fun initWCV2(){

    }
}