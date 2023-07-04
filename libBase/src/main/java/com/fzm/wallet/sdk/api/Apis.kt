package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.ProConfig
import com.fzm.wallet.sdk.bean.*
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.DNSResponse
import com.fzm.wallet.sdk.net.HttpResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

    @POST("interface/wallet-coin")
    suspend fun getCoinList(@Body body: Map<String, Any>): HttpResponse<List<Coin>>

    @POST("interface/wallet-coin/search")
    suspend fun searchCoinList(@Body body: RequestBody): HttpResponse<List<Coin>>


    @POST("interface/recommend-coin")
    suspend fun getTabData(): HttpResponse<List<AddCoinTabBean>>

    @GET("interface/explore")
    suspend fun getExploreList(): HttpResponse<List<ExploreBean>>

    @GET("interface/explore/category")
    suspend fun getExploreCategory(@Query("id") id: Int): HttpResponse<List<ExploreBean>>

    @GET("interface/supported-chain")
    suspend fun getSupportedChain(): HttpResponse<List<Coin>>


    //获取公告
    @GET("interface/notice/list")
    suspend fun getNoticeList(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("type") type: Int
    ): HttpResponse<Notices>

    //获取公告详情
    @GET("interface/notice/detail")
    suspend fun getNoticeDetail(
        @Query("id") id: Int
    ): HttpResponse<Notice>

    /**
     * DNS域名查询
     * @param type 记录类型：1-地址类型 2-身份类型 9-自定义，默认为地址，只有地址类型支持反向解析，其他不支持
     * @param key 解析关键字，正向为域名，反向为地址或取值
     * @param kind 正向(0)/反向(1)解析，默认正向:域名查询地址
     */
    @GET("https://dns.bityuan.com/dns/dns/resolve")
    suspend fun getDNSResolve(
        @Query("type") type: Int,
        @Query("key") key: String,
        @Query("kind") kind: Int
    ): DNSResponse<List<String>>


    @GET(UPDATE_JSON)
    suspend fun getUpdate(): HttpResponse<AppVersion>

    companion object {
        /* update.json文件 示例
   //status:   4代表强制更新，1代表普通更新
   {
       "code": 0,
       "data": {
       "status": 1,
       "version": "1.2.2",
       "version_code": 12200,
       "download_url": "https://app-release.apk",
       "log": "1.优化使用体验\n2.已知BUG修复",
   }
   }

   */
        const val UPDATE_JSON = "https://.../update.json"
    }
}