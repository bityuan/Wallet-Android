package com.fzm.wallet.sdk.net

import com.fzm.wallet.sdk.BuildConfig
import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.base.BWallet
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.fzm.wallet.sdk.net.security.SSLSocketClient
import com.fzm.wallet.sdk.repo.ExchangeRepository
import com.fzm.wallet.sdk.repo.OutRepository
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.ToolUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.context.KoinContextHandler
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*


val rootScope: Scope
    get() = KoinContextHandler.get()._scopeRegistry.rootScope

val walletQualifier = _q(BWallet)

val walletBaseModules = module { walletNetModule() }

fun Module.walletNetModule() {

    single<OkHttpClient>(walletQualifier) {

        RetrofitUrlManager.getInstance().apply {
            putDomain(UrlConfig.DOMAIN_URL_GO, UrlConfig.GO_URL)
            putDomain(UrlConfig.DOMAIN_EXCHANGE_MANAGER, UrlConfig.EXCHANGE_MANAGER)
            putDomain(UrlConfig.DOMAIN_EXCHANGE_DO, UrlConfig.EXCHANGE_DO)
        }.with(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(get(walletQualifier))
           /*     .addNetworkInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = when (BuildConfig.DEBUG) {
                            true -> HttpLoggingInterceptor.Level.BODY
                            false -> HttpLoggingInterceptor.Level.NONE
                        }
                    }
                )*/
                .sslSocketFactory(
                    SSLSocketClient.getSSLSocketFactory(),
                    SSLSocketClient.getTrustManager()
                )
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
        )


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
                .header("FZM-PLATFORM-ID", "81")
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

    single(walletQualifier) { ExchangeRepository(get(walletQualifier)) }
}

object UrlConfig {
    const val DOMAIN_URL_BASE = "url_base"
    const val DOMAIN_URL_GO = "url_go"
    const val DOMAIN_EXCHANGE_MANAGER = "exchange_manager"
    const val DOMAIN_EXCHANGE_DO = "exchange_do"


    private val config: Properties by lazy { openAssets() }

    val BASE_URL: String = config.getProperty("BASE_URL")
    val GO_URL: String = config.getProperty("GO_URL")
    val EXCHANGE_MANAGER: String = config.getProperty("EXCHANGE_MANAGER")
    val EXCHANGE_DO: String = config.getProperty("EXCHANGE_DO")


    private fun openAssets(): Properties {
        try {
            val open = WalletModuleApp.context.assets.open("app-base.properties")
            val config = Properties()
            config.load(InputStreamReader(open, Charset.forName("UTF-8")))
            open.close()
            return config
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Properties()
    }


}