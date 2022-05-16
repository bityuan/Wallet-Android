package com.fzm.wallet.sdk.api

import com.fzm.wallet.sdk.net.GoResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface GoService {


    //根据用户地址和nft合约地址获取NFT数量
    @POST
    suspend fun getNFTBalance(@Body body: RequestBody): GoResponse<String>
}