package com.fzm.walletdemo.wconnect

import com.google.gson.annotations.SerializedName
import org.web3j.utils.Numeric

//发送消息类型
enum class MessageType {
    @SerializedName("pub")
    PUB,
    @SerializedName("sub")
    SUB
}

//返回消息类型
enum class WCMethod {
    @SerializedName("wc_sessionRequest")
    SESSION_REQUEST,

    @SerializedName("wc_sessionUpdate")
    SESSION_UPDATE,

    @SerializedName("eth_sign")
    ETH_SIGN,

    @SerializedName("personal_sign")
    ETH_PERSONAL_SIGN,

    @SerializedName("eth_signTypedData")
    ETH_SIGN_TYPE_DATA,

    @SerializedName("eth_signTransaction")
    ETH_SIGN_TRANSACTION,

    @SerializedName("eth_sendTransaction")
    ETH_SEND_TRANSACTION,

    @SerializedName("get_accounts")
    GET_ACCOUNTS,
}


//-------------------------------------------Message------------------------------------------



data class WCEthereumSignMessage(
    val raw: List<String>,
    val type: WCSignType
) {
    enum class WCSignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    val data
        get() = when (type) {
            WCSignType.MESSAGE -> raw[1]
            WCSignType.TYPED_MESSAGE -> raw[1]
            WCSignType.PERSONAL_MESSAGE -> raw[0]
        }
}

data class WCEthereumTransaction(
    val from: String,
    val to: String,
    val nonce: String?,
    val gas: String,
    val gasPrice: String?,
    val gasLimit: String?,
    val value: String?,
    val data: String
) {
    override fun toString(): String {
        return "WCEthereumTransaction(from='$from', to=$to, nonce=$nonce, gasPrice=$gasPrice, gasLimit=$gasLimit, value=$value, data='$data')"
    }
}

data class CreateTran(
    val from: String,
    val gas: Long,
    val gasPrice: Long,
    val input: String,
    val nonce: Long,
    val to: String,
    val value: Long
)

//---------------------------------------Exception------------------------------------------

class InvalidHmacException : Exception("Invalid HMAC")
class InvalidJsonRpcParamsException(val requestId: Long) : Exception("Invalid JSON RPC Request")
class InvalidSessionException : Exception("Invalid session")
class InvalidPayloadException : Exception("Invalid WCEncryptionPayload")


//-----------------------------------------ext fun--------------------------------------------------

fun ByteArray.toHexString(): String {
    return Numeric.toHexString(this, 0, this.size, false)
}

fun String.toByteArray(): ByteArray {
    return Numeric.hexStringToByteArray(this)
}

//-------------------------------------------request------------------------------------


private const val JSONRPC_VERSION = "2.0"

data class JsonRpcRequest<T>(
    val id: Long,
    val jsonrpc: String = JSONRPC_VERSION,
    val method: WCMethod?,
    val params: T
)

data class JsonRpcResponse<T>(
    val jsonrpc: String = JSONRPC_VERSION,
    val id: Long,
    val result: T
)

data class JsonRpcErrorResponse(
    val jsonrpc: String = JSONRPC_VERSION,
    val id: Long,
    val error: JsonRpcError
)

data class JsonRpcError(
    val code: Int,
    val message: String
) {
    companion object {
        fun serverError(message: String) = JsonRpcError(-32000, message)
        fun invalidParams(message: String) = JsonRpcError(-32602, message)
        fun invalidRequest(message: String) = JsonRpcError(-32600, message)
        fun parseError(message: String) = JsonRpcError(-32700, message)
        fun methodNotFound(message: String) = JsonRpcError(-32601, message)
    }
}

//---------------------------------session-------------------------------------

data class WCSessionRequest(
    val peerId: String,
    val peerMeta: WCPeerMeta,
    val chainId: String?
)

data class WCApproveSessionResponse(
    val approved: Boolean = true,
    val chainId: Int,
    val accounts: List<String>,
    val peerId: String?,
    val peerMeta: WCPeerMeta?
)

data class WCSessionUpdate(
    val approved: Boolean,
    val chainId: Int?,
    val accounts: List<String>?
)

data class WCEncryptionPayload(
    val data: String,
    val hmac: String,
    val iv: String
)

data class WCSocketMessage(
    val topic: String,
    val type: MessageType,
    val payload: String
)

data class WCPeerMeta (
    val name: String,
    val url: String,
    val description: String? = null,
    val icons: List<String> = listOf("")
)
