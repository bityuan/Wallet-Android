package com.fzm.walletdemo.wcv2

import com.walletconnect.web3.wallet.client.Wallet

class WCParam {
    companion object {
        var sessionProposal: Wallet.Model.SessionProposal? = null
        var address: String? = null
        var chooseChain: String? = null

        var sessionRequest: Wallet.Model.SessionRequest? = null
        var settledTopic: String? = null
    }

}