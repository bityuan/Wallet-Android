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

class PrivateKeyWallet(wallet: PWallet) : BaseWallet(wallet) {


    suspend fun init(configuration: WalletConfiguration): Long {
        return with(configuration) {
            configuration.coins[0].let { chooseChain ->
                privateKey =
                    if (Walletapi.checkWifCompress(privateKey)) Walletapi.wifKeyToHex(privateKey) else privateKey
                val pubkey = GoWallet.priToPub(chooseChain.chain, privateKey!!)
                    ?: throw ImportWalletException("私钥有误")
                val address = Walletapi.pubToAddress_v2(chooseChain.chain, pubkey)
                chooseChain.status = Coin.STATUS_ENABLE
                chooseChain.address = address

                val bPassword = Walletapi.encPasswd(password)
                val encByteKey = Walletapi.encKey(bPassword, Walletapi.hexTobyte(privateKey))
                val encPrivateKey = Walletapi.byteTohex(encByteKey)
                chooseChain.setPrivkey(encPrivateKey)
                val pubKey = GoWallet.priToPub(chooseChain.chain,privateKey!!)
                chooseChain.pubkey = Walletapi.byteTohex(pubKey)
                configuration.coins = listOf(chooseChain)
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