package com.fzm.walletdemo.wcv2

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.IApplication
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityWconnectBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.google.gson.GsonBuilder
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Route(path = RouterPath.APP_WCONNECT)
class WConnectActivity : BaseActivity() {

    private val binding by lazy { ActivityWconnectBinding.inflate(layoutInflater) }
    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment).navController
    }

    @JvmField
    @Autowired(name = RouterPath.PARAM_WC_URL)
    var wcUrl: String? = null

    private var myPriv = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initData()
        initObserver()

    }

    override fun initData() {
        super.initData()
        initWCV2()
    }

    private fun gotoSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
        val bundle = Bundle()
        bundle.putString("url", sessionProposal.url)
        bundle.putString("name", sessionProposal.name)
        bundle.putString("proposerPublicKey", sessionProposal.proposerPublicKey)
        bundle.putString("sessionTopic", sessionProposal.pairingTopic)
        var chain = ""
        sessionProposal.requiredNamespaces["eip155"]?.let {
            chain = it.chains[0]
        }
        bundle.putString("chain", chain)
        navController.navigate(R.id.action_to_sessionProposal, bundle)
    }


    private fun gotoSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        logDebug("sessionRequest =  $sessionRequest")
        val data = SessionRequestData(
            topic = sessionRequest.topic,
            appIcon = sessionRequest.peerMetaData?.icons?.firstOrNull(),
            appName = sessionRequest.peerMetaData?.name,
            appUri = sessionRequest.peerMetaData?.url,
            requestId = sessionRequest.request.id,
            params = sessionRequest.request.params,
            chain = sessionRequest.chainId,
            method = sessionRequest.request.method
        )
        logDebug("转换后SessionRequestData =  $data")
        val bundle = Bundle()
        bundle.putParcelable("SessionRequestData",data)
        navController.navigate(R.id.action_to_sessionRequest, bundle)
    }


    //---------------------------------WCV2-----------------------------------

    private fun initWCV2() {
        //projectId 每天都连接限制，到达限制次数后就超时
        wcUrl?.let { wurl ->
            val relayUrl = "relay.walletconnect.com"
            val projectId = "3a4cf034cbeb5652038e46fc1efca0c7"
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
                    Timber.e("初始化核心 Error: $error")
                })

            val init = Wallet.Params.Init(CoreClient)
            // Initialize Sign client
            Web3Wallet.initialize(init) { error ->
                Timber.e("初始化签名 Error: $error")
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

                }

                override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
                    logDebug("返回：onSessionProposal = $sessionProposal")
                    //ETH 链
                    //返回：onSessionProposal = SessionProposal(
                    // pairingTopic=180672104c11ab2229f9778f2a91d9d63589510c9e0bb607b687988346426b61,
                    // name=Exchange | PancakeSwap, description=The most popular AMM on BSC by user count! Earn CAKE through yield farming or win it in the Lottery, then stake it in Syrup Pools to earn more tokens! Initial Farm Offerings (new token launch model pioneered by PancakeSwap), NFTs, and more, on a platform you can trust.,
                    // url=https://pancakeswap.finance,
                    // icons=[https://pancakeswap.finance/favicon.ico, https://pancakeswap.finance/logo.png],
                    // requiredNamespaces={
                    // eip155=Proposal(
                    // chains=[eip155:1],
                    // methods=[eth_sendTransaction, personal_sign],
                    // events=[chainChanged, accountsChanged], extensions=null)},
                    // proposerPublicKey=d760c7f51389a01f84fe1bfec02adae66487feec489c637a6fdaf82a3550933b,
                    // relayProtocol=irn,
                    // relayData=null)
                    lifecycleScope.launch(Dispatchers.Main) {
                        gotoSessionProposal(sessionProposal)
                    }

                }

                override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
                    logDebug("返回：onSessionRequest = $sessionRequest")
                    lifecycleScope.launch(Dispatchers.Main) {
                        gotoSessionRequest(sessionRequest)
                    }

                }

                override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                    logDebug("返回：onSessionSettleResponse = $settleSessionResponse")

                }

                override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                    logDebug("返回：onSessionUpdateResponse = $sessionUpdateResponse")

                }

            })
            //连接
            pair(wurl)

        }

    }

    private fun pair(pairingUri: String) {
        Timber.d("Test - pairingUri: $pairingUri")
        val pairingParams = Core.Params.Pair(pairingUri)
        CoreClient.Pairing.pair(pairingParams) { error ->
            Timber.e("Error: ${error.throwable.stackTraceToString()}")
        }
    }

}
