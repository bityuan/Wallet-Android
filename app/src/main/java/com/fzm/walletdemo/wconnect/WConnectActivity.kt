package com.fzm.walletdemo.wconnect

import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityWconnectBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import walletapi.Walletapi
import java.util.*
import java.util.concurrent.TimeUnit

@Route(path = RouterPath.APP_WCONNECT)
class WConnectActivity : BaseActivity() {

    private val binding by lazy { ActivityWconnectBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired(name = RouterPath.PARAM_WC_URL)
    var wcUrl: String? = null

    //当前地址
    private val accounts = mutableListOf<String>()
    private lateinit var topic: String
    private lateinit var userInfo: String
    private lateinit var key: String
    private lateinit var randomPeerId: String
    private var myPriv = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initData()

    }

    override fun initData() {
        super.initData()
        wcUrl?.let {
            val bnbAddress = GoWallet.getChain("BNB")?.address
            bnbAddress?.let { addr ->
                accounts.add(addr)
            }
            randomPeerId = UUID.randomUUID().toString()


            val uriString = it.replace("wc:", "wc://")
            val uri = Uri.parse(uriString)
            val bridge = uri.getQueryParameter("bridge")
            key = uri.getQueryParameter("key")!!
            topic = uri.userInfo!!
            userInfo = uri.userInfo!!
            val version = uri.host
            logDebug("uri = $uri,\nbridge = $bridge,\nkey = $key, \ntopic = $topic, \nversion = $version")
            initWebSocket(bridge!!)
        }

    }


    //---------------------------------------websocket-------------------------------------

    private lateinit var webSocket: WebSocket


    private fun initWebSocket(url: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .writeTimeout(7, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()


        val request: Request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                //连接成功
                updateStatus("onOpen！", Color.GREEN)
                //连接上以后发送topic到socket，然后才会有消息返回
                // The Session.topic channel is used to listen session request messages only.
                //Session.topic通道仅用于侦听会话请求消息。
                sendTopic(topic)
                // The peerId channel is used to listen to all messages sent to this httpClient.
                //peerId通道用于侦听发送到此httpClient的所有消息。
                sendTopic(randomPeerId)

            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                logDebug("收到消息= $text")
                val deResult = decryptMessage(text)
                logDebug("解密后 === $deResult")

                val jsonResult = JSONObject(deResult)
                val id = jsonResult.getLong("id")
                val method = jsonResult.getString("method")
                val array = jsonResult.getJSONArray("params")

                //最外层解析为对象
                val request = gson.fromJson<JsonRpcRequest<JsonArray>>(
                    deResult,
                    typeToken<JsonRpcRequest<JsonArray>>()
                )

                logDebug("method === $method")
                when (method) {
                    "wc_sessionRequest" -> {
                        val params = JSONObject(array[0].toString())
                        val peerId = params.getString("peerId")
                        val peerMeta = params.getJSONObject("peerMeta")
                        val wcPeerMeta = gson.fromJson<WCPeerMeta>(peerMeta.toString())
                        val chainId = params.getInt("chainId")

                        //替换为新的
                        topic = peerId
                        lifecycleScope.launch(Dispatchers.Main) {
                            val builder = AlertDialog.Builder(this@WConnectActivity)
                            val dialog: AlertDialog = builder
                                .setTitle("名字")
                                .setMessage("链接")
                                .setPositiveButton(R.string.dialog_approve) { d, w ->
                                    approveSession(id, chainId, wcPeerMeta)
                                }
                                .setNegativeButton(R.string.dialog_reject) { d, w ->
                                    finish()
                                }
                                .setCancelable(false)
                                .create()
                            dialog.show()
                        }
                    }
                    "eth_sign" -> {
                        val params = gson.fromJson<List<String>>(request.params)
                        if (params.size < 2) throw InvalidJsonRpcParamsException(request.id)
                        val msg =
                            WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.MESSAGE)
                    }
                    "personal_sign" -> {
                        val params = gson.fromJson<List<String>>(request.params)
                        if (params.size < 2) throw InvalidJsonRpcParamsException(request.id)
                        val msg = WCEthereumSignMessage(
                            params,
                            WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE
                        )
                    }
                    "eth_signTypedData" -> {
                        val params = gson.fromJson<List<String>>(request.params)
                        if (params.size < 2) throw InvalidJsonRpcParamsException(request.id)
                        val sign = WCEthereumSignMessage(
                            params,
                            WCEthereumSignMessage.WCSignType.TYPED_MESSAGE
                        )
                        //签名
                        logDebug("签名前： ${sign.data}")
                        val signed = Walletapi.signTypedMessage(sign.data, myPriv)
                        logDebug("SDK签名后的数据： $signed")
                        approveRequest(id, signed)
                    }
                    "eth_signTransaction" -> {
                        val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    }
                    "eth_sendTransaction" -> {
                        //{
                        //	"id": "16872316684976",
                        //	"jsonrpc": "2.0",
                        //	"method": "eth_sendTransaction",
                        //	"params": [{
                        //		"data": "",
                        //		"from": "",
                        //		"gas": "0x2df14",
                        //		"to": "",
                        //		"value": "0x71afd498d0000"
                        //	}]
                        //}


                        try {
                            val params = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                            val param = params[0]
                            val dGas = param.gas.substringAfter("0x")
                            val gas = dGas.toLong(16)
                            var value:Long = 0
                            param.value?.let {
                                val dValue = it.substringAfter("0x")
                                value = dValue.toLong(16)
                            }


                            val web3j = Web3j.build(HttpService("https://bsc.publicnode.com"));
                            val nonceResult = web3j.ethGetTransactionCount(
                                param.from,
                                DefaultBlockParameterName.PENDING
                            ).send()
                            val nonce = nonceResult.transactionCount.toLong()

                            val gasPriceResult = web3j.ethGasPrice().send();
                            val gasPrice = gasPriceResult.gasPrice.toLong()

                            logDebug("nonce = $nonce   gasPrice = $gasPrice")

                            val input = param.data.substringAfter("0x")
                            val input64 = Base64.encodeToString(input.toByteArray(), Base64.DEFAULT)

                            val createTran = CreateTran(
                                param.from,
                                gas,
                                gasPrice,
                                input64,
                                nonce,
                                param.to,
                                value
                            )
                            val createJson = gson.toJson(createTran)
                            logDebug("构造数据 == $createJson")

                            val bCreate = Walletapi.stringTobyte(createJson)
                            val signed =
                                GoWallet.signTran("BNB", bCreate, myPriv, 2)
                            logDebug("签名数据： $signed")
                            val sendHash = GoWallet.sendTran("BNB", signed!!, "")
                            logDebug("发送Hash： $sendHash")
                            val sendJson = JSONObject(sendHash)
                            val sendResult = sendJson.getString("result")
                            approveRequest(id, sendResult)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("error", e.toString())
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                //如果服务器传递的是byte类型的
                val msg = bytes.utf8()
                logDebug("byte =  $msg")
                updateStatus("byte类型的: $msg", Color.WHITE)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                //连接失败调用 异常信息
                Log.e("error", "${t.message}")
                updateStatus("${t.message}", Color.RED)
            }
        })

        //内存不足时释放
        client.dispatcher().executorService().shutdown()
    }


    private fun updateStatus(msg: String, color: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            //binding.tvStatus.text = msg
            //binding.tvStatus.setTextColor(color)
        }
    }


    //---------------------------------对消息处理-----------------------------------

    private val gson = GsonBuilder()
        .serializeNulls()
        .create()

    //初次连接发送消息验证
    private fun sendTopic(topic: String): Boolean {
        val message = WCSocketMessage(
            topic = topic,
            type = MessageType.SUB,
            payload = ""
        )
        val msg = gson.toJson(message)
        logDebug("sendTopic消息 $msg")
        return webSocket.send(msg)
    }

    //解密消息
    private fun decryptMessage(text: String): String {
        val message = gson.fromJson<WCSocketMessage>(text)
        val encrypted = gson.fromJson<WCEncryptionPayload>(message.payload)
        return String(WCCipher.decrypt(encrypted, key), Charsets.UTF_8)
    }


    private fun approveSession(id: Long, chainId: Int, wcPeerMeta: WCPeerMeta) {
        val result = WCApproveSessionResponse(
            chainId = chainId,
            accounts = accounts,
            peerId = randomPeerId,
            peerMeta = wcPeerMeta
        )
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        encryptAndSend(gson.toJson(response))
    }

    private fun approveRequest(id: Long, result: String): Boolean {
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        return encryptAndSend(gson.toJson(response))
    }

    private fun encryptAndSend(result: String): Boolean {
        logDebug("==> 包装数据： $result");
        val payload =
            gson.toJson(WCCipher.encrypt(result.toByteArray(Charsets.UTF_8), key))
        val message = WCSocketMessage(
            topic = topic,
            type = MessageType.PUB,
            payload = payload
        )
        val json = gson.toJson(message)
        logDebug("==> 同意连接发送数据： $json")
        return webSocket.send(json)
    }


}
