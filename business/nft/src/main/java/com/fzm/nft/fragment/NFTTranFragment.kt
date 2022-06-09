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
import com.fzm.nft.NftTran
import com.fzm.nft.adapter.NFTAdapter
import com.fzm.nft.adapter.NFTTranAdapter
import com.fzm.nft.databinding.FragmentNftTranBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.RouterPath.PARAM_NFT_TRAN
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.ui.fragment.TransactionFragment
import org.jetbrains.anko.support.v4.onRefresh
import org.koin.android.ext.android.inject
import walletapi.HDWallet
import walletapi.Walletapi

class NFTTranFragment : Fragment() {

    private lateinit var binding: FragmentNftTranBinding
    private val nftViewModel: NFTViewModel by inject(walletQualifier)

    private val type by lazy { arguments?.getInt(TYPE) as Int }
    private val coin by lazy { arguments?.getSerializable(COIN) as Coin }
    private lateinit var adapter: NFTTranAdapter

    private val list = mutableListOf<NftTran>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNftTranBinding.inflate(inflater, container, false)
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
            initData()
        }

        binding.rvList.layoutManager = LinearLayoutManager(context)
        adapter = NFTTranAdapter(list, coin)
        binding.rvList.adapter = adapter
        adapter.setOnItemClickListener {
            val tran = list[it]
            ARouter.getInstance().build(RouterPath.NFT_TRAN_DETAIL).withSerializable(PARAM_NFT_TRAN,tran).navigation()
        }
    }

    private fun initObserver() {
        nftViewModel.getNftTran.observe(viewLifecycleOwner, Observer {
            binding.swipeList.isRefreshing = false
            list.clear()
            list.addAll(it)
            adapter.notifyDataSetChanged()
        })
    }

    private fun initData() {
        nftViewModel.getNFTTran(
            Walletapi.TypeETHString,
            "",
            coin.contract_address,
            coin.address,
            0,
            20,
            0,
            type
        )
    }

    companion object {
        private const val TYPE = "type"
        private const val COIN = "coin"
        fun newInstance(type: Int, coin: Coin): NFTTranFragment {
            val bundle = Bundle()
            bundle.putInt(TYPE, type)
            bundle.putSerializable(COIN, coin)
            val fragment = NFTTranFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

}