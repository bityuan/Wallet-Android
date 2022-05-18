package com.fzm.nft.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fzm.nft.databinding.ItemNftBinding
import com.fzm.wallet.sdk.db.entity.Coin

class NFTAdapter(private val context: Context, private val list: List<Coin>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val nftBinding = ItemNftBinding.inflate(LayoutInflater.from(parent.context))
        return NFTViewHolder(nftBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NFTViewHolder) {
            val coin = list[position]
            holder.binding.tvName.text = coin.name
            holder.binding.tvNickName.text = " (NFT) "
            holder.binding.tvBalance.text = coin.balance
            Glide.with(context).load(coin.icon).into(holder.binding.ivIcon)
        }
        holder.itemView.setOnClickListener {
            clickListener(position)
        }

    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            if (holder is NFTViewHolder) {
                val payload = payloads[0] as Int
                if (payload == PAYLOAD_BALANCE) {
                    val coin = list[position]
                    holder.binding.tvBalance.text = coin.balance
                }

            }

        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class NFTViewHolder(val binding: ItemNftBinding) : RecyclerView.ViewHolder(binding.root)

    lateinit var clickListener: (Int) -> Unit

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.clickListener = listener
    }


    companion object {
        const val PAYLOAD_BALANCE = 1
    }

}