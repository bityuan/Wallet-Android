package com.fzm.walletmodule.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.totalAsset
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.WalletAdapter
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.*
import com.fzm.walletmodule.ui.activity.*
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class WalletFragment : BaseFragment() {
    private var mWalletAdapter: WalletAdapter? = null
    private var mHeaderView: View? = null
    private var mPWallet: PWallet? = null
        set(value) {
            BWallet.get().changeWallet(value)
            field = value
        }
    private val mCoinList = CopyOnWriteArrayList<Coin>()
    private var more: ImageView? = null
    private var name: TextView? = null
    private var money: TextView? = null
    private var timeCount = 0

    private var job: Job? = null

    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            // FIXME: 最好使用lifecycle2.4.0提供的repeatOnLifecycle方法
            job = lifecycleScope.launch {
                BWallet.get().getCoinBalance(0, Constants.DELAYED_TIME, true)
                    .collect {
                        mCoinList.clear()
                        mCoinList.addAll(it)
                        mWalletAdapter?.notifyDataSetChanged()
                        money?.text = DecimalUtils.subWithNum(it.sumOf { c-> c.totalAsset }, 2)
                    }
            }
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            job?.cancel()
        }
    }

    override fun getLayout(): Int {
        return R.layout.fragment_wallet
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        initView()
        initHeaderView()
        initData()
        initListener()
        viewLifecycleOwner.lifecycle.addObserver(observer)

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
        money = mHeaderView?.findViewById<TextView>(R.id.money)
        recyclerView.addHeaderView(mHeaderView)
    }

    override fun initData() {
        mPWallet = WalletUtils.getUsingWallet()
        name?.text = mPWallet?.name
    }

    override fun initListener() {
        swl_layout.setOnRefreshListener {
            swl_layout.onRefreshComplete()
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
            mPWallet = WalletUtils.getUsingWallet()
            name?.text = mPWallet?.name
        }
    }

    //回调 - 删除账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWalletDeleteEvent(event: WalletDeleteEvent) {
        if (mPWallet?.id == event.walletId) {
            initData()
        }
    }

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMyWalletEvent(event: MyWalletEvent) {
        if (event.mPWallet != null && mPWallet?.id != event.mPWallet?.id) {
            mPWallet = event.mPWallet
            name?.text = mPWallet?.name
        }
    }

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTransactionsEvent(event: TransactionsEvent) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }

    }
}