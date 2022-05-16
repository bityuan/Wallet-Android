package com.fzm.walletdemo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentNftBinding

class NFTFragment : Fragment() {

    private lateinit var binding: FragmentNftBinding
    private lateinit var nftAdapter: BaseQuickAdapter<String, BaseViewHolder>
    private val list= mutableListOf("老虎","长颈鹿")
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


        nftAdapter = object: BaseQuickAdapter<String,BaseViewHolder>(R.layout.item_nft,list){
            override fun convert(holder: BaseViewHolder, item: String) {
                holder.setText(R.id.tv_name,item)
            }

        }
        binding.rvList.layoutManager = LinearLayoutManager(context)
        binding.rvList.adapter = nftAdapter
        nftAdapter.setOnItemClickListener { adapter, view, position ->
            val item = list[position]

        }
    }
}