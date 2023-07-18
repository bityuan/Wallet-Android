package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.alpha.NormalWallet
import com.fzm.wallet.sdk.alpha.PrivateKeyWallet
import com.fzm.wallet.sdk.alpha.RecoverWallet
import com.fzm.wallet.sdk.base.FZM_PLATFORM_ID
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletNetModule
import org.koin.core.module.Module

internal class BWalletImpl : BWallet {

    companion object {
        var BASE_URL = ""
        var GO_URL = ""
    }

    private var btyPrivkey: String = ""


    override fun init(
        context: Context,
        module: Module?,
        platformId: String,
        appSymbol: String,
        appId: String,
        appKey: String,
        device: String
    ) {
        FZM_PLATFORM_ID = platformId
        module?.walletNetModule()
    }

    override fun setUrls(baseUrl: String, goUrl: String) {
        BASE_URL = baseUrl
        GO_URL = goUrl
    }


    override suspend fun importWallet(configuration: WalletConfiguration): Long {
        return when (configuration.type) {
            PWallet.TYPE_NOMAL -> {
                NormalWallet(PWallet()).init(configuration)
            }
            PWallet.TYPE_PRI_KEY -> {
                PrivateKeyWallet(PWallet()).init(configuration)
            }
            PWallet.TYPE_RECOVER -> {
                RecoverWallet(PWallet()).init(configuration)
            }
            else -> {
                NormalWallet(PWallet()).init(configuration)
            }
        }
    }

    fun setBtyPrivkey(value: String) {
        this.btyPrivkey = value
    }

    override fun getBtyPrikey(): String {
        return btyPrivkey
    }
}