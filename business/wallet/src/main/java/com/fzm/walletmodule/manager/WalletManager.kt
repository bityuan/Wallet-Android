package com.fzm.walletmodule.manager

import android.app.AlertDialog
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.finalteam.loadingviewfinal.RecyclerViewFinal
import com.bumptech.glide.Glide
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.GlideUtils
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import walletapi.Walletapi

class WalletManager {
    //选择主链
    fun chooseChain(activity: AppCompatActivity?, data: List<Coin>) {
        val builder = AlertDialog.Builder(activity)
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_choose_chain, null)
        builder.setView(view)
        val alertDialog = builder.create()
        val window = alertDialog.window
        window!!.setBackgroundDrawableResource(R.color.transparent)
        alertDialog.show()
        window.decorView.setPadding(0, 0, 0, 0)
        window.setGravity(Gravity.BOTTOM)
        val lp = window.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = lp
        val tvClose = view.findViewById<TextView>(R.id.tv_close)
        val rvList: RecyclerViewFinal = view.findViewById(R.id.rv_list)
        tvClose.setOnClickListener { alertDialog.dismiss() }
        rvList.layoutManager = LinearLayoutManager(activity)
        rvList.adapter =
            object : CommonAdapter<Coin>(activity, R.layout.listitem_choose_chain, data) {
                override fun convert(holder: ViewHolder, coin: Coin, position: Int) {
                    holder.setText(R.id.tv_chain, coin.uiName)
                    holder.setText(
                        R.id.nickName,
                        if (TextUtils.isEmpty(coin.nickname)) "" else "·" + coin.nickname
                    )
                    holder.setText(R.id.address, coin.address)
                    val ivCoin: ImageView = holder.getView(R.id.icon)
                    if (TextUtils.isEmpty(coin.icon)) {
                        Glide.with(mContext).load(coin.icon).into(ivCoin)
                    } else {
                        Glide.with(mContext).load(coin.icon).into(ivCoin)
                    }

                }
            }
        rvList.setOnItemClickListener { holder, position ->
            if (null != mOnItemClickListener) {
                mOnItemClickListener?.onItemClick(position)
            }
        }
    }

    private var mOnItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }


    //导出私钥和公钥
    fun exportContent(activity: AppCompatActivity?, privateKey: String, name: String) {
        val builder = AlertDialog.Builder(activity)
        val view: View =
            LayoutInflater.from(activity).inflate(R.layout.dialog_priv, null)
        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        alertDialog.show()
        val tvWif = view.findViewById<TextView>(R.id.tv_wif)
        tvWif.text = "查看wif 压缩$name"
        tvWif.visibility = if (name == "BTC公钥" || name == "BTC私钥") View.VISIBLE else View.GONE
        val ivClose = view.findViewById<ImageView>(R.id.iv_close)
        val ivQRcode = view.findViewById<ImageView>(R.id.iv_pri_qrcode)
        GlideUtils.intoQRBitmap(ivQRcode, privateKey)
        val tvMnem = view.findViewById<TextView>(R.id.tv_mnem)
        val titleView = view.findViewById<TextView>(R.id.title)
        val btnCopy = view.findViewById<Button>(R.id.btn_copy)
        titleView.text = name
        tvMnem.text = privateKey

        var wifKey = ""
        tvWif.setOnClickListener {
            wifKey = Walletapi.hexKeyToWIF(privateKey, true)
            tvMnem.text = wifKey
            GlideUtils.intoQRBitmap(ivQRcode, wifKey)
        }
        ivClose.setOnClickListener { alertDialog.dismiss() }
        btnCopy.setOnClickListener {
            ClipboardUtils.clip(
                activity,
                wifKey.ifEmpty { privateKey }
            )
        }
    }

    //导出助记词
    fun exportMnem(activity: AppCompatActivity, mnem: String, pWallet: PWallet) {
        val builder = AlertDialog.Builder(activity)
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_mnem, null)
        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        alertDialog.show()
        val ivClose = view.findViewById<ImageView>(R.id.iv_close)
        val ivQRcode = view.findViewById<ImageView>(R.id.iv_mnem_qrcode)
        GlideUtils.intoQRBitmap(ivQRcode, mnem)
        val tvMnem = view.findViewById<TextView>(R.id.tv_mnem)
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        tvTitle.text = "导出助记词"
        val btnCopy = view.findViewById<Button>(R.id.btn_copy)
        tvMnem.text =
            if (pWallet.mnemType == PWallet.TYPE_CHINESE) configSpace(mnem, false) else mnem
        ivClose.setOnClickListener { alertDialog.dismiss() }
        btnCopy.setOnClickListener {
            ClipboardUtils.clip(activity, mnem)
        }
    }

    fun configSpace(mnem: String, isPrinter: Boolean): String? {
        if (TextUtils.isEmpty(mnem)) {
            return ""
        }
        val chineses = mnem.replace(" ".toRegex(), "")
        var chinese: String? = ""
        for (i in 0 until chineses.length) {
            val value = chineses[i].toString()
            val j = i + 1
            if (j % 3 == 0) {
                chinese += "$value    "
                if (isPrinter && j % 6 == 0) {
                    chinese += "\n"
                }
            } else {
                chinese += value
            }
        }
        return chinese
    }

}