package com.fzm.walletdemo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.fzm.nft.databinding.ItemNftBinding
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.walletdemo.databinding.ItemExploreBinding

class ExploresAdapter(private val context: Context, private val list: List<ExploreBean.AppsBean>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemExploreBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val item = list[position]
            holder.binding.tvExploreTitle.text = item.name
            holder.binding.tvExploreDes.text = item.slogan
            Glide.with(context).load(item.icon).apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                .into(holder.binding.ivExplore)
        }
        holder.itemView.setOnClickListener {
            clickListener(position)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding: ItemExploreBinding) : RecyclerView.ViewHolder(binding.root)

    lateinit var clickListener: (Int) -> Unit

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.clickListener = listener
    }


}