package com.fzm.walletdemo.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.nft.NFTViewModel
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentNftBinding
import com.fzm.walletmodule.vm.OutViewModel
import org.jetbrains.anko.support.v4.toast
import org.koin.android.ext.android.inject
import walletapi.Walletapi

class NFTFragment : Fragment() {

    private val nftViewModel: NFTViewModel by inject(walletQualifier)

    private lateinit var binding: FragmentNftBinding
    private lateinit var nftAdapter: BaseQuickAdapter<String, BaseViewHolder>
    private val list = mutableListOf("老虎", "长颈鹿")
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
        initObserver()

        nftAdapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_nft, list) {
            override fun convert(holder: BaseViewHolder, item: String) {
                holder.setText(R.id.tv_name, item)
            }

        }
        binding.rvList.layoutManager = LinearLayoutManager(context)
        binding.rvList.adapter = nftAdapter
        nftAdapter.setOnItemClickListener { adapter, view, position ->
            val item = list[position]
            nftViewModel.getNFTBalance(
                Walletapi.TypeETHString,
                "",
                "0x6b7E1e936F2C50B62ffA373EfFCeE1F77706e757",
                "0x8e9eea1fd7bf204372d9ee85a4dcf3d415d497d1"
            )

        }
    }


    private fun initObserver() {
        nftViewModel.getNFTBalance.observe(viewLifecycleOwner, Observer {
            if (it.isSucceed()) {
                it.data()?.let {
                    Log.v("nft", it)
                    toast(it)
                }
            } else {
                toast(it.error())
            }
        })
    }
}