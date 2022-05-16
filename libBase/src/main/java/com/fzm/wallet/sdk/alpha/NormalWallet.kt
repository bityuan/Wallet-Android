package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.base.DEFAULT_COINS
import com.fzm.wallet.sdk.base.REGEX_CHINESE
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.exception.ImportWalletException
import com.fzm.wallet.sdk.utils.GoWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.Walletapi

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */
class NormalWallet(wallet: PWallet) : BaseWallet(wallet) {

    override suspend fun init(configuration: WalletConfiguration): String {
        return with(configuration) {
            if (mnemonic.isNullOrEmpty()) {
                throw ImportWalletException("助记词不能为空")
            }
            if (walletName.isNullOrEmpty()) {
                throw ImportWalletException("钱包名称不能为空")
            }
            if (password.isNullOrEmpty()) {
                throw ImportWalletException("钱包密码不能为空")
            }
            val type = if (mnemonic!!.substring(0, 1).matches(REGEX_CHINESE.toRegex())) {
                PWallet.TYPE_CHINESE
            } else PWallet.TYPE_ENGLISH
            val mnem = if (type == PWallet.TYPE_CHINESE) getChineseMnem(mnemonic!!) else mnemonic!!

            val hdWallet = withContext(Dispatchers.IO) {
                GoWallet.getHDWallet(Walletapi.TypeBtyString, mnem)
            } ?: throw ImportWalletException("助记词不存在")

            val pubKey = GoWallet.encodeToStrings(hdWallet.newKeyPub(0))
            val count = LitePal.where("pubkey = ?", pubKey).find<Coin>(true)

            if (count.isNotEmpty()) {
                if (count[0].getpWallet().type == PWallet.TYPE_NOMAL) {
                    throw ImportWalletException("助记词重复")
                }
            }

            wallet.also {
                it.mnemType = type
                it.mnem = mnem
                it.type = PWallet.TYPE_NOMAL
                it.name = walletName
                it.password = password
                it.user = configuration.user
            }

            GoWallet.createWallet(wallet, coins.ifEmpty { DEFAULT_COINS }).id.toString()
        }
    }
}