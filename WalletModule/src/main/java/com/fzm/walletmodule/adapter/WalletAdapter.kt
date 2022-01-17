package com.fzm.walletmodule.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.totalAsset
import com.fzm.walletmodule.R
import com.fzm.walletmodule.utils.DecimalUtils
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder

class WalletAdapter(context: Context, layoutId: Int, datas: List<Coin>, fragment: Fragment) :
    CommonAdapter<Coin>(context, layoutId, datas) {

    override fun convert(holder: ViewHolder?, baseCoin: Coin?, position: Int) {
        holder!!.setText(R.id.name, baseCoin!!.uiName)
        holder.setText(
            R.id.nickName,
            if (TextUtils.isEmpty(baseCoin.nickname)) "" else "(" + baseCoin.nickname.toString() + ")"
        )
        holder.setText(R.id.price, "≈¥${DecimalUtils.subZeroAndDot(baseCoin.rmb)}")
        holder.setText(R.id.money, "¥${DecimalUtils.subWithNum(baseCoin.totalAsset, 2)}")
        holder.setText(R.id.balance, DecimalUtils.subZeroAndDot(baseCoin.balance))
        val ivCoin = holder.getView<ImageView>(R.id.icon)
        if (TextUtils.isEmpty(baseCoin.icon)) {
            Glide.with(mContext)
                .load(baseCoin.icon)
                .into(ivCoin)
        } else {
            Glide.with(mContext)
                .load(baseCoin.icon)
                .into(ivCoin)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemViewDelegate =
            mItemViewDelegateManager.getItemViewDelegate(viewType)
        val layoutId = itemViewDelegate.itemViewLayoutId
        val itemView = LayoutInflater.from(mContext).inflate(
            layoutId, parent,
            false
        )
        val holder = ViewHolder(mContext, itemView)
        onViewHolderCreated(holder, holder.convertView)
        holder.setOnClickListener(R.id.middle) { view ->
            if (mItemClickListener != null) {
                val position: Int = holder.adapterPosition
                mItemClickListener?.OnItemClick(view, position)
            }
        }
        holder.setOnLongClickListener(R.id.middle) { view ->
            if (mItemClickListener != null) {
                val position: Int = holder.adapterPosition
                mItemClickListener?.OnLongItemClick(view, position)
            }
            false
        }

        return holder
    }

    interface ItemClickListener {
        fun OnItemClick(view: View?, position: Int)
        fun OnLongItemClick(view: View?, position: Int)
    }

    private var mItemClickListener: ItemClickListener? = null

    fun setOnItemClickListener(itemClickListener: ItemClickListener?) {
        mItemClickListener = itemClickListener
    }
}