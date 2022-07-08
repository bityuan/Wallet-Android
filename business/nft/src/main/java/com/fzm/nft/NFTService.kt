package com.fzm.nft

import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.UrlConfig.DOMAIN_URL_GO
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.DOMAIN_NAME_HEADER
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface NFTService {

    //-------------------------------------------------ETH-----------------------------------------

    //根据用户地址和nft合约地址获取NFT数量
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun getNFTBalance(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<String>

    //根据用户地址和nft合约地址查询账单
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun getNFTTran(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<List<NftTran>>

    //NFT转账
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun outNFT(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<String>

    //查询NFT列表
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun getNFTList(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<List<String>>


    //---------------------------------------------------slg--------------------------------------------
//根据用户地址和nft合约地址获取NFT数量
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun getSLGNFTBalance(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<String>

    //NFT转账
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun outSLGNFT(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<String>


    //根据用户地址和nft合约地址查询账单
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun getSLGNFTTran(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<List<NftTran>>
}