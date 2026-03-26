package com.fzm.wallet.sdk.bean

import android.util.Log
import com.fzm.wallet.sdk.BuildConfig
import com.fzm.wallet.sdk.widget.IMarqueeItem
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger


fun toRequestBody(vararg params: Pair<String, Any?>): RequestBody {
    return RequestBody.create(
        "application/json".toMediaTypeOrNull(),
        toJSONObject(*params).toString()
    )
}

fun toRequestBody(method: String, vararg params: Pair<String, Any?>): RequestBody {
    return RequestBody.create(
        "application/json".toMediaTypeOrNull(),
        toJSONParam(method, *params).toString()
    )
}

fun toJSONObject(vararg params: Pair<String, Any?>): JSONObject {
    val param = JSONObject()
    for (i in params) {
        val value = if (i.second == null) "" else i.second
        param.put(i.first, value)
    }
    return param
}

fun toJSONParam(method: String, vararg params: Pair<String, Any?>): JSONObject {
    val param = JSONObject()
    val array = JSONArray()
    val obj = JSONObject()
    for (i in params) {
        val value = if (i.second == null) "" else i.second
        obj.put(i.first, value)
    }
    array.put(obj)
    param.put("id", 1)
    param.put("method", method)
    param.put("params", array)
    if (BuildConfig.DEBUG) {
        Log.v("param：", param.toString())
    }
    return param
}


data class Notices(
    val list: List<Notice>
)

data class Notice(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val type: Int,
    //1为置顶（不可关闭） 0为普通
    val is_top: Int,
    val create_time: String,
    val update_time: String
) : IMarqueeItem, Serializable {
    companion object {
        const val KEY_ID: String = "key_id"
    }

    override fun marqueeMessage(): kotlin.CharSequence {
        return title
    }

}

//铭文
data class Brc20Balances(
    val detail: List<Brc20Balance>
)

data class Brc20Balance(
    val ticker: String,
    val availableBalance: String,
    val transferableBalance: String,
    var address: String?,
)


data class Brc20Tran(
    val amount: String,
    val availableBalance: String,
    val fee: Int,
    val from: String,
    val height: Int,
    val inscriptionId: String,
    val inscriptionNumber: Int,
    val overallBalance: String,
    val satoshi: Int,
    val ticker: String,
    val blocktime: Long,
    val to: String,
    val transferBalance: String,
    val txid: String,
    val txidx: Int,
    val type: String,
    val valid: Boolean
)


//查询可以转移的铭文
data class TransferAbles(
    val detail: List<TransferAble>,
    val height: Int,
    val start: Int,
    val total: Int
)

data class TransferAble(
    val confirmations: Int,
    val data: Able,
    val inscriptionId: String,
    val inscriptionNumber: Int,
    val satoshi: Int
)

data class Able(
    val amt: String,
    val decimal: String,
    val lim: String,
    val op: String,
    val tick: String
)
data class InsTransfer2(
    val rawtx: String
)
data class CreateBindMiner(
    val txHex: String
)
data class TicketBalance(
    val currency: BigDecimal,
    val balance: BigDecimal,
    val frozen: BigDecimal,
    val addr: String
)
