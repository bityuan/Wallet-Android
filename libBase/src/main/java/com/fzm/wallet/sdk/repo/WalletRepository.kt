package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.bean.*
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall
import com.fzm.wallet.sdk.net.dnsCall
import com.fzm.wallet.sdk.net.goCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.Query

class WalletRepository constructor(private val apis: Apis) {
    suspend fun getCoinList(names: List<String>): HttpResult<List<Coin>> {
        return apiCall { apis.getCoinList(mapOf("names" to names)) }
    }

    suspend fun searchCoinList(
        page: Int,
        limit: Int,
        keyword: String,
        chain: String,
        platform: String
    ): HttpResult<List<Coin>> {
        val body = toRequestBody(
            "page" to page,
            "limit" to limit,
            "keyword" to keyword,
            "chain" to chain,
            "platform" to platform
        )
        return apiCall { apis.searchCoinList(body) }
    }

    suspend fun getTabData(): HttpResult<List<AddCoinTabBean>> {
        return apiCall { apis.getTabData() }
    }

    suspend fun getExploreList(): HttpResult<List<ExploreBean>> {
        return apiCall { apis.getExploreList() }
    }

    suspend fun getExploreCategory(id: Int): HttpResult<List<ExploreBean>> {
        return apiCall { apis.getExploreCategory(id) }
    }

    suspend fun getSupportedChain(): HttpResult<List<Coin>> {
        return apiCall { apis.getSupportedChain() }
    }

    suspend fun getNoticeList(page: Int, limit: Int, type: Int): HttpResult<Notices> {
        return apiCall { apis.getNoticeList(page, limit, type) }
    }

    suspend fun getNoticeDetail(id: Int): HttpResult<Notice> {
        return apiCall { apis.getNoticeDetail(id) }
    }

    suspend fun getDNSResolve(type: Int, key: String, kind: Int): HttpResult<List<String>> {
        return dnsCall { apis.getDNSResolve(type, key, kind) }
    }

    suspend fun getUpdate(): HttpResult<AppVersion> {
        return apiCall { apis.getUpdate() }
    }

    suspend fun getTransactionCount(address: String): HttpResult<String> {

        val param = JSONObject()
        param.put("id", 1)
        param.put("jsonrpc", "2.0")
        param.put("method", "eth_getTransactionCount")
        param.put("params", JSONArray(listOf(address, "latest")))




        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goCall { apis.getTransactionCount(requestBody) }
    }
    suspend fun getGasPrice(): HttpResult<String> {

        val param = JSONObject()
        param.put("id", 1)
        param.put("jsonrpc", "2.0")
        param.put("method", "eth_gasPrice")

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goCall { apis.getGasPrice(requestBody) }
    }

    suspend fun sendRawTransaction(signHash: String?): HttpResult<String> {
        val param = JSONObject()
        param.put("id", 1)
        param.put("jsonrpc", "2.0")
        param.put("method", "eth_sendRawTransaction")
        param.put("params", JSONArray(listOf(signHash)))

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goCall { apis.sendRawTransaction(requestBody) }
    }

    suspend fun queryTxHistoryCount(
        cointype: String,
        tokensymbol: String,
        from: String,
        to: String
    ): HttpResult<String> {
        val jobj = JSONObject()
        jobj.put("cointype", cointype)
        jobj.put("tokensymbol", tokensymbol)
        jobj.put("from", from)
        jobj.put("to", to)
        val rawdata = JSONObject()
        rawdata.put("payload", jobj)
        rawdata.put("method", "QueryTxHistoryCount")


        return goCall {
            apis.queryTxHistoryCount(
                toRequestBody(
                    "Wallet.Transport",
                    "cointype" to cointype,
                    "tokensymbol" to tokensymbol,
                    "rawdata" to rawdata
                )
            )
        }
    }


    suspend fun queryTxHistoryDetail(
        cointype: String,
        tokensymbol: String,
        from: String,
        to: String,
        direction: Int,
        count: Int,
        index: Int
    ): HttpResult<TxTotal> {
        val jobj = JSONObject()
        jobj.put("cointype", cointype)
        jobj.put("tokensymbol", tokensymbol)
        jobj.put("from", from)
        jobj.put("to", to)
        jobj.put("direction", direction)
        jobj.put("count", count)
        jobj.put("index", index)
        val rawdata = JSONObject()
        rawdata.put("payload", jobj)
        rawdata.put("method", "QueryTxHistoryDetail")


        return goCall {
            apis.queryTxHistoryDetail(
                toRequestBody(
                    "Wallet.Transport",
                    "cointype" to cointype,
                    "tokensymbol" to tokensymbol,
                    "rawdata" to rawdata
                )
            )
        }
    }

}