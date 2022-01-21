package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.bean.ExchangeFee
import com.fzm.wallet.sdk.bean.WithHold
import com.fzm.wallet.sdk.bean.toRequestBody
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall
import com.fzm.wallet.sdk.net.goCall

class ExchangeRepository constructor(private val apis: Apis) {
    companion object {
        const val token = "Basic MzNleGNoYW5nZTpleGNoYW5nZWZsYXNoMjAyMjAxMTg="
    }

    suspend fun flashExchange(
        cointype: String,
        tokensymbol: String,
        bindAddress: String,
        rawTx: String,
        amount: Double,
        to: String,
        gasfee: Boolean
    ): HttpResult<String> {
        val body = toRequestBody(
            "Exchange.FlashExchange",
            "cointype" to cointype,
            "tokensymbol" to tokensymbol,
            "bindAddress" to bindAddress,
            "rawTx" to rawTx,
            "amount" to amount,
            "to" to to,
            "gasfee" to gasfee
        )
        return goCall {
            apis.flashExchange(token, body)
        }
    }


    suspend fun getExLimit(address: String): HttpResult<Double> {
        return apiCall { apis.getExLimit(address) }
    }

    suspend fun getExFee(): HttpResult<ExchangeFee> {
        return apiCall { apis.getExFee() }
    }


}