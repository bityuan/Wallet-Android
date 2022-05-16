package com.fzm.nft

import com.fzm.wallet.sdk.net.walletQualifier
import org.koin.dsl.module
import retrofit2.Retrofit


val serviceModule = module {
    single(walletQualifier) { get<Retrofit>(walletQualifier).create(NFTService::class.java) }
}
val repoModule = module {
    single(walletQualifier) { NFTRepository(get(walletQualifier)) }
}
val vmModule = module {
    factory(walletQualifier) { NFTViewModel(get(walletQualifier)) }
}


val nftModule = listOf(serviceModule, repoModule, vmModule)