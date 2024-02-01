package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.bean.*
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.DNSResponse
import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.HttpResponse
import com.fzm.wallet.sdk.net.UrlConfig
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

@JvmSuppressWildcards
interface Apis {


    @GET("goapi/interface/fees/recommended")
    suspend fun getMinerList(
        @Query("name") name: String
    ): HttpResponse<Miner>

    @POST("v2api/interface/wallet-coin")
    suspend fun getCoinList(@Body body: Map<String, Any>): HttpResponse<List<Coin>>

    @POST("v2api/interface/wallet-coin/search")
    suspend fun searchCoinList(@Body body: RequestBody): HttpResponse<List<Coin>>


    @POST("v2api/interface/recommend-coin")
    suspend fun getTabData(): HttpResponse<List<AddCoinTabBean>>

    @GET("v2api/interface/explore")
    suspend fun getExploreList(): HttpResponse<List<ExploreBean>>

    @GET("v2api/interface/explore/category")
    suspend fun getExploreCategory(@Query("id") id: Int): HttpResponse<List<ExploreBean>>

    @GET("v2api/interface/supported-chain")
    suspend fun getSupportedChain(): HttpResponse<List<Coin>>


    //获取公告
    @GET("v2api/interface/notice/list")
    suspend fun getNoticeList(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("type") type: Int
    ): HttpResponse<Notices>

    //获取公告详情
    @GET("v2api/interface/notice/detail")
    suspend fun getNoticeDetail(
        @Query("id") id: Int
    ): HttpResponse<Notice>

    /**
     * DNS域名查询
     * @param type 记录类型：1-地址类型 2-身份类型 9-自定义，默认为地址，只有地址类型支持反向解析，其他不支持
     * @param key 解析关键字，正向为域名，反向为地址或取值
     * @param kind 正向(0)/反向(1)解析，默认正向:域名查询地址
     */
    @GET(IPConfig.DNS)
    suspend fun getDNSResolve(
        @Query("type") type: Int,
        @Query("key") key: String,
        @Query("kind") kind: Int
    ): DNSResponse<List<String>>

    @GET(IPConfig.UPDATE_JSON)
    suspend fun getUpdate(): HttpResponse<AppVersion>


    @POST(IPConfig.BTY_ETH_NODE)
    suspend fun getTransactionCount(@Body body: RequestBody): GoResponse<String>
    @POST(IPConfig.BTY_ETH_NODE)
    suspend fun getGasPrice(@Body body: RequestBody): GoResponse<String>
    @POST(IPConfig.BTY_ETH_NODE)
    suspend fun sendRawTransaction(@Body body: RequestBody): GoResponse<String>


    @POST
    suspend fun queryTxHistoryCount(
        @Body body: RequestBody,
        @Url url: String = UrlConfig.GO_URL
    ): GoResponse<String>


    @POST
    suspend fun queryTxHistoryDetail(
        @Body body: RequestBody,
        @Url url: String = UrlConfig.GO_URL
    ): GoResponse<TxTotal>
}