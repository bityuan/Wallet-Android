package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.alpha.NormalWallet
import com.fzm.wallet.sdk.alpha.PrivateKeyWallet
import com.fzm.wallet.sdk.alpha.RecoverWallet
import com.fzm.wallet.sdk.base.FZM_PLATFORM_ID
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletNetModule
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.OutRepository
import com.fzm.wallet.sdk.repo.WalletRepository
import org.koin.core.module.Module

internal class BWalletImpl : BWallet {

    companion object {
        var BASE_URL = ""
        var GO_URL = ""
    }

    private var btyPrivkey: String = ""

    private val walletRepository by lazy { rootScope.get<WalletRepository>(walletQualifier) }

    private val outRepository by lazy { rootScope.get<OutRepository>(walletQualifier) }

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


    override suspend fun importWallet(configuration: WalletConfiguration, switch: Boolean): Long {
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

    override suspend fun getExploreList(): List<ExploreBean> {
        return walletRepository.getExploreList().dataOrNull() ?: emptyList()
    }

    override suspend fun getExploreCategory(id: Int): List<ExploreBean> {
        return walletRepository.getExploreCategory(id).dataOrNull() ?: emptyList()
    }

    fun setBtyPrivkey(value: String) {
        this.btyPrivkey = value
    }

    override fun getBtyPrikey(): String {
        return btyPrivkey
    }
}