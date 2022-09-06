package com.fzm.wallet.sdk.alpha

import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.exception.ImportWalletException
import com.fzm.wallet.sdk.utils.GoWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import walletapi.Walletapi

class RecoverWallet(wallet: PWallet) : BaseWallet(wallet) {

    suspend fun init(configuration: WalletConfiguration): Long {
        return with(configuration) {
            coins[0].let { chooseChain ->
                GoWallet.priToPub(chooseChain.chain, privateKey!!)
                    ?: throw ImportWalletException("私钥有误")
                val bPassword = Walletapi.encPasswd(password)
                val encByteKey = Walletapi.encKey(bPassword, Walletapi.hexTobyte(privateKey))
                val encPrivateKey = Walletapi.byteTohex(encByteKey)
                chooseChain.status = Coin.STATUS_ENABLE
                chooseChain.setPrivkey(encPrivateKey)
                coins = listOf(chooseChain)
            }
            withContext(Dispatchers.IO) {
                wallet.also {
                    it.mnemType = 0
                    it.mnem = ""
                    it.type = type
                    it.name = walletName
                    password?.let { p ->
                        it.password = GoWallet.passwdHash(Walletapi.encPasswd(p))
                    }
                }
                LitePal.saveAll(coins)
                wallet.coinList.addAll(coins)
                wallet.save()
                wallet.id
            }


        }
    }
}