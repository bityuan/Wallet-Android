package com.fzm.walletmodule.api

import com.fzm.walletmodule.bean.Miner
import com.fzm.walletmodule.bean.WithHold
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.net.HttpResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Apis {

    //获取矿工费
    @GET("/goapi/interface/fees/recommended")
    suspend fun getMinerList(
        @Query("name") name: String
    ): HttpResponse<Miner>


    @GET("interface/coin/get-with-hold")
    suspend fun getWithHold(
        @Query("platform") paltform: String,
        @Query("coinname") coinName: String): HttpResponse<WithHold>

    @POST("interface/wallet-coin")
    suspend fun getCoinList(@Body body: RequestBody): HttpResponse<List<Coin>>
}