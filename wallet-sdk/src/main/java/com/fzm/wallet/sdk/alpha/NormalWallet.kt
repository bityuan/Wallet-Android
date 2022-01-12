package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.base.DEFAULT_COINS
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
class NormalWallet(wallet: PWallet) : BaseWallet(wallet) {

    override suspend fun init(
        user: String,
        mnem: String,
        mnemType: Int,
        walletName: String,
        password: String,
        coins: List<Coin>
    ): String {
        if (mnem.isEmpty()) {
            throw Exception("助记词不能为空")
        }
        if (walletName.isEmpty()) {
            throw Exception("钱包名称不能为空")
        }
        if (password.isEmpty()) {
            throw Exception("钱包密码不能为空")
        }
        val wallet = PWallet().apply {
            this.mnemType = mnemType
            if (mnemType == PWallet.TYPE_CHINESE) {
                this.mnem = getChineseMnem(mnem)
            } else {
                this.mnem = mnem
            }
            this.type = PWallet.TYPE_NOMAL
            this.name = walletName
            this.password = password
        }

        return GoWallet.createWallet(wallet, coins.ifEmpty { DEFAULT_COINS }).id.toString()
    }
}