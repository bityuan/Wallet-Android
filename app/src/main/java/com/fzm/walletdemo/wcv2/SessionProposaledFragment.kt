package com.fzm.walletdemo.wcv2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.walletdemo.databinding.FragmentSessionProposaledBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet

class SessionProposaledFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSessionProposaledBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSessionProposaledBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val url = it.getString("url")
            val name = it.getString("name")
            val address = it.getString("address")
            val chooseChain = it.getString("chooseChain")
            val sessionTopic = it.getString("sessionTopic")
            logDebug("sessionTopic = $sessionTopic")

            binding.tvWalletState.text = "$name 已经与下列钱包建立连接"
            binding.tvDappUrl.text = url
            binding.tvAddress.text = address
            binding.tvChain.text = chooseChain
            binding.btnDis.setOnClickListener {
                sessionTopic?.let { topic ->
                    val sessionDisconnect = Wallet.Params.SessionDisconnect(topic)
                    Web3Wallet.disconnectSession(sessionDisconnect) { error ->
                        logDebug("disconnectSession = $error")
                    }
                }
            activity?.finish()
            }
        }
    }

}