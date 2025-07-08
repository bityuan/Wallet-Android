package com.fzm.walletdemo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fzm.wallet.sdk.bean.Brc20Balance
import com.fzm.walletmodule.databinding.ItemCoinBrc20Binding

class Brc20BalanceAdapter(private val context: Context, private val list: List<Brc20Balance>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemCoinBrc20Binding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val item = list[position]
            holder.binding.tvName.text = item.ticker
            holder.binding.tvAllBalance.text = item.availableBalance
            holder.binding.tvOutBalance.text = item.transferableBalance
            holder.binding.tvAddr.text = item.address
//            Glide.with(context).load(item.icon).apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
//                .into(holder.binding.ivExplore)
        }
        holder.itemView.setOnClickListener {
            clickListener(position)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding: ItemCoinBrc20Binding) : RecyclerView.ViewHolder(binding.root)

    lateinit var clickListener: (Int) -> Unit

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.clickListener = listener
    }


}