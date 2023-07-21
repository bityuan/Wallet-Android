package com.fzm.walletdemo.wcv2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.walletdemo.IApplication
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import timber.log.Timber

object InitWCV2 {

    private val _signModel = MutableLiveData<Wallet.Model?>()
    val signModel: LiveData<Wallet.Model?>
        get() = _signModel

    init {
        logDebug("===============单例初始化v2================")
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

                /*lifecycleScope.launch(Dispatchers.Main) {
                    gotoSessionProposal(sessionProposal)
                }*/
                _signModel.postValue(sessionProposal)

            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
                logDebug("返回：onSessionRequest = $sessionRequest")
              /*  lifecycleScope.launch(Dispatchers.Main) {
                    gotoSessionRequest(sessionRequest)
                }*/
                _signModel.postValue(sessionRequest)

            }

            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                //连上以后会返回新的sessiontopic ，断开连接要用新的
                logDebug("返回：onSessionSettleResponse = $settleSessionResponse")
                //返回：onSessionSettleResponse = Result(
                // session=Session(
                // topic=65a62cba6767f00a4bfb3029feb2f53e16fc24990a2373b401c3c7c76a968152,
                // expiry=1690528386,
                // namespaces={eip155=Session(accounts=[eip155:1:0x6b7E1e936F2C50B62ffA373EfFCeE1F77706e757, eip155:56:0x6b7E1e936F2C50B62ffA373EfFCeE1F77706e757, eip155:2999:0x6b7E1e936F2C50B62ffA373EfFCeE1F77706e757],
                // methods=[personal_sign, eth_sendTransaction, eth_signTransaction],
                // events=[chainChanged, accountsChanged], extensions=null)},
                // metaData=
                // AppMetaData(
                // name=BitYuan,
                // description=,
                // url=https://bityuan.com,
                // icons=[https://bityuan.com/favicon.ico],
                // redirect=null)))
              /*  lifecycleScope.launch(Dispatchers.Main) {
                    gotoSettleResponse(settleSessionResponse)
                }*/
                _signModel.postValue(settleSessionResponse)

            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                logDebug("返回：onSessionUpdateResponse = $sessionUpdateResponse")

            }

        })
    }
}