package com.fzm.walletmodule.repo

import com.fzm.walletmodule.api.Apis
import com.fzm.walletmodule.bean.Miner
import com.fzm.walletmodule.net.HttpResult
import com.fzm.walletmodule.net.apiCall
import retrofit2.Retrofit

class OutRepository constructor(private val apis: Apis) {
    suspend fun getMiner(name: String): HttpResult<Miner> {
        return apiCall { apis.getMinerList(name) }
    }


}