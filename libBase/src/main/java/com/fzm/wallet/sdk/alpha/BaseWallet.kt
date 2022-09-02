package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletBean
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.OutRepository
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.toWalletBean
import com.google.gson.Gson

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
abstract class BaseWallet(protected val wallet: PWallet) {

    protected val gson by lazy { Gson() }
    protected val walletRepository by lazy { rootScope.get<WalletRepository>(walletQualifier) }
    protected val outRepository by lazy { rootScope.get<OutRepository>(walletQualifier) }


     val walletInfo: WalletBean
        get() = wallet.toWalletBean()


    protected fun getChineseMnem(mnem: String): String {
        val afterString = mnem.replace(" ", "")
        val afterString2 = afterString.replace("\n", "")
        return afterString2.replace("", " ").trim()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseWallet

        if (wallet != other.wallet) return false

        return true
    }

    override fun hashCode(): Int {
        return wallet.hashCode()
    }


}