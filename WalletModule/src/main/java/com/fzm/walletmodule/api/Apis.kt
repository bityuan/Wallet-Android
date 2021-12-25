package com.fzm.walletmodule.api

import com.fzm.walletmodule.bean.Miner
import com.fzm.walletmodule.bean.WithHold
import com.fzm.walletmodule.net.HttpResponse
import retrofit2.Call
import retrofit2.http.GET
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
}