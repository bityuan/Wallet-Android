package com.fzm.walletmodule.net

import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletmodule.vm.OutViewModel
import org.koin.dsl.module


val viewModelModule = module {
    factory(walletQualifier) { OutViewModel(get(walletQualifier)) }
}