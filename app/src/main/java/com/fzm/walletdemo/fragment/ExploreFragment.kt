package com.fzm.walletdemo.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.walletdemo.databinding.FragmentExploreBinding
import com.fzm.walletdemo.databinding.ItemExploreBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreBinding
    private val exList = mutableListOf<ExploreBean.AppsBean>()
    private lateinit var adapter: Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getExploreAll()
    }

    private fun getExploreAll() {
        context?.let {
            binding.rvExplore.layoutManager = LinearLayoutManager(context)
            adapter = Adapter(it, exList)
            binding.rvExplore.adapter = adapter
        }

        exList.clear()
        CoroutineScope((Dispatchers.IO)).launch {
            val list = BWallet.get().getExploreList()
            withContext(Dispatchers.Main) {
                for (l in list) {
                    exList.addAll(l.apps)
                }
                adapter.notifyDataSetChanged()
            }
        }

        adapter.setOnItemClickListener {
            exList[it].let { appBean ->
                ARouter.getInstance().build(RouterPath.EX_DAPP).withString("name", appBean.name)
                    .withString("url", appBean.app_url).navigation()
            }

        }
    }


    inner class Adapter(
        private val context: Context,
        private val list: List<ExploreBean.AppsBean>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding = ItemExploreBinding.inflate(LayoutInflater.from(parent.context))
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ViewHolder) {
                val item = list[position]
                holder.binding.tvExploreVerticalTitle.text = item.name
                holder.binding.tvExploreVerticalDes.text = item.slogan
                Glide.with(context)
                    .load(item.icon).apply(RequestOptions().transforms(CenterCrop(),RoundedCorners(20)))
                    .into(holder.binding.ivExploreVertical)

                holder.itemView.setOnClickListener { clickListener(position) }
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }

        lateinit var clickListener: (Int) -> Unit

        fun setOnItemClickListener(listener: (Int) -> Unit) {
            this.clickListener = listener
        }


        inner class ViewHolder(val binding: ItemExploreBinding) :
            RecyclerView.ViewHolder(binding.root)

    }


}