package com.fzm.walletdemo.ui.adapter

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.fzm.wallet.sdk.bean.ExploreBean

class ExploreDiffCallBack(
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

        //这里返回false，getChangePayload才会被调用
        return true
    }


    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        val payload = Bundle()
        if (oldItem.name != newItem.name) {
            payload.putString(ExploreAdapter.PAYLOAD_NAME, newItem.name)
        }
        if (oldItem.slogan != newItem.slogan) {
            payload.putString(ExploreAdapter.PAYLOAD_SLOGAN, newItem.slogan)
        }
        if (oldItem.icon != newItem.icon) {
            payload.putString(ExploreAdapter.PAYLOAD_ICON, newItem.icon)
        }
        if (oldItem.style != newItem.style) {
            payload.putInt(ExploreAdapter.PAYLOAD_STYLE, newItem.style)
        }
        return payload
    }

}