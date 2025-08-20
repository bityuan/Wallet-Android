package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.bean.CreateRaw
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.toRequestBody
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall
import com.fzm.wallet.sdk.net.goCall
import org.json.JSONObject

class OutRepository constructor(private val apis: Apis) {
    suspend fun getMiner(name: String): HttpResult<Miner> {
        return apiCall { apis.getMinerList(name) }
    }

    suspend fun createRawTransaction(
        cointype: String,
        tokensymbol: String,
        from: String,
        to: String,
        amount: Double,
        fee: Double,
        contractAddress: String
    ): HttpResult<CreateRaw> {
        val extend = JSONObject()
        extend.put("token_addr", contractAddress)
        val transaction = JSONObject()
        transaction.put("from", from)
        transaction.put("to", to)
        transaction.put("amount", amount)
        transaction.put("fee", fee)
        transaction.put("extend", extend)

        return goCall {
            apis.createByContract(
                toRequestBody(
                    "Wallet.CreateRawTransaction",
                    "cointype" to cointype,
                    "tokensymbol" to tokensymbol,
                    "transaction" to transaction
                )
            )
        }
    }

}