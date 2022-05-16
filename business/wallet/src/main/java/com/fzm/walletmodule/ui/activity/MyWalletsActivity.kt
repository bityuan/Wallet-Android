package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.event.MyWalletEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.isFastClick
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.android.synthetic.main.activity_my_wallets.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.litepal.LitePal

class MyWalletsActivity : BaseActivity() {
    private var mAdapter: CommonAdapter<PWallet>? = null
    private var mSelectedId: Long = 0
    private val list: ArrayList<PWallet> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mStatusColor = Color.WHITE
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_wallets)
        initData()
        refresh()
        initListener()
    }

    override fun initData() {
        listView.layoutManager = LinearLayoutManager(this)

        mAdapter = object : CommonAdapter<PWallet>(this, R.layout.view_my_wallet, ArrayList<PWallet>()) {
                override fun convert(holder: ViewHolder?, wallet: PWallet?, position: Int) {
                //    holder!!.setText(R.id.money, moneySign + DecimalUtils.subWithNum(wallet!!.amounts, 2))
                    holder!!.setVisible(R.id.money, true)
                    holder.setVisible(R.id.walletId, true)
                    holder.setText(R.id.walletId, getString(R.string.my_wallets_mnem_walllet))
                    holder.setBackgroundRes(R.id.walletBg, R.mipmap.my_wallet_bg_black)
                    holder.setBackgroundRes(R.id.coinIcon, R.mipmap.my_wallet_coins)
                    holder.setText(R.id.name, wallet?.name)
                    holder.setTextColorRes(R.id.name, R.color.white)
                    holder.setTextColorRes(R.id.money, R.color.white)
                    holder.setTextColorRes(R.id.walletId, R.color.color_80ffffff)
                    if (wallet?.id == mSelectedId) {
                        holder.setVisible(R.id.currentWallet, true)
                    } else {
                        holder.setVisible(R.id.currentWallet, false)
                    }
                }
            }
        listView.adapter = mAdapter
        listView.setOnItemClickListener { holder, position ->
            val wallet = mAdapter!!.datas[position] as PWallet
            BWallet.get().changeWallet(wallet.id.toString())
            EventBus.getDefault().post(MyWalletEvent(wallet,true))
            finish()
        }
    }

    private fun refresh() {
        mSelectedId = BWallet.get().getCurrentWallet()?.id ?: 0L
        list.clear()
        doAsync {
            val walletList: List<PWallet> = LitePal.findAll(PWallet::class.java, true)
            for (i in walletList.indices) {
                val pWallet = walletList[i]
                if (pWallet.id == mSelectedId) {
                    list.add(0, pWallet)
                } else {
                    list.add(pWallet)
                }
            }
            runOnUiThread {
                val thisList = mAdapter!!.datas
                thisList.clear()
                thisList.addAll(list)
                if (ListUtils.isEmpty(list)){
                    stateView.showEmpty()
                }else{
                    stateView.showContentWithNoAnim()
                }
                mAdapter!!.notifyDataSetChanged()
            }
        }
    }

    override fun initListener() {
        walletCreateLayout.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            startActivity<CreateWalletActivity>()
        }
        walletImport.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            startActivity<ImportWalletActivity>()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        refresh()
    }
}