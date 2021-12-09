package com.fzm.walletmodule.api

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.ui.base.BaseWebActivity
import com.fzm.walletmodule.utils.GoUtils
import com.google.gson.Gson
import org.litepal.LitePal.select
import wendu.dsbridge.CompletionHandler
import java.util.HashMap

/**
 *@author zx
 *@since 2021/12/9
 */
class AndroidWebBridge(var webView: WebView, var baseWebActivity: BaseWebActivity) {

    var context by Weak {
        baseWebActivity
    }


    @JavascriptInterface
    fun getCurrentBTYAddress(msg: Any, handler: CompletionHandler<String>) {
        val coin: Coin? = GoUtils.getBTY()
        if (coin == null) {
            context?.runOnUiThread {
                Toast.makeText(context, "BTY地址获取失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            handler.complete(coin.getAddress().toString())
        }
    }

    @JavascriptInterface
    fun closeCurrentWebview(msg: Any, handler: CompletionHandler<String>) {
        context?.runOnUiThread {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                context?.finish()
            }
        }

    }

    @JavascriptInterface
    fun sign(msg: Any, handler: CompletionHandler<String>) {
        context?.runOnUiThread { context?.handleSign(msg, handler) }
    }

    @JavascriptInterface
    fun signTxGroup(msg: Any, handler: CompletionHandler<String>) {
        context?.runOnUiThread { context?.handleSignTxGroup(msg, handler) }
    }

    @JavascriptInterface
    fun configPriv(msg: Any, handler: CompletionHandler<String>) {
        context?.runOnUiThread { context?.handleConfigPri(msg, handler) }
    }

    @JavascriptInterface
    fun browserOpen(msg: Any, handler: CompletionHandler<String>) {
        context?.browserOpen(msg, handler)
    }

    //----------------------------------跨连桥-----------------------------------

    @JavascriptInterface
    fun getAddress(msg: Any, handler: CompletionHandler<String>) {
        context?.getAddress(msg, handler)
    }

    //bty 主网 - exchange
    @JavascriptInterface
    fun mainToExchange(msg: Any, handler: CompletionHandler<String>) {
        context!!.runOnUiThread { context?.mainToExchange(msg, handler) }
    }

    //bty exchange - 主网
    @JavascriptInterface
    fun exchangeToMain(msg: Any, handler: CompletionHandler<String>) {
        context!!.runOnUiThread { context?.exchangeToMain(msg, handler) }
    }

    //eth eth - dex ethLock
    @JavascriptInterface
    fun ethLock(msg: Any, handler: CompletionHandler<String>) {
        context!!.runOnUiThread { context?.ethLock(msg, handler) }
    }

    //eth dex - eth ethUnlock
    @JavascriptInterface
    fun ethUnlock(msg: Any, handler: CompletionHandler<String>) {
        context!!.runOnUiThread { context?.ethUnlock(msg, handler) }
    }

    @JavascriptInterface
    fun evmToExchange(msg: Any, handler: CompletionHandler<String>) {
        context!!.runOnUiThread { context?.evmToExchange(msg, handler) }
    }

    @JavascriptInterface
    fun exchangeToEvm(msg: Any, handler: CompletionHandler<String>) {
        context!!.runOnUiThread { context?.exchangeToEvm(msg, handler) }
    }

}