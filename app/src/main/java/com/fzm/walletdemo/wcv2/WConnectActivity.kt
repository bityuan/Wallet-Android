package com.fzm.walletdemo.wcv2

import android.os.Bundle
import androidx.lifecycle.Observer
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
import com.walletconnect.sign.client.Sign
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
        initObserver()
        initData()

    }

    override fun initData() {
        super.initData()
        initWCV2()
    }

    override fun initObserver() {
        super.initObserver()
        InitWCV2.signModel.observe(this, Observer { walletModel ->
            when (walletModel) {
                is Wallet.Model.SessionProposal -> {
                    logDebug("activity - SessionProposal ")
                    gotoSessionProposal(walletModel)
                }

                is Wallet.Model.SessionRequest -> {
                    logDebug("activity - SessionRequest ")
                    gotoSessionRequest(walletModel)
                }

                is Wallet.Model.SettledSessionResponse -> {
                    logDebug("activity - SessionRequest ")
                    gotoSettleResponse(walletModel)
                }
            }
        })
    }


    //---------------------------------WCV2-----------------------------------

    private fun initWCV2() {
        //projectId 每天都连接限制，到达限制次数后就超时
        wcUrl?.let { wurl ->
            //InitWCV2
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
        bundle.putParcelable("SessionRequestData", data)
        navController.navigate(R.id.action_to_sessionRequest, bundle)
    }


    private fun gotoSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        when (settleSessionResponse) {
            is Wallet.Model.SettledSessionResponse.Result -> {
                val bundle = Bundle()
                bundle.putString("url", settleSessionResponse.session.metaData?.url)
                bundle.putString("name", settleSessionResponse.session.metaData?.name)
                bundle.putString("sessionTopic", settleSessionResponse.session.topic)
                var account = ""
                settleSessionResponse.session.namespaces["eip155"]?.let {
                    account = it.accounts[0]
                }

                val acccountSplit = account.split(":")
                val cChain = "${acccountSplit[0]}:${acccountSplit[1]}"
                val chainName = GoWallet.getChainName(cChain)
                bundle.putString("address", acccountSplit[2])
                bundle.putString("chooseChain", chainName)
                navController.navigate(R.id.action_to_sessionProposaled, bundle)


            }

            is Wallet.Model.SettledSessionResponse.Error -> Wallet.Model.SettledSessionResponse.Error(
                "SettleResponse 连接失败"
            )
        }


    }

}
