package com.fzm.walletdemo.ui

import android.net.wifi.WifiInfo
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.databinding.DialogLoadingBinding
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.ext.jsonToMap
import com.fzm.wallet.sdk.ext.toJSONStr
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.ToolUtils
import com.fzm.walletmodule.utils.NetWorkUtils
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.Walletapi
import wendu.dsbridge.CompletionHandler
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.Charset

class JsApi(private val webView: WebView, private val activity: FragmentActivity) {
    private var cointype = ""
    private var createHash = ""
    private var exer = ""
    private var withhold = -1

    //addressID 比特格式的地址传0， 以太坊格式传2
    private var addressid = -1

    private val loading by lazy {
        val loadingBinding = DialogLoadingBinding.inflate(activity.layoutInflater)
        return@lazy AlertDialog.Builder(activity).setView(loadingBinding.root).create().apply {
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }


    @JavascriptInterface
    fun getCurrentBTYAddress(msg: Any, handler: CompletionHandler<String?>) {
        val chain = GoWallet.getChain("BTY")
        handler.complete(chain?.address)
    }

    @JavascriptInterface
    fun getAddress(msg: Any, handler: CompletionHandler<String?>) {
        val map = msg.toString().jsonToMap<String>()
        val chain = map["cointype"]
        chain?.let {
            val chain = GoWallet.getChain(it)
            handler.complete(toJSONStr("address" to chain?.address))
        }

    }

    @JavascriptInterface
    fun getDeviceId(msg: Any, handler: CompletionHandler<String?>) {
        handler.complete(ToolUtils.getMyUUID(activity))
    }

    private fun isHaved(list: List<ArrayMap<String, String>>, ip: String): Boolean {
        for (arrayMap in list) {
            if (arrayMap["ip"] == ip) {
                return true
            }
        }
        return false
    }

    @JavascriptInterface
    fun getDeviceList(msg: Any, handler: CompletionHandler<String?>) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            /*在这里同样使用约定好的端口*/
            val port = 8804
            var server: DatagramSocket? = null
            try {
                server = DatagramSocket(port)
                val packet = DatagramPacket(buffer, buffer.size)
                val list = mutableListOf<ArrayMap<String, String>>()
                while (true) {
                    server.receive(packet)
                    val data =
                        String(packet.data, 0, packet.length, Charset.defaultCharset())
                    val ip = packet.address.toString()

                    if (ip.isNotEmpty()) {
                        val map = arrayMapOf<String, String>()
                        map["ip"] = ip.substring(1)
                        map["serial"] = data

                        if (isHaved(list, ip.substring(1))) {
                            val json = Gson().toJson(list)
                            withContext(Dispatchers.Main) {
                                Log.v("dao", json)
                                handler.complete(json)
                            }

                            break
                        } else {
                            list.add(map)
                        }

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                server?.close()
            }
        }

    }

    @JavascriptInterface
    fun closeCurrentWebview(msg: Any?, handler: CompletionHandler<String?>?) {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            activity.finish()
        }
    }

    /*   @JavascriptInterface
       fun setTitle(msg: Any, handler: CompletionHandler<String?>?) {
           //val map = msg.toString().jsonToMap<String>()
           val jsTitle = Gson().fromJson(msg.toString(), JsTitle::class.java)
           val title = jsTitle.title
           binding.xbar.tvToolbar.text = title
           handler?.complete()
       }*/

    @JavascriptInterface
    fun getCurrentWifi(msg: Any?, handler: CompletionHandler<String?>) {
        val wifiInfo: WifiInfo = NetWorkUtils.getWifi(activity)
        handler.complete(toJSONStr("name" to wifiInfo.ssid))
    }


    @JavascriptInterface
    fun sign(msg: Any?, handler: CompletionHandler<String?>?) {
        val jsSign = Gson().fromJson(msg.toString(), JsSign::class.java)
        cointype = jsSign.cointype
        createHash = jsSign.createHash
        exer = jsSign.exer
        withhold = jsSign.withhold
        addressid = getAddressId()
        showPwdDialog(1, handler)
    }

    inner class JsTitle {
        var title = ""
    }

    inner class JsSign {
        var cointype = ""
        var createHash = ""
        var exer = ""
        var withhold = -1
    }

    @JavascriptInterface
    fun signTxGroup(msg: Any?, handler: CompletionHandler<String?>?) {
        val jsSign = Gson().fromJson(msg.toString(), JsSign::class.java)
        cointype = jsSign.cointype
        createHash = jsSign.createHash
        exer = jsSign.exer
        withhold = jsSign.withhold
        addressid = getAddressId()
        showPwdDialog(2, handler)
    }

    private fun getAddressId(): Int {
        return when (cointype) {
            "BTY" -> {
                0
            }
            "YCC" -> {
                2
            }
            else -> 0
        }
    }


    @JavascriptInterface
    fun importSeed(msg: Any?, handler: CompletionHandler<String?>?) {
        showPwdDialog(3, handler)
    }

    @JavascriptInterface
    fun scanQRCode(msg: Any?, handler: CompletionHandler<String?>?) {
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(activity, Observer { scan ->
            handler?.complete(scan)
        })
        ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
    }

    private fun getPrikey(wallet: PWallet, password: String, cointype: String): String {
        val bPassword = GoWallet.encPasswd(password)!!
        val priKey: String = when (wallet.type) {
            2 -> {
                var thisCointype = ""
                val mnem: String = GoWallet.decMenm(bPassword, wallet.mnem)
                if (cointype == "YCC") {
                    thisCointype = Walletapi.TypeETHString
                } else {
                    thisCointype = Walletapi.TypeBtyString
                }

                val priKey = GoWallet.getPrikey(thisCointype, mnem)
                priKey
            }
            //私钥
            4 -> {
                val priKey = LitePal.find<PWallet>(
                    wallet.id,
                    true
                ).coinList[0].getPrivkey(password)
                priKey

            }
            else -> {
                ""
            }
        }
        return priKey
    }

    private fun showPwdDialog(from: Int, handler: CompletionHandler<String?>?) {
        val bindingDialog = DialogPwdBinding.inflate(activity.layoutInflater)
        val dialog =
            AlertDialog.Builder(activity).setView(bindingDialog.root).create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }
        bindingDialog.ivClose.setOnClickListener {
            dialog.dismiss()
            handler?.complete(toJSONStr("error" to "取消"))
        }
        bindingDialog.btnOk.setOnClickListener {
            val password = bindingDialog.etInput.text.toString()
            if (password.isEmpty()) {
                activity.toast("请输入密码")
                return@setOnClickListener
            }
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val wallet = LitePal.find<PWallet>(MyWallet.getId())
                wallet?.let {
                    withContext(Dispatchers.Main) {
                        loading.show()
                    }
                    val check = GoWallet.checkPasswd(password, it.password)
                    if (!check) {
                        withContext(Dispatchers.Main) {
                            activity.toast("密码错误")
                            loading.dismiss()
                        }
                        return@let
                    }
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        if (!loading.isShowing) {
                            loading.show()
                        }

                        when (from) {
                            1 -> {
                                val priKey = getPrikey(it, password, cointype)
                                val signTx = GoWallet.signTran(
                                    cointype,
                                    Walletapi.hexTobyte(createHash),
                                    priKey,
                                    addressid
                                )
                                handler?.complete(toJSONStr("signHash" to signTx))
                                loading.dismiss()

                            }
                            2 -> {
                                val priKey = getPrikey(it, password, cointype)
                                val signTx = GoWallet.signTxGroup(
                                    exer,
                                    createHash,
                                    priKey,
                                    priKey,
                                    0.03,
                                    addressid
                                )
                                handler?.complete(toJSONStr("signHash" to signTx))
                                loading.dismiss()

                            }
                            3 -> {
                                val bPassword = GoWallet.encPasswd(password)!!
                                val mnem: String = GoWallet.decMenm(bPassword, it.mnem)
                                handler?.complete(
                                    toJSONStr(
                                        "passwd" to password,
                                        "seed" to mnem
                                    )
                                )
                                loading.dismiss()
                            }
                            else -> {}
                        }

                    }


                }

            }
        }
    }

}