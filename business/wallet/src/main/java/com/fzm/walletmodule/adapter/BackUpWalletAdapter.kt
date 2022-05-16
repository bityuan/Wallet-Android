package com.fzm.walletmodule.adapter

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.walletmodule.R
import com.fzm.walletmodule.bean.WalletBackUp
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.utils.ScreenUtils

class BackUpWalletAdapter(
    context: Context,
    layoutResId: Int,
    data: MutableList<WalletBackUp>,
    mnemType: Int
) :
    BaseQuickAdapter<WalletBackUp, BaseViewHolder>(layoutResId, data) {
    private var mMnemType = 0

    init {
        mMnemType = mnemType
    }

    override fun convert(helper: BaseViewHolder, item: WalletBackUp) {
        val view = helper.getView<TextView>(R.id.recycle_text)
        if (mMnemType == PWallet.TYPE_CHINESE) {
            val pra = view.layoutParams as LinearLayout.LayoutParams
            pra.width = ScreenUtils.dp2px(context, 40f)
            pra.height = ScreenUtils.dp2px(context, 40f)
            view.setPadding(0, 0, 0, 0)
            view.layoutParams = pra
        } else {
            val pra = view.layoutParams as LinearLayout.LayoutParams
            pra.width = LinearLayout.LayoutParams.WRAP_CONTENT
            pra.height = LinearLayout.LayoutParams.WRAP_CONTENT
            view.setPadding(
                ScreenUtils.dp2px(context, 9f),
                ScreenUtils.dp2px(context, 5f),
                ScreenUtils.dp2px(context, 9f),
                ScreenUtils.dp2px(context, 6f)
            )
            view.layoutParams = pra
        }
        helper.setText(R.id.recycle_text, item.mnem)
    }


}