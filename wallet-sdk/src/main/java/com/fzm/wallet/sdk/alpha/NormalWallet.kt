package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.base.DEFAULT_COINS
import com.fzm.wallet.sdk.base.REGEX_CHINESE
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
class NormalWallet(wallet: PWallet) : BaseWallet(wallet) {

    override suspend fun init(configuration: WalletConfiguration): String {
        return with(configuration) {
            if (mnemonic.isNullOrEmpty()) {
                throw Exception("助记词不能为空")
            }
            if (walletName.isNullOrEmpty()) {
                throw Exception("钱包名称不能为空")
            }
            if (password.isNullOrEmpty()) {
                throw Exception("钱包密码不能为空 ")
            }
            val wallet = PWallet().apply {
                this.mnemType = if (mnemonic!!.substring(0, 1).matches(REGEX_CHINESE.toRegex())) {
                    PWallet.TYPE_CHINESE
                } else PWallet.TYPE_ENGLISH
                if (mnemType == PWallet.TYPE_CHINESE) {
                    this.mnem = getChineseMnem(mnem)
                } else {
                    this.mnem = mnem
                }
                this.type = PWallet.TYPE_NOMAL
                this.name = walletName
                this.password = password
            }

            GoWallet.createWallet(wallet, coins.ifEmpty { DEFAULT_COINS }).id.toString()
        }
    }
}