package com.fzm.nft

import com.fzm.wallet.sdk.net.walletQualifier
import org.koin.dsl.module


val nftModule = module {
    single(walletQualifier) { NFTRepository(get(walletQualifier)) }

    factory(walletQualifier) { NFTViewModel(get(walletQualifier)) }
}