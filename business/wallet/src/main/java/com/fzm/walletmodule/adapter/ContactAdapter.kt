package com.fzm.walletmodule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fzm.wallet.sdk.db.entity.Contacts
import com.fzm.wallet.sdk.widget.sidebar.IndexAdapter
import com.fzm.wallet.sdk.widget.sidebar.Indexable
import com.fzm.walletmodule.R
import com.jiang.android.lib.adapter.expand.StickyRecyclerHeadersAdapter
import com.jiang.android.lib.widget.SwipeItemLayout
import com.jiang.android.lib.widget.SwipeItemLayout.SwipeItemLayoutDelegate
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.util.*

class ContactAdapter(context: Context?, layoutId: Int, datas: MutableList<Contacts>) : CommonAdapter<Contacts>(context, layoutId, datas), StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder>,
    IndexAdapter {
    private var mContactsList: MutableList<Contacts> = ArrayList()
    private val mOpenSwipes: MutableList<SwipeItemLayout> = ArrayList()

    override fun getHeaderId(position: Int): Long {
        return mContactsList[position].sortLetters[0].toLong()
    }

    override fun onCreateHeaderViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.contact_header, viewGroup, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val textView = holder.itemView as TextView
        val showValue = mContactsList[position].sortLetters[0].toString()
        textView.text = showValue
    }

    override fun convert(holder: ViewHolder, contacts: Contacts, position: Int) {
        holder.setText(R.id.tv_name, contacts.nickName)
        holder.setText(R.id.tv_phone, contacts.phone)
        val nameFirst = holder.getView<TextView>(R.id.tv_name_first)
        if (contacts.nickName != null && contacts.nickName.length > 0) {
            holder.setText(R.id.tv_name_first, contacts.nickName.substring(0, 1))
        } else {
            holder.setText(R.id.tv_name_first, "#")
        }
        if (position % 3 == 1) {
            nameFirst.setBackgroundResource(R.drawable.bg_constacts_green)
        } else if (position % 3 == 2) {
            nameFirst.setBackgroundResource(R.drawable.bg_constacts_purple)
        } else {
            nameFirst.setBackgroundResource(R.drawable.bg_constacts_blue)
        }
        val swipeItemLayout = holder.getView<SwipeItemLayout>(R.id.swipe_content)
        swipeItemLayout.setDelegate(object : SwipeItemLayoutDelegate {
            override fun onSwipeItemLayoutOpened(swipeItemLayout: SwipeItemLayout) {
                mOpenSwipes.add(swipeItemLayout)
            }

            override fun onSwipeItemLayoutClosed(swipeItemLayout: SwipeItemLayout) {
                mOpenSwipes.remove(swipeItemLayout)
                holder.setText(R.id.tv_delete, mContext.getString(R.string.delete))
                holder.setBackgroundRes(R.id.tv_delete, R.color.color_8E92A3)
            }

            override fun onSwipeItemLayoutStartOpen(swipeItemLayout: SwipeItemLayout) {
                closeSwipeLayouts()
            }
        })
        holder.setOnClickListener(R.id.tv_delete) {
            val tvDelete = holder.getView<TextView>(R.id.tv_delete)
            if (tvDelete.text == mContext.getString(R.string.o_delete)) {
                closeSwipeLayouts()
                swipeDeleteListener.delete(position)
            } else {
                holder.setBackgroundRes(R.id.tv_delete, R.color.red_common)
                holder.setText(R.id.tv_delete, mContext.getString(R.string.o_delete))
            }
        }
    }

    //关闭所有
    fun closeSwipeLayouts() {
        for (swipeItemLayout in mOpenSwipes) {
            swipeItemLayout.close()
        }
        mOpenSwipes.clear()
    }

    //根据sectioni查询位置
    fun getPositionForSection(section: Char): Int {
        for (i in 0 until itemCount) {
            val sortStr = mContactsList[i].sortLetters
            val firstChar = sortStr.toUpperCase()[0]
            if (firstChar == section) {
                return i
            }
        }
        return -1
    }

    override fun getItem(position: Int): Indexable {
        return mContactsList[position]
    }

    init {
        mContext = context
        mContactsList = datas
    }

    private lateinit var swipeDeleteListener: SwipeDeleteListener

    fun setSwipeDeleteListener(swipeDeleteListener: SwipeDeleteListener) {
        this.swipeDeleteListener = swipeDeleteListener
    }

    interface SwipeDeleteListener {
        fun delete(position: Int)
    }
}