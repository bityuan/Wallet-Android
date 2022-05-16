package com.fzm.nft

import com.fzm.wallet.sdk.api.GoService
import com.fzm.wallet.sdk.bean.toRequestBody
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.goCall
import com.fzm.wallet.sdk.utils.GoWallet
import org.json.JSONObject
import walletapi.Walletapi

class NFTRepository constructor(private val goService: GoService) {
    suspend fun getNFTBalance(
        cointype: String = Walletapi.TypeETHString,
        tokensymbol: String = "",
        from: String,
        contractAddr: String
    ): HttpResult<String> {
        val jobj = JSONObject()
        jobj.put("from", from)
        jobj.put("contractAddr", contractAddr)
        val rawdata = JSONObject()
        rawdata.put("payload", jobj)
        rawdata.put("method", "NFT_BalanceOf")

        return goCall {
            GoWallet.checkSessionID()
            goService.getNFTBalance(
                GoWallet.sessionID,
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