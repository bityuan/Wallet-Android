package com.fzm.nft

import com.fzm.wallet.sdk.bean.toRequestBody
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.goCall
import com.fzm.wallet.sdk.utils.GoWallet
import org.json.JSONObject
import walletapi.Walletapi

class NFTRepository constructor(private val nftService: NFTService) {
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
            nftService.getNFTBalance(
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

    suspend fun getNFTTran(
        cointype: String = Walletapi.TypeETHString,
        tokensymbol: String = "",
        contractAddr: String,
        address: String,
        index: Int,
        count: Int,
        direction: Int,
        type: Int
    ): HttpResult<List<NftTran>> {
        val jobj = JSONObject()
        jobj.put("contractAddr", contractAddr)
        jobj.put("address", address)
        jobj.put("index", index)
        jobj.put("count", count)
        jobj.put("direction", direction)
        jobj.put("type", type)
        val rawdata = JSONObject()
        rawdata.put("payload", jobj)
        rawdata.put("method", "NFT_QueryTxsByAddr")

        return goCall {
            GoWallet.checkSessionID()
            nftService.getNFTTran(
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