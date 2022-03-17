package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.bean.BrowserBean
import com.fzm.wallet.sdk.bean.ExchangeFee
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.WithHold
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.HttpResponse
import com.fzm.wallet.sdk.net.UrlConfig.DOMAIN_EXCHANGE_DO
import com.fzm.wallet.sdk.net.UrlConfig.DOMAIN_EXCHANGE_MANAGER
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.DOMAIN_NAME_HEADER
import okhttp3.RequestBody
import retrofit2.http.*

@JvmSuppressWildcards
interface Apis {


    @GET("/goapi/interface/fees/recommended")
    suspend fun getMinerList(
        @Query("name") name: String
    ): HttpResponse<Miner>


    @GET("interface/coin/get-with-hold")
    suspend fun getWithHold(
        @Query("platform") paltform: String,
        @Query("coinname") coinName: String
    ): HttpResponse<WithHold>

    @GET("goapi/interface/tokenview/explore")
    suspend fun getBrowserUrl(@Query("platform") platform: String): HttpResponse<BrowserBean>


    @POST("interface/wallet-coin")
    suspend fun getCoinList(@Body body: Map<String, Any>): HttpResponse<List<Coin>>

    @POST("interface/wallet-coin/search")
    suspend fun searchCoinList(@Body body: RequestBody): HttpResponse<List<Coin>>


    @POST("interface/recommend-coin")
    suspend fun getTabData(): HttpResponse<List<AddCoinTabBean>>

    //---------------------------exchange-------------------------------

    /**
     * apply
     * @param token
     */
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_EXCHANGE_DO")
    @POST("/")
    suspend fun flashExchange(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): GoResponse<String>

    /**
     * exchange limit
     * @param address
     */
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_EXCHANGE_MANAGER")
    @GET("public/limit")
    suspend fun getExLimit(
        @Query("address") address: String,
        @Query("cointype") cointype: String,
        @Query("tokensymbol") tokensymbol: String
    ): HttpResponse<Double>

    /**
     * exchange fee
     */
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_EXCHANGE_MANAGER")
    @GET("public/fee")
    suspend fun getExFee(
        @Query("cointype") cointype: String,
        @Query("tokensymbol") tokensymbol: String
    ): HttpResponse<ExchangeFee>
}