package com.fzm.walletdemo.wcv2

import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.IPConfig.Companion.projectId
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_WC_MODEL
import com.fzm.wallet.sdk.base.LIVE_WC_STATUS
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.walletdemo.IApplication
import com.jeremyliao.liveeventbus.LiveEventBus
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import timber.log.Timber

object InitWCV2 {

    init {
        logDebug("===============单例初始化v2================")
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val walletIconUrl = "https://avatars.githubusercontent.com/u/6955922?s=280&v=4"

        val connectionType = ConnectionType.AUTOMATIC
        val appMetadata = Core.Model.AppMetaData(
            name = "Android Wallet Sample",
            description = "Wallet description",
            url = "example.wallet",
            icons = listOf(walletIconUrl),
            redirect = "kotlin-wallet-wc:/request" // optional
        )
        // Initialize Core client
        CoreClient.initialize(
            relayServerUrl = serverUrl,
            connectionType = connectionType,
            application = IApplication.getInstance(),
            metaData = appMetadata,
            onError = { error ->
                Timber.e("1.初始化核心 Error: $error")
            })

        val init = Wallet.Params.Init(CoreClient)
        // Initialize Sign client
        Web3Wallet.initialize(init) { error ->
            Timber.e("2.初始化签名 Error: $error")
        }

        Web3Wallet.setWalletDelegate(object : Web3Wallet.WalletDelegate {
            override fun onAuthRequest(authRequest: Wallet.Model.AuthRequest) {
                logDebug("返回：onAuthRequest = $authRequest")
            }

            override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                logDebug("返回：onConnectionStateChange: $state")
            }

            override fun onError(error: Wallet.Model.Error) {
                logDebug("返回：onError = $error")
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                logDebug("返回：onSessionDelete = $sessionDelete")
                postModel(sessionDelete)
                postToMain(false)

            }

            override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
                logDebug("返回：onSessionProposal = $sessionProposal")
                postModel(sessionProposal)

            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
                logDebug("返回：onSessionRequest = $sessionRequest")
                ARouter.getInstance().build(RouterPath.APP_WCONNECT)
                    .withInt(RouterPath.PARAM_FROM,2)
                    .navigation()
                WCParam.sessionRequest = sessionRequest
                postModel(sessionRequest)
            }

            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                //连上以后会返回新的sessiontopic ，断开连接要用新的
                logDebug("返回：onSessionSettleResponse = $settleSessionResponse")
                postToMain(true)
                postModel(settleSessionResponse)

            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                logDebug("返回：onSessionUpdateResponse = $sessionUpdateResponse")

            }

        })
    }


    private fun postToMain(state:Boolean){
        LiveEventBus.get<Boolean>(LIVE_WC_STATUS).post(state)
    }

    private fun postModel(model:Wallet.Model?){
        LiveEventBus.get<Wallet.Model?>(LIVE_WC_MODEL).post(model)
    }

}