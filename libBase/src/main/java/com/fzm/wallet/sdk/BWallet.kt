package com.fzm.wallet.sdk

import android.content.Context
import com.fzm.wallet.sdk.bean.ExploreBean
import org.koin.core.module.Module

interface BWallet {

    companion object {
        private val wallet by lazy { BWalletImpl() }
        fun get(): BWallet = wallet

    }

    fun init(
        context: Context,
        module: Module?,
        platformId: String,
        appSymbol: String,
        appId: String,
        appKey: String,
        device: String
    )

    fun setUrls(baseUrl: String, goUrl: String)

    suspend fun importWallet(configuration: WalletConfiguration, switch: Boolean): Long
    suspend fun getExploreList(): List<ExploreBean>
    suspend fun getExploreCategory(id: Int): List<ExploreBean>

    fun getBtyPrikey(): String?

}