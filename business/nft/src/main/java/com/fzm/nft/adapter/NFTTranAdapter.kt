package com.fzm.nft.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fzm.nft.NftTran
import com.fzm.nft.databinding.ItemNftTranBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.walletmodule.utils.TimeUtils

class NFTTranAdapter(private val list: List<NftTran>, private val coin: Coin) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemNftTranBinding.inflate(LayoutInflater.from(parent.context))
        return NFTTranViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NFTTranViewHolder) {
            val nftTran = list[position]

            when (coin.address) {
                nftTran.from -> {
                    "-${nftTran.value}${coin.name}".also { holder.binding.tvMoney.text = it }
                    holder.binding.tvAddress.text = nftTran.to
                }
                nftTran.to -> {
                    "+${nftTran.value}${coin.name}".also { holder.binding.tvMoney.text = it }
                    holder.binding.tvAddress.text = nftTran.from
                }
            }

            holder.binding.tvTime.text = TimeUtils.getTime(nftTran.blocktime * 1000L)
            when (nftTran.status) {
                -1 -> {
                    holder.binding.tvStatus.text = "失败"
                    holder.binding.tvStatus.setTextColor(Color.parseColor("#EC5151"))
                }
                0 -> {
                    holder.binding.tvStatus.text = "确认中"
                    holder.binding.tvStatus.setTextColor(Color.parseColor("#7190FF"))
                }
                1 -> {
                    holder.binding.tvStatus.text = "成功"
                    holder.binding.tvStatus.setTextColor(Color.parseColor("#37AEC4"))
                }
            }

        }

        holder.itemView.setOnClickListener { clickListener(position) }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    inner class NFTTranViewHolder(val binding: ItemNftTranBinding) :
        RecyclerView.ViewHolder(binding.root)


    lateinit var clickListener: (Int) -> Unit

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.clickListener = listener
    }
}