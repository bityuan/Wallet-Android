package com.fzm.wallet.sdk.net

import com.fzm.wallet.sdk.api.ApiEnv
import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.base.BWallet
import com.fzm.wallet.sdk.repo.OutRepository
import com.fzm.wallet.sdk.utils.ToolUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val walletQualifier = _q(BWallet)

val walletBaseModules = module { walletNetModule() }

fun Module.walletNetModule() {

    single<OkHttpClient>(walletQualifier) {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
//            .addNetworkInterceptor(HttpLoggingInterceptor())
            .addInterceptor(get(walletQualifier))
            .build()
    }

    // Http头部基础数据
    single(walletQualifier) {
        Interceptor { chain ->
            val originalRequest = chain.request()
            val newBuilder = originalRequest.newBuilder()
            newBuilder
                .header("AppType", "TPOS")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Fzm-Request-Source", "wallet")
                .header("FZM-REQUEST-OS", "android")
                .header("FZM-REQUEST-UUID", ToolUtils.getMyUUID(get()))
                .header(
                    "version",
                    "${ToolUtils.getVersionName(get())},${ToolUtils.getVersionCode(get())}"
                )
                .header(
                    "device",
                    "${android.os.Build.BRAND},${android.os.Build.MODEL},${android.os.Build.VERSION.RELEASE}"
                )
                .method(originalRequest.method(), originalRequest.body())
            chain.proceed(newBuilder.build())
        }
    }

    single(walletQualifier) { OutRepository(get(walletQualifier)) }

    single<Retrofit>(walletQualifier) {
        Retrofit.Builder()
            .baseUrl(ApiEnv.BASE_URL)
            .client(get(walletQualifier))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single(walletQualifier) { get<Retrofit>(walletQualifier).create(Apis::class.java) }
}