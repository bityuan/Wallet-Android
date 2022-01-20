package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.WithHold
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.HttpResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

@JvmSuppressWildcards
interface Apis {

    //获取矿工费
    @GET("/goapi/interface/fees/recommended")
    suspend fun getMinerList(
        @Query("name") name: String
    ): HttpResponse<Miner>


    @GET("interface/coin/get-with-hold")
    suspend fun getWithHold(
        @Query("platform") paltform: String,
        @Query("coinname") coinName: String
    ): HttpResponse<WithHold>


    @POST("interface/wallet-coin")
    suspend fun getCoinList(@Body body: Map<String, Any>): HttpResponse<List<Coin>>


    @POST("https://159.138.88.29:18084/")
    suspend fun flashExchange(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): GoResponse<String>


}