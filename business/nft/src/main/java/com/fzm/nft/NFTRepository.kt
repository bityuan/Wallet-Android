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
        val payload = JSONObject()
        payload.put("contractAddr", contractAddr)
        payload.put("address", address)
        payload.put("index", index)
        payload.put("count", count)
        payload.put("direction", direction)
        payload.put("type", type)
        val rawdata = JSONObject()
        rawdata.put("payload", payload)
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

    suspend fun outNFT(
        cointype: String = Walletapi.TypeETHString,
        tokenId: String,
        contractAddr: String,
        from: String,
        to: String,
        fee: Double
    ): HttpResult<String> {
        val payload = JSONObject()
        payload.put("tokenId", tokenId)
        payload.put("contractAddr", contractAddr)
        payload.put("from", from)
        payload.put("to", to)
        payload.put("fee", fee)
        val rawdata = JSONObject()
        rawdata.put("payload", payload)
        rawdata.put("method", "NFT_TransferFrom")

        return goCall {
            GoWallet.checkSessionID()
            nftService.outNFT(
                GoWallet.sessionID,
                toRequestBody(
                    "Wallet.Transport",
                    "cointype" to cointype,
                    "rawdata" to rawdata
                )
            )
        }
    }

    suspend fun getNFTList(
        cointype: String = Walletapi.TypeETHString,
        contractAddr: String,
        from: String,
    ): HttpResult<List<String>> {
        val payload = JSONObject()
        payload.put("contractAddr", contractAddr)
        payload.put("from", from)
        val rawdata = JSONObject()
        rawdata.put("payload", payload)
        rawdata.put("method", "NFT_TokenIdList")

        return goCall {
            GoWallet.checkSessionID()
            nftService.getNFTList(
                GoWallet.sessionID,
                toRequestBody(
                    "Wallet.Transport",
                    "cointype" to cointype,
                    "rawdata" to rawdata
                )
            )
        }
    }


}