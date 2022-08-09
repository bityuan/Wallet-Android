package com.fzm.walletdemo.ui.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.walletdemo.databinding.ItemExploreBinding
import com.fzm.walletdemo.databinding.ItemExploreGridBinding
import com.fzm.walletdemo.databinding.ItemExploreTitleBinding

class ExploreAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var list: List<ExploreBean.AppsBean> = mutableListOf()

    companion object {
        const val ITEM_VIEW_TYPE_TITLE = 1
        const val ITEM_VIEW_TYPE = 2
        const val ITEM_VIEW_TYPE_GRID = 3

        const val PAYLOAD_NAME = "payload_name"
        const val PAYLOAD_SLOGAN = "payload_slogan"
        const val PAYLOAD_ICON = "payload_icon"
    }

    fun setData(list: List<ExploreBean.AppsBean>) {
        this.list = list
    }

    override fun getItemViewType(position: Int): Int {
        val item = list[position]
        return when (item.style) {
            1 -> {
                //id为-1的作为title
                if (item.id == -1) {
                    ITEM_VIEW_TYPE_TITLE
                } else {
                    ITEM_VIEW_TYPE
                }
            }
            2 -> {
                ITEM_VIEW_TYPE_GRID
            }
            else -> ITEM_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_TITLE -> {
                val binding = ItemExploreTitleBinding.inflate(LayoutInflater.from(parent.context))
                TitleViewHolder(binding)
            }

            ITEM_VIEW_TYPE -> {
                val binding = ItemExploreBinding.inflate(LayoutInflater.from(parent.context))
                ViewHolder(binding)
            }
            ITEM_VIEW_TYPE_GRID -> {
                val binding = ItemExploreGridBinding.inflate(LayoutInflater.from(parent.context))
                GridViewHolder(binding)
            }

            else -> {
                val binding = ItemExploreBinding.inflate(LayoutInflater.from(parent.context))
                ViewHolder(binding)
            }
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val item = list[position]
                holder.binding.tvExploreTitle.text = item.name
                holder.binding.tvExploreDes.text = item.slogan
                Glide.with(context)
                    .load(item.icon)
                    .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
                    .into(holder.binding.ivExplore)

                holder.itemView.setOnClickListener { clickListener(position) }
            }
            is TitleViewHolder -> {
                val item = list[position]
                holder.binding.tvTitle.text = item.name
            }
            is GridViewHolder -> {
                val item = list[position]
                holder.binding.tvExploreTitle.text = item.name
                Glide.with(context)
                    .load(item.icon)
                    .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
                    .into(holder.binding.ivExplore)

                holder.itemView.setOnClickListener { clickListener(position) }
            }
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
                    PAYLOAD_NAME -> {
                        if (holder is ViewHolder) {
                            holder.binding.tvExploreTitle.text = item.name
                        }
                    }
                    PAYLOAD_SLOGAN -> {
                        if (holder is ViewHolder) {
                            holder.binding.tvExploreDes.text = item.slogan
                        }
                    }
                    PAYLOAD_ICON -> {
                        if (holder is ViewHolder) {
                            Glide.with(context)
                                .load(item.icon)
                                .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
                                .into(holder.binding.ivExplore)
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

    inner class TitleViewHolder(val binding: ItemExploreTitleBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class GridViewHolder(val binding: ItemExploreGridBinding) :
        RecyclerView.ViewHolder(binding.root)

}