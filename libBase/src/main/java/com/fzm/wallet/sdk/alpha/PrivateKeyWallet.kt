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
class PrivateKeyWallet(wallet: PWallet) : BaseWallet(wallet) {


    override suspend fun init(configuration: WalletConfiguration): String {
        return with(configuration) {
            configuration.coins[0].let { chooseChain ->
                val pubkey = GoWallet.priToPub(chooseChain.chain, privateKey!!)
                    ?: throw ImportWalletException("私钥有误")
                val address = Walletapi.pubToAddress_v2(chooseChain.chain, pubkey)
                chooseChain.status = Coin.STATUS_ENABLE
                chooseChain.pubkey = Walletapi.byteTohex(pubkey)
                chooseChain.address = address

                val bPassword = Walletapi.encPasswd(password)
                val encByteKey = Walletapi.encKey(bPassword, Walletapi.hexTobyte(privateKey))
                val encPrivateKey = Walletapi.byteTohex(encByteKey)
                chooseChain.setPrivkey(encPrivateKey)
                configuration.coins = listOf(chooseChain)
            }
            withContext(Dispatchers.IO) {
                wallet.also {
                    it.mnemType = 0
                    it.mnem = ""
                    it.type = PWallet.TYPE_PRI_KEY
                    it.name = walletName
                    password?.let { p ->
                        it.password = GoWallet.passwdHash(Walletapi.encPasswd(p))
                    }
                    it.user = configuration.user
                }
                LitePal.saveAll(coins)
                wallet.coinList.addAll(coins)
                wallet.save()
                wallet.id.toString()
            }


        }
    }
}