package com.fzm.walletdemo.ui

import android.text.TextUtils
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import com.fzm.walletdemo.web3.bean.Address
import com.fzm.walletdemo.web3.bean.Web3Call
import com.fzm.walletdemo.web3.bean.Web3Transaction
import com.fzm.walletdemo.web3.listener.JsListener
import com.fzm.walletdemo.web3.util.Hex
import org.json.JSONObject
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import timber.log.Timber
import java.math.BigInteger

class JsWCApi(
    private val webView: WebView,
    private val activity: FragmentActivity,
    private val jsListener: JsListener
) {


    @JavascriptInterface
    fun signTransaction(
        callbackId: Int,
        recipient: String?,
        value: String?,
        nonce: String?,
        gasLimit: String?,
        gasPrice: String?,
        payload: String?
    ) {
        try {
            val dValue = if (value == null || value == "undefined") {
                "0"
            } else {
                value
            }

            val dGasPrice = gasPrice ?: "0"
            val transaction = Web3Transaction(
                if (TextUtils.isEmpty(recipient)) Address.EMPTY else Address(recipient!!),
                null,
                Hex.hexToBigInteger(dValue),
                Hex.hexToBigInteger(dGasPrice, BigInteger.ZERO),
                Hex.hexToBigInteger(gasLimit, BigInteger.ZERO),
                Hex.hexToLong(nonce, -1),
                payload,
                callbackId.toLong()
            )

            webView.post {
                jsListener.onSignTransaction(transaction)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    @JavascriptInterface
    fun signMessage(callbackId: Int, data: String?) {
        Timber.tag("edao").v("signMessage")
    }

    @JavascriptInterface
    fun signPersonalMessage(callbackId: Int, data: String?) {
        Timber.tag("edao").v("signPersonalMessage")
    }


    @JavascriptInterface
    fun requestAccounts(callbackId: Long) {
        webView.post {
            jsListener.onRequestAccounts(callbackId)
        }
    }

    @JavascriptInterface
    fun signTypedMessage(callbackId: Int, data: String?) {
        webView.post {
        }
    }

    @JavascriptInterface
    fun ethCall(callbackId: Int, recipient: String) {
        try {
            val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
            val json = JSONObject(recipient)
            val defaultBlockParameter: DefaultBlockParameter
            val to = if (json.has("to")) json.getString("to") else ZERO_ADDRESS
            val payload = if (json.has("data")) json.getString("data") else "0x"
            val value = if (json.has("value")) json.getString("value") else null
            val gasLimit = if (json.has("gas")) json.getString("gas") else null
            defaultBlockParameter =
                DefaultBlockParameterName.LATEST //TODO: Take block param from query if present
            val call = Web3Call(
                Address(to),
                defaultBlockParameter,
                payload,
                value,
                gasLimit,
                callbackId.toLong()
            )
            jsListener.onEthCall(call)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun walletAddEthereumChain(callbackId: Int, msgParams: String?) {
        //切换网络回调
        Timber.tag("edao").v("======================walletAddEthereumChain")
    }

    @JavascriptInterface
    fun walletSwitchEthereumChain(callbackId: Int, msgParams: String?) {
        Timber.tag("edao").v("----------------------------walletSwitchEthereumChain")
    }


}