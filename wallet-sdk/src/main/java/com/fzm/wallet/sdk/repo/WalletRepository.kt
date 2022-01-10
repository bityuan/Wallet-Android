package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall
import com.fzm.walletmodule.bean.toRequestBody

class WalletRepository constructor(private val apis: Apis) {
    suspend fun getCoinList(names: List<String>): HttpResult<List<Coin>> {
        val body = toRequestBody("names" to names)
        return apiCall { apis.getCoinList(body) }
    }


}