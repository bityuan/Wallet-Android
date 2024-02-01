package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall

class OutRepository constructor(private val apis: Apis) {
    suspend fun getMiner(name: String): HttpResult<Miner> {
        return apiCall { apis.getMinerList(name) }
    }



}