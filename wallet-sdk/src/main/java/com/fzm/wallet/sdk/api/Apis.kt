package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.bean.ExchangeFee
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

    //---------------------------闪兑接口-------------------------------


    /**
     * 发起USDT兑换申请
     * @param token 接口权限
     */
    @POST(EXCHANGE_DO)
    suspend fun flashExchange(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): GoResponse<String>

    /**
     * 根据地址获取兑换额度
     * @param address trc20 usdt地址
     */
    @GET("$EXCHANGE_Manager/public/limit")
    suspend fun getExLimit(@Query("address") address: String): HttpResponse<Long>

    /**
     * 获取闪兑需要的手续费
     * @param address trc20 usdt地址
     */
    @GET("$EXCHANGE_Manager/public/fee")
    suspend fun getExFee(): HttpResponse<ExchangeFee>


    companion object {
        //审核管理后台
        const val EXCHANGE_Manager = "http://119.8.37.55:8888"

        //真正闪兑的功能模块
        const val EXCHANGE_DO = "https://159.138.88.29:18084"
    }
}