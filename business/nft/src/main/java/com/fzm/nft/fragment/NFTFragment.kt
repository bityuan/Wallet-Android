package com.fzm.nft.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.NFTViewModel
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.nft.adapter.NFTAdapter
import com.fzm.nft.databinding.FragmentNftBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletmodule.utils.isFastClick
import com.fzm.walletmodule.vm.WalletViewModel
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.findAll
import walletapi.Walletapi

class NFTFragment : Fragment() {

    private val nftViewModel: NFTViewModel by inject(walletQualifier)
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private lateinit var binding: FragmentNftBinding
    private lateinit var nftAdapter: NFTAdapter
    private val list = mutableListOf<Coin>()
    private val chains by lazy { LitePal.findAll<Coin>() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
        initData()
    }

    private fun initView() {
        binding.swipeList.onRefresh {
            walletViewModel.getCoinList(listOf("MEKA,ethereum"))
        }
        context?.let {
            nftAdapter = NFTAdapter(it, list)
        }
        binding.rvList.layoutManager = LinearLayoutManager(context)
        binding.rvList.adapter = nftAdapter
        nftAdapter.setOnItemClickListener {
            if (isFastClick()) {
                return@setOnItemClickListener
            }
            val coin = list[it]
            coin.address?.let {
                ARouter.getInstance().build(RouterPath.NFT_TRAN).withSerializable(RouterPath.PARAM_COIN, coin).navigation()
            }
        }
    }


    private fun initObserver() {
        nftViewModel.getNFTValue.observe(viewLifecycleOwner, Observer {
            for (l in list) {
                if (l.contract_address == it.contractAddr) {
                    l.balance = it.balance
                    nftAdapter.notifyItemChanged(it.position, NFTAdapter.PAYLOAD_BALANCE)
                }
            }

        })
        walletViewModel.getCoinList.observe(viewLifecycleOwner, Observer {
            binding.swipeList.isRefreshing = false
            if (it.isSucceed()) {
                it.data()?.let {
                    list.clear()
                    list.addAll(it)
                    nftAdapter.notifyDataSetChanged()

                    for (i in list.indices) {
                        val coin = list[i]
                        val chain = chains.find { coin.chain == it.chain }
                        coin.address = chain?.address
                        coin.address?.let {
                            //list[i].address = eth.address
                            //list[i].address = GoWallet.testEthAddr
                            //list[i].address = GoWallet.testEthAddr
                            nftViewModel.getNFTBalance(
                                i,
                                Walletapi.TypeETHString,
                                "",
                                list[i].address,
                                list[i].contract_address
                            )
                        }


                    }
                }
            } else {
                toast(it.error())
            }
        })
    }

    private fun initData() {
        walletViewModel.getCoinList(listOf("MEKA,ethereum"))
    }
}