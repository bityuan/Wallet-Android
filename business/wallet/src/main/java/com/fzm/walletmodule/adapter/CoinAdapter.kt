package com.fzm.walletmodule.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.walletmodule.databinding.ItemCoinBinding
import java.math.RoundingMode
import java.text.DecimalFormat

class CoinAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var list: List<Coin> = mutableListOf()

    companion object {

        const val PAYLOAD_NICKNAME = "payload_nickname"
        const val PAYLOAD_ICON = "payload_icon"
        const val PAYLOAD_BALANCE = "payload_balance"
    }

    fun setData(list: List<Coin>) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val item = list[position]
                holder.binding.tvName.text = item.name
                holder.binding.tvNickName.text =
                    if (item.nickname.isNullOrEmpty()) "" else "(${item.nickname})"
                Glide.with(context)
                    .load(item.icon)
                    .apply(RequestOptions().transforms(CenterCrop(), RoundedCorners(20)))
                    .into(holder.binding.ivIcon)
                val formatValue = format(item.balance.toDouble())
                holder.binding.tvBalance.text = formatValue
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
                logDebug("key ===="+key)
                when (key) {
                    PAYLOAD_NICKNAME -> {
                        if (holder is ViewHolder) {
                            holder.binding.tvNickName.text = item.nickname
                        }
                    }
                    PAYLOAD_BALANCE -> {
                        if (holder is ViewHolder) {
                            val formatValue = format(item.balance.toDouble())
                            holder.binding.tvBalance.text = formatValue
                        }
                    }
                    PAYLOAD_ICON -> {
                        if (holder is ViewHolder) {
                            Glide.with(context)
                                .load(item.icon)
                                .apply(
                                    RequestOptions().transforms(
                                        CenterCrop(),
                                        RoundedCorners(20)
                                    )
                                )
                                .into(holder.binding.ivIcon)
                        }
                    }
                }
            }
        }
    }

    private fun format(value: Double): String {
        val format = DecimalFormat("0.####")
        //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.FLOOR
        return format.format(value)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    lateinit var clickListener: (Int) -> Unit

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.clickListener = listener
    }

    inner class ViewHolder(val binding: ItemCoinBinding) :
        RecyclerView.ViewHolder(binding.root)


}