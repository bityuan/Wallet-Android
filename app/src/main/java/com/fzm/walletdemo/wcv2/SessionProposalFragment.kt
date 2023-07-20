package com.fzm.walletdemo.wcv2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentSessionProposalBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet

class SessionProposalFragment : BottomSheetDialogFragment() {
    private var address: String? = ""
    private var chooseChain: String = ""
    private lateinit var binding: FragmentSessionProposalBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSessionProposalBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val url = it.getString("url")
            val name = it.getString("name")
            val proposerPublicKey = it.getString("proposerPublicKey")
            val sessionTopic = it.getString("sessionTopic")
            val chain = it.getString("chain")
            //56:bsc
            when (chain) {
                GoWallet.CHAIN_ID_ETH -> {
                    chooseChain = "ETH"
                }

                GoWallet.CHAIN_ID_BNB -> {
                    chooseChain = "BNB"

                }
            }
            address = GoWallet.getChain(chooseChain)?.address
            val namespaces = configNamespaces()


            binding.tvDappName.text = name
            binding.tvDappUrl.text = url
            binding.btnRefuse.setOnClickListener {
                //拒绝的原因，随便填写
                val rejectionReason = "Reject Session"
                val reject = Wallet.Params.SessionReject(proposerPublicKey!!, rejectionReason)
                Web3Wallet.rejectSession(reject) { error ->
                    logDebug("$error")
                }
                activity?.finish()
            }
            binding.btnGrant.setOnClickListener {
                logDebug("proposerPublicKey = $proposerPublicKey, namespaces = $namespaces")
                val approve = Wallet.Params.SessionApprove(proposerPublicKey!!, namespaces)
                Web3Wallet.approveSession(approve) { error ->
                    logDebug("$error")
                }


                val bundle = Bundle()
                bundle.putString("url", url)
                bundle.putString("name", name)
                bundle.putString("address", address)
                bundle.putString("chooseChain", chooseChain)
                bundle.putString("sessionTopic", sessionTopic)
                findNavController().navigate(R.id.action_to_sessionProposaled, bundle)

            }
        }
    }

    private fun configNamespaces(): Map<String, Wallet.Model.Namespace.Session> {
        return mapOf(
            "eip155" to Wallet.Model.Namespace.Session(
                listOf("${GoWallet.CHAIN_ID_ETH}:$address","${GoWallet.CHAIN_ID_BNB}:$address"),
                listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                listOf("chainChanged", "accountsChanged"),
                null
            )
        )
    }
}