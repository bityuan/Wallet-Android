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

class AddressWallet(wallet: PWallet) : BaseWallet(wallet) {


    suspend fun init(configuration: WalletConfiguration): Long {
        return with(configuration) {
            configuration.coins[0].let { chooseChain ->
                chooseChain.status = Coin.STATUS_ENABLE
                chooseChain.address = address
                configuration.coins = listOf(chooseChain)
            }
            withContext(Dispatchers.IO) {
                wallet.also {
                    it.mnemType = 0
                    it.mnem = ""
                    it.type = type
                    it.name = walletName
                }
                LitePal.saveAll(coins)
                wallet.coinList.addAll(coins)
                wallet.save()
                wallet.id
            }


        }
    }
}