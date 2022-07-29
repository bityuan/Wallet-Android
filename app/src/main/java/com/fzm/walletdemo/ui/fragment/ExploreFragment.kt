package com.fzm.walletdemo.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
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
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.databinding.FragmentExploreBinding
import com.fzm.walletdemo.databinding.ItemExploreBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.toast
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find

class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreBinding
    private var exList = mutableListOf<ExploreBean.AppsBean>()
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
        context?.let {
            binding.rvExplore.layoutManager = LinearLayoutManager(context)
            adapter = Adapter(it)
            binding.rvExplore.adapter = adapter
        }
        binding.swipeExplore.setOnRefreshListener {
            getExploreAll()
        }
        getExploreAll()
    }

    private fun getExploreAll() {
        lifecycleScope.launch {
            val list = BWallet.get().getExploreList()
            withContext(Dispatchers.Main) {
                binding.swipeExplore.isRefreshing = false
                val newList = mutableListOf<ExploreBean.AppsBean>()
                for (l in list) {
                    newList.addAll(l.apps)
                }
                val diffCallBack = DiffCallBack(exList, newList)
                val diffResult = DiffUtil.calculateDiff(diffCallBack)
                diffResult.dispatchUpdatesTo(adapter)
                adapter.setData(newList)
                exList = newList
            }
        }

        adapter.setOnItemClickListener {
            exList[it].let { appBean ->
                if (appBean.type == 1) {
                    val count = LitePal.count<PWallet>()
                    if (count == 0) {
                        toast("请先创建钱包")
                        return@let
                    }
                }
                ARouter.getInstance().build(RouterPath.APP_DAPP).withString("name", appBean.name)
                    .withString("url", appBean.app_url).navigation()
            }

        }
    }


    inner class Adapter(private val context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var list: List<ExploreBean.AppsBean> = mutableListOf()

        fun setData(list: List<ExploreBean.AppsBean>) {
            this.list = list
        }

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
                    .load(item.icon)
                    .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
                    .into(holder.binding.ivExploreVertical)

                holder.itemView.setOnClickListener { clickListener(position) }
            }

        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
            } else {
                val item = list[position]
                val payload = payloads[0] as Bundle
                for (key in payload.keySet()) {
                    when (key) {
                        "pl_name" -> {
                            if (holder is ViewHolder) {
                                holder.binding.tvExploreVerticalTitle.text = item.name
                            }
                        }
                        "pl_slogan" -> {
                            if (holder is ViewHolder) {
                                holder.binding.tvExploreVerticalDes.text = item.slogan
                            }
                        }
                    }
                }
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


    inner class DiffCallBack(
        private val oldList: MutableList<ExploreBean.AppsBean>,
        private val newList: MutableList<ExploreBean.AppsBean>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            if (oldItem.name != newItem.name) {
                return false
            } else if (oldItem.slogan != newItem.slogan) {
                return false
            } else if (oldItem.icon != newItem.icon) {
                return false
            }

            return true
        }


        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            val payload = Bundle()
            if (oldItem.name != newItem.name) {
                payload.putString("pl_name", newItem.name)
            }
            if (oldItem.slogan != newItem.slogan) {
                payload.putString("pl_slogan", newItem.slogan)
            }
            return payload
        }

    }

}