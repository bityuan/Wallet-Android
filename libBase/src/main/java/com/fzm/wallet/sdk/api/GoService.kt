package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.net.GoResponse
import com.fzm.wallet.sdk.net.UrlConfig.DOMAIN_URL_GO
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.DOMAIN_NAME_HEADER
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GoService {


    //根据用户地址和nft合约地址获取NFT数量
    @Headers("$DOMAIN_NAME_HEADER$DOMAIN_URL_GO")
    @POST(".")
    suspend fun getNFTBalance(
        @Header("SessionId") sessionId: String,
        @Body body: RequestBody
    ): GoResponse<String>
}