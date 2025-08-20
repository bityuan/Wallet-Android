package com.fzm.wallet.sdk.repo

import com.fzm.wallet.sdk.api.Apis
import com.fzm.wallet.sdk.bean.*
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.apiCall
import com.fzm.wallet.sdk.net.brc20Call
import com.fzm.wallet.sdk.net.dnsCall
import com.fzm.wallet.sdk.net.goCall
import com.fzm.wallet.sdk.net.goStrCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigInteger

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

    suspend fun sendTransaction(signHash: String?): HttpResult<String> {
        val param = JSONObject()
        val data = JSONObject()
        data.put("data", signHash)
        param.put("id", 1)
        param.put("method", "Chain33.SendTransaction")
        param.put("params", JSONArray(listOf(data)))

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goStrCall { apis.sendTransaction(requestBody) }
    }
    suspend fun sendTransactionTest(signHash: String?): HttpResult<String> {
        val param = JSONObject()
        val data = JSONObject()
        data.put("data", signHash)
        param.put("id", 1)
        param.put("method", "Chain33.SendTransaction")
        param.put("params", JSONArray(listOf(data)))

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goStrCall { apis.sendTransactionTest(requestBody) }
    }
    suspend fun createBindMiner(amount: Long?,bindAddr:String,originAddr:String): HttpResult<CreateBindMiner> {
        val param = JSONObject()
        val data = JSONObject()
        data.put("amount", amount)
        data.put("bindAddr", bindAddr)
        data.put("checkBalance", false)
        data.put("originAddr", originAddr)
        param.put("id", 1)
        param.put("method", "ticket.CreateBindMiner")
        param.put("params", JSONArray(listOf(data)))

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goStrCall { apis.createBindMiner(requestBody) }
    }
    suspend fun chain33CreateRaw(amount: BigInteger?): HttpResult<String> {
        val param = JSONObject()
        val data = JSONObject()
        data.put("to", "16htvcBNSEA7fZhAdLJphDwQRQJaHpyHTp")
        data.put("amount", amount)
        data.put("fee", 100000)
        data.put("note", "")
        data.put("isToken", false)
        data.put("isWithdraw", true)
        data.put("tokenSymbol", "")
        data.put("execName", "")
        data.put("exec", "coins")

        param.put("id", 1)
        param.put("method", "Chain33.CreateRawTransaction")
        param.put("params", JSONArray(listOf(data)))

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goStrCall { apis.chain33CreateRaw(requestBody) }
    }
    suspend fun chain33Balance(address: String): HttpResult<List<TicketBalance>> {
        val param = JSONObject()
        val data = JSONObject()
        data.put("addresses", JSONArray(listOf(address)))
        data.put("execer", "ticket")

        param.put("id", 1)
        param.put("method", "Chain33.GetBalance")
        param.put("params", JSONArray(listOf(data)))

        val requestBody =
            param.toString().toRequestBody("application/json".toMediaTypeOrNull())

        return goStrCall { apis.chain33Balance(requestBody) }
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


    //铭文
    suspend fun getBrc20Balance(address: String): HttpResult<Brc20Balances> {
        return brc20Call { apis.getBrc20Balance(address) }
    }

    suspend fun getBrc20Tran(address: String, name: String): List<Brc20Tran> {
        return apis.getBrc20Tran(address, name)
    }

    suspend fun genBtcWitNessAddr(pubkey: String): String {
        return apis.genBtcWitNessAddr(pubkey)
    }


    suspend fun transferAble(address: String,name: String): HttpResult<TransferAbles> {
        return brc20Call { apis.transferAble(address,name) }

    }

    //---------------------------铭刻-----------------------------
    suspend fun inscribeTransfer(address: String,name: String): String {
        return apis.inscribeTransfer(address,name)
    }

    suspend fun inscriptionTransfer(
        signer: String,
        ticker: String,
        amount: Int,
        raw_utxo: String,
        data: String,
        test: Boolean
    ): InsTransfer2 {


        return apis.inscriptionTransfer(
            toRequestBody(
                "signer" to signer,
                "ticker" to ticker,
                "amount" to amount,
                "raw_utxo" to raw_utxo,
                "data" to data,
                "test" to test,
            )
        )
    }

    suspend fun transfer(rawtx: String): List<String> {
        return apis.transfer(
            toRequestBody(
                "rawtx" to rawtx
            )
        )
    }


    //--------------------------转账--------------------------------
    suspend fun insTransfer(
        address: String?,
        name: String?,
        inscriptionId: String?,
    ): String {
        return apis.insTransfer(address, name, inscriptionId)
    }

    //{
    //    "signer": "tb1qs49cddy0zzkzc0jwwwaek5tap6guw3yhp4hf7s",
    //    "ticker": "ordi",
    //    "receive":"目的地址"，
    //    "raw_utxo": "7b2274786964223a2262623064653161323062383465633065653264663032383036386436663463623561353361313564373530353764356666366134386662376161666436353439222c22766f7574223a302c22616d6f756e74223a302e3030352c227363726970745075624b6579223a223030313438353462383662343866313061633263336534653733626239623531376430653931633734343937227d",
    //    "data": "privkey",
    //    "test": true
    //}
    suspend fun insTransferPost(
        signer: String?,
        ticker: String?,
        receive: String?,
        raw_utxo: String?,
        data: String?,
        test: Boolean
    ): InsTransfer2 {
        return apis.insTransferPost(
            toRequestBody(
                "signer" to signer,
                "ticker" to ticker,
                "receive" to receive,
                "raw_utxo" to raw_utxo,
                "data" to data,
                "test" to test,
            )
        )
    }

}