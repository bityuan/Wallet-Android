package com.fzm.walletmodule.repo

import com.fzm.walletmodule.api.Apis
import com.fzm.walletmodule.bean.Miner
import com.fzm.walletmodule.bean.WithHold
import com.fzm.walletmodule.bean.toRequestBody
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.net.HttpResult
import com.fzm.walletmodule.net.apiCall
import retrofit2.Retrofit

class WalletRepository constructor(private val apis: Apis) {
    suspend fun getCoinList(names: List<String>): HttpResult<List<Coin>> {
        val body = toRequestBody("names" to names)
        return apiCall { apis.getCoinList(body) }
    }


}