package com.fzm.walletdemo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fzm.nft.databinding.ItemNftTranBinding
import com.fzm.wallet.sdk.bean.Brc20Tran
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.walletmodule.utils.TimeUtils

class Brc20TransAdapter(private val list: List<Brc20Tran>, private val coin: Coin) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemNftTranBinding.inflate(LayoutInflater.from(parent.context))
        return TranViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TranViewHolder) {
            val item = list[position]
            //inscribe-transfer": 铭刻铭文：
            //send： 发送
            //inscribe-mint：铸造铭文 铸币
            when (coin.address) {
                item.from -> {
                    "-${item.amount} ${coin.name}".also { holder.binding.tvMoney.text = it }
                    holder.binding.tvAddress.text = item.to
                }

                item.to -> {
                    "+${item.amount} ${coin.name}".also { holder.binding.tvMoney.text = it }
                    holder.binding.tvAddress.text = item.from
                }
            }

            holder.binding.tvTime.text =
                if (item.blocktime == 0L) {
                    "确认中"
                } else {
                    TimeUtils.getTime(item.blocktime * 1000L)
                }



            when (item.type) {
                "inscribe-transfer" -> {
                    holder.binding.tvStatus.text = "铭刻铭文"
                }

                "send" -> {
                    holder.binding.tvStatus.text = "发送"
                }

                "inscribe-mint" -> {
                    holder.binding.tvStatus.text = "铸造铭文"
                }
            }

        }

        //holder.itemView.setOnClickListener { clickListener(position) }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    inner class TranViewHolder(val binding: ItemNftTranBinding) :
        RecyclerView.ViewHolder(binding.root)


    lateinit var clickListener: (Int) -> Unit

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.clickListener = listener
    }
}