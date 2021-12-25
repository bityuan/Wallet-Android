package com.fzm.walletmodule.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.WalletAdapter
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.db.entity.PWallet
import com.fzm.walletmodule.event.*
import com.fzm.walletmodule.ui.activity.*
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.runOnUiThread
import org.litepal.LitePal.where
import java.lang.String
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class WalletFragment : BaseFragment() {
    private var mWalletAdapter: WalletAdapter? = null
    private var mHeaderView: View? = null
    private var mPWallet: PWallet? = null
    private val mCoinList = CopyOnWriteArrayList<Coin>()
    private var more: ImageView? = null
    private var name: TextView? = null
    private var mTimer: Timer? = null
    private var balanceTimer: Timer? = null
    private var timeCount = 0
    override fun getLayout(): Int {
        return R.layout.fragment_wallet
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        StatusBarUtil.setStatusBarColor(activity, Color.TRANSPARENT, true)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        initView()
        initHeaderView()
        initData()
        initListener()
        if(isAdded) {
            startTimer()
        }

    }

    /**
     * 开启定时器  定时更新余额
     */
    private fun startTimer() {
        balanceTimer = Timer()
        balanceTimer!!.schedule(object : TimerTask() {
            override fun run() {
               val size = mCoinList.size
               for (index in 0 until size) {
                   var coin = mCoinList[index]
                   val handleBalance = GoWallet.handleBalance(coin)
                   coin.balance = handleBalance
                   runOnUiThread {
                       mWalletAdapter?.notifyDataSetChanged()
                   }
               }
            }
        }, 0, Constants.DELAYED_TIME)
    }

    override fun initView() {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        mWalletAdapter =
            WalletAdapter(requireActivity(), R.layout.view_item_coin_info, mCoinList, this)
        mWalletAdapter?.setOnItemClickListener(object : WalletAdapter.ItemClickListener {
            override fun OnItemClick(view: View?, position: Int) {
                val coinPosition = position - 1 //减去header
                if (ListUtils.isEmpty(mCoinList) || coinPosition < 0 || coinPosition >= mCoinList.size) {
                    return
                }
                val coin: Coin = mCoinList[coinPosition]
                val `in` = Intent(activity, TransactionsActivity::class.java)
                `in`.putExtra(Coin::class.java.simpleName, coin)
                startActivity(`in`)
            }

            override fun OnLongItemClick(view: View?, position: Int) {}
        })
        recyclerView.adapter = mWalletAdapter
    }

    private fun initHeaderView() {
        mHeaderView =
            LayoutInflater.from(activity).inflate(R.layout.view_header_wallet, null, false)
        more = mHeaderView?.findViewById<ImageView>(R.id.more)
        name = mHeaderView?.findViewById<TextView>(R.id.name)
        recyclerView.addHeaderView(mHeaderView)
    }

    override fun initData() {
        mPWallet = PWallet.getUsingWallet()
        name?.text = mPWallet?.name
        val coinList = mPWallet!!.coinList
        doAsync {
            //获取最新的数据
            val localCoinList = where(
                "pwallet_id = ? and status = ?", String.valueOf(mPWallet!!.id),
                String.valueOf(Coin.STATUS_ENABLE)
            ).find(Coin::class.java, true)
            coinList.addAll(localCoinList)
            mCoinList.clear()
            mCoinList.addAll(localCoinList)
            for (coin in mCoinList) {
                val handleBalance = GoWallet.handleBalance(coin)
                coin.balance = handleBalance
            }
            runOnUiThread {
                if (ListUtils.isEmpty(localCoinList)) {
                    emptyView.visibility = View.VISIBLE
                } else {
                    emptyView.visibility = View.GONE
                }
                mWalletAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun initListener() {
        swl_layout.setOnRefreshListener {
            swl_layout.onRefreshComplete()
            initData()
        }
        more?.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            val `in` = Intent()
            `in`.setClass(requireActivity(), WalletDetailsActivity::class.java)
            `in`.putExtra(PWallet::class.java.simpleName, mPWallet)
            startActivityForResult(`in`, UPDATE_WALLET)
        }
        iv_back.setOnClickListener {
            requireActivity().finish()
        }
        topLeft.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            val intent = Intent()
            intent.setClass(requireActivity(), CaptureCustomActivity::class.java)
            intent.putExtra(
                CaptureCustomActivity.REQUST_CODE,
                CaptureCustomActivity.REQUESTCODE_HOME
            )
            startActivity(intent)
        }
        topRight.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            val intent = Intent()
            intent.setClass(requireActivity(), MyWalletsActivity::class.java)
            startActivity(intent)
        }
        titleLayout.setOnClickListener {
            if (ClickUtils.isFastClick(1000)) {
                recyclerView.smoothScrollToPosition(0)
            }
        }
    }

    companion object {

        const val UPDATE_WALLET = 1000
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onUpdateWalletNameEvent(event: UpdateWalletNameEvent) {
        if (event != null && event.needUpdate) {
            mPWallet = PWallet.getUsingWallet()
            name?.text = mPWallet?.name
        }
    }

    //回调 - 删除账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWalletDeleteEvent(event: WalletDeleteEvent) {
        if (mPWallet!!.id === event.walletId) {
            initData()
        }
    }

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMyWalletEvent(event: MyWalletEvent) {
        if (mPWallet != null && event.mPWallet != null && mPWallet!!.id !== event.mPWallet!!.id) {
            mPWallet = event.mPWallet
            PWallet.setUsingWallet(mPWallet)
            initData()
        }
    }

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTransactionsEvent(event: TransactionsEvent) {
        if (event != null) {
            val coin: Coin? = event.coin
            if (coin != null) {
                mTimer = Timer()
                mTimer!!.schedule(object : TimerTask() {
                    override fun run() {
                        val balance = GoWallet.handleBalance(coin)
                        if (timeCount == 3 || balance != coin.balance) {
                            cancel()
                            timeCount = 0
                        }
                        for (i in mCoinList.indices) {
                            val coinSign =
                                coin.name + coin.platform + coin.chain
                            val oldCoinSign =
                                mCoinList[i].name + mCoinList[i]
                                    .platform + mCoinList[i].chain
                            if (oldCoinSign == coinSign) {
                                mCoinList[i].balance = balance
                                runOnUiThread { mWalletAdapter!!.notifyItemChanged(i) }
                                break
                            }
                        }
                        timeCount++
                    }
                }, 2000, 2000)
            }
        }
    }

    //扫码回调
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onCaptureEvent(event: CaptureEvent) {
        val type: Int = event.type
        val text: kotlin.String = event.text
        val requstCode = event.requstCode
        if (type == CaptureCustomActivity.RESULT_SUCCESS && requstCode == CaptureCustomActivity.REQUESTCODE_HOME && !TextUtils.isEmpty(text)) {

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        if(isAdded) {
            if (mTimer != null) {
                mTimer!!.cancel()
            }
            if (balanceTimer != null) {
                balanceTimer!!.cancel()
            }
        }

    }
}