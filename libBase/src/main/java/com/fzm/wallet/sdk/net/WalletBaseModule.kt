package com.fzm.wallet.sdk.net

import com.fzm.wallet.sdk.BWalletImpl
import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.base.FZM_PLATFORM_ID
import com.fzm.wallet.sdk.base.Q_BWallet
import com.fzm.wallet.sdk.net.security.SSLSocketClient
import com.fzm.wallet.sdk.repo.OutRepository
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.LocalManageUtil
import com.fzm.wallet.sdk.utils.ToolUtils
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


val walletQualifier = _q(Q_BWallet)

val walletBaseModules = module { walletNetModule() }

fun Module.walletNetModule() {

    single<OkHttpClient>(walletQualifier) {

        RetrofitUrlManager.getInstance().apply {
            putDomain(UrlConfig.DOMAIN_URL_GO, UrlConfig.GO_URL)
        }.with(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val platformId = FZM_PLATFORM_ID
                    val originalRequest = chain.request()
                    val newBuilder = originalRequest.newBuilder()
                    newBuilder
                        .header("AppType", "TPOS")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Fzm-Request-Source", "wallet")
                        .header("FZM-REQUEST-OS", "android")
                        .header("FZM-PLATFORM-ID", platformId)
                        .header("lang", LocalManageUtil.getSelectLanguage())
                        .header(
                            "version",
                            "${ToolUtils.getVersionName(get())},${ToolUtils.getVersionCode(get())}"
                        )
                        .header(
                            "device",
                            "${android.os.Build.BRAND},${android.os.Build.MODEL},${android.os.Build.VERSION.RELEASE}"
                        )
                        .method(originalRequest.method, originalRequest.body)
                    chain.proceed(newBuilder.build())
                }
                .addNetworkInterceptor(
                    /*HttpLoggingInterceptor().apply {
                        level = when (BuildConfig.DEBUG) {
                            true -> HttpLoggingInterceptor.Level.BODY
                            false -> HttpLoggingInterceptor.Level.NONE
                        }
                    }*/
                    MyHttpLoggingInterceptor()
                )
                .sslSocketFactory(
                    SSLSocketClient.getSSLSocketFactory(),
                    SSLSocketClient.getTrustManager()
                )
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
        )


            .build()
    }


    single<Retrofit>(walletQualifier) {
        Retrofit.Builder()
            .baseUrl(UrlConfig.BASE_URL)
            .client(get(walletQualifier))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single(walletQualifier) { get<Retrofit>(walletQualifier).create(Apis::class.java) }
    single(walletQualifier) { OutRepository(get(walletQualifier)) }
    single(walletQualifier) { WalletRepository(get(walletQualifier)) }


}

object UrlConfig {
    const val DOMAIN_URL_GO = "url_go"


    val BASE_URL: String by lazy { BWalletImpl.BASE_URL }
    val GO_URL: String by lazy { BWalletImpl.GO_URL }


}