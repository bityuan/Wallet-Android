package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.bean.*
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall
import com.fzm.wallet.sdk.net.dnsCall
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

}