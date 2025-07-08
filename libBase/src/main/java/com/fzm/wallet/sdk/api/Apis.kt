package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.bean.*
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.DNSResponse
import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.GoStrResponse
import com.fzm.wallet.sdk.net.HttpResponse
import com.fzm.wallet.sdk.net.UrlConfig
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
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

    @POST(IPConfig.BTY_API_NODE)
    suspend fun sendTransaction(@Body body: RequestBody): GoStrResponse<String>


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


    @POST
    suspend fun createByContract(
        @Body body: RequestBody,
        @Url url: String = UrlConfig.GO_URL
    ): GoResponse<CreateRaw>


    //铭文 获取余额
    @GET("http://190.92.231.38:8080/api/v1/inscription/balance/{address}")
    suspend fun getBrc20Balance(@Path("address") address: String): GoResponse<Brc20Balances>

    //铭文 获取交易记录
    @GET("http://190.92.231.38:8080/api/v1/inscription/history/{address}/{name}/0/10")
    suspend fun getBrc20Tran(
        @Path("address") address: String,
        @Path("name") name: String
    ): List<Brc20Tran>

    //铭文：根据公钥创建地址
    @GET("http://190.92.231.38:8080/api/v1/inscription/genBtcWitNessAddr/{pubkey}/testnet")
    suspend fun genBtcWitNessAddr(
        @Path("pubkey") pubkey: String,
    ): String

    //查询可转移的块
    @GET("http://190.92.231.38:8080/api/v1/inscription/TransferAble/{address}/{name}")
    suspend fun transferAble(
        @Path("address") address: String,
        @Path("name") name: String,
    ): GoResponse<TransferAbles>

    //铭刻第1步：铭刻铭文获取utxo数据
    @GET("http://190.92.231.38:8080/api/v1/inscription/inscribe-transfer/{address}/{name}")
    suspend fun inscribeTransfer(
        @Path("address") address: String,
        @Path("name") name: String,
    ): String

    //铭刻第2步：构造签名铭文数据
    @POST("http://190.92.231.38:8080/api/v1/inscription/inscriptionTransfer")
    suspend fun inscriptionTransfer(
        @Body body: RequestBody,
    ): InsTransfer2

    //铭刻第3步：构造签名铭文数据
    @POST("http://190.92.231.38:8080/api/v1/transfer")
    suspend fun transfer(
        @Body body: RequestBody,
    ): List<String>

    //转账第1步：构造
    @GET("http://190.92.231.38:8080/api/v1/inscription/transfer/{address}/{name}/{inscriptionId}")
    suspend fun insTransfer(
        @Path("address") address: String?,
        @Path("name") name: String?,
        @Path("inscriptionId") inscriptionId: String?,
    ): String

    @POST("http://190.92.231.38:8080/api/v1/inscription/transfer")
    suspend fun insTransferPost(
        @Body body: RequestBody,
    ): InsTransfer2

}