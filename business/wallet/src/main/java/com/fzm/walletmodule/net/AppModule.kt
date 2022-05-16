package com.fzm.walletmodule.net

import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletmodule.vm.OutViewModel
import com.fzm.walletmodule.vm.WalletViewModel
import org.koin.dsl.module


val walletModule = module {
    factory(walletQualifier) { OutViewModel(get(walletQualifier)) }
    factory(walletQualifier) { WalletViewModel(get(walletQualifier)) }
}