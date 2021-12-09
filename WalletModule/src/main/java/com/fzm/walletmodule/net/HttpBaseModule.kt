package com.fzm.walletmodule.net

import com.fzm.walletmodule.base.WalletModuleApp
import com.fzm.walletmodule.utils.ToolUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val httpBaseModules = module {
    factory {
        OkHttpClient.Builder()
    }

    factory {
        Retrofit.Builder()
    }

    single<OkHttpClient> {
        get<OkHttpClient.Builder>()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor())
            .addInterceptor(get())
            .build()
    }

// Http头部基础数据
    single {
        Interceptor { chain ->
            val originalRequest = chain.request()
            val newBuilder = originalRequest.newBuilder()
            newBuilder
                .header("AppType", "TPOS")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Fzm-Request-Source", "wallet")
                .header("FZM-REQUEST-OS", "android")
                .header("FZM-REQUEST-UUID", ToolUtils.getMyUUID(WalletModuleApp.context))
                .header("version", "${ToolUtils.getVersionName(WalletModuleApp.context)},${ToolUtils.getVersionCode(WalletModuleApp.context)}")
                .header("device", "${android.os.Build.BRAND},${android.os.Build.MODEL},${android.os.Build.VERSION.RELEASE}")
                .method(originalRequest.method(), originalRequest.body())
            chain.proceed(newBuilder.build())
        }
    }
}