package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.BWalletImpl
import com.fzm.wallet.sdk.WalletConfiguration
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

class NormalWallet(wallet: PWallet) : BaseWallet(wallet) {

    suspend fun init(configuration: WalletConfiguration): Long {
        return with(configuration) {
            val mnemType = if (mnemonic!!.substring(0, 1).matches(REGEX_CHINESE.toRegex())) {
                PWallet.TYPE_CHINESE
            } else PWallet.TYPE_ENGLISH
            val mnem =
                if (mnemType == PWallet.TYPE_CHINESE) getChineseMnem(mnemonic!!) else mnemonic!!

            val hdWallet = withContext(Dispatchers.IO) {
                GoWallet.getHDWallet(Walletapi.TypeETHString, mnem)
            } ?: throw ImportWalletException("助记词有误")

            val pubKey = GoWallet.encodeToStrings(hdWallet.newKeyPub(0))
            val count = LitePal.where("pubkey = ?", pubKey).find<Coin>(true)

            if (count.isNotEmpty()) {
                throw ImportWalletException("助记词重复")
            }
            withContext(Dispatchers.IO) {
                coins.forEachIndexed { index, coin ->
                    val chain = if ("ETHW" == coin.chain) "ETH" else coin.chain
                    val hdWallet = GoWallet.getHDWallet(chain, mnem)
                    hdWallet?.let {
                        val privateKey = it.newKeyPriv(0)
                        val pubkey = it.newKeyPub(0)
                        val address = it.newAddress_v2(0)
                        val pubkeyStr = Walletapi.byteTohex(pubkey)
                        coin.sort = index
                        coin.status = Coin.STATUS_ENABLE
                        coin.pubkey = pubkeyStr
                        coin.address = address
                        if (Walletapi.TypeBtyString == coin.chain) {
                            val bWalletImpl = BWallet.get() as BWalletImpl
                            bWalletImpl.setBtyPrivkey(Walletapi.byteTohex(privateKey))
                        }
                    }
                }
                wallet.also {
                    it.mnemType = mnemType
                    it.type = type
                    it.name = walletName
                    val bPassword = Walletapi.encPasswd(password)
                    it.password = GoWallet.passwdHash(bPassword)
                    it.mnem = GoWallet.encMenm(bPassword, mnem)
                }

                LitePal.saveAll(coins)
                wallet.coinList.addAll(coins)
                wallet.save()
                wallet.id
            }


        }
    }
}