package com.fzm.walletmodule.net


import com.fzm.walletmodule.api.ApiEnv
import com.fzm.walletmodule.api.Apis
import com.fzm.walletmodule.repo.OutRepository
import com.fzm.walletmodule.repo.WalletRepository
import com.fzm.walletmodule.vm.OutViewModel
import com.fzm.walletmodule.vm.WalletViewModel
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


val viewModelModule = module {
    factory { OutViewModel(get()) }
    factory { WalletViewModel(get()) }
}
val repositoryModule = module {
    single { OutRepository(get()) }
    single { WalletRepository(get()) }
}

val appNetModules = module {
    single<Retrofit>() {
        get<Retrofit.Builder>()
                .baseUrl(ApiEnv.BASE_URL)
                .client(get<OkHttpClient>())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}

val appServiceModule = module {
    single {
        get<Retrofit>().create(Apis::class.java)
    }
}

val appModule = listOf( appNetModules, appServiceModule,viewModelModule, repositoryModule)