package com.fzm.walletmodule.adapter

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin

class CoinDiffCallBack(
    private val oldList: MutableList<Coin>,
    private val newList: MutableList<Coin>
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
        var same = true
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        //界面上没有的字段，这里无需判断
        //这里是判断本地的数据是否刷新到页面上
        if (oldItem.nickname != newItem.nickname) {
            same = false
        } else if (oldItem.icon != newItem.icon) {
            same = false
        } else if (oldItem.balance != newItem.balance) {
            same = false
        }

        //这里返回false，getChangePayload才会被调用
        logDebug("same$same")
        return same
    }


    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        val payload = Bundle()
        if (oldItem.nickname != newItem.nickname) {
            payload.putString(CoinAdapter.PAYLOAD_NICKNAME, newItem.nickname)
        }
        if (oldItem.icon != newItem.icon) {
            payload.putString(CoinAdapter.PAYLOAD_ICON, newItem.icon)
        }
        if (oldItem.balance != newItem.balance) {
            payload.putString(CoinAdapter.PAYLOAD_BALANCE, newItem.balance)
        }
        return payload
    }

}