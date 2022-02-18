package com.fzm.walletmodule.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.uiThread
import org.litepal.LitePal.where
import java.lang.String
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class WalletFragment : BaseFragment() {
    private var mWalletAdapter: WalletAdapter? = null
    private var mPWallet: PWallet? = null
    private val mCoinList = CopyOnWriteArrayList<Coin>()
    private var more: ImageView? = null
    private var addCoin: ImageView? = null
    private var name: TextView? = null
    private var money: TextView? = null

    private var job: Job? = null

    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            // FIXME: 最好使用lifecycle2.4.0提供的repeatOnLifecycle方法
            job = lifecycleScope.launch {
                BWallet.get().getCoinBalance(0, Constants.DELAYED_TIME, true)
                    .collect {
                      //  Log.e("wallet","getCoinBalance")
                        mCoinList.clear()
                        mCoinList.addAll(it)
                        //mCoinList.sort()
                        mWalletAdapter?.notifyDataSetChanged()
                        money?.text = DecimalUtils.subWithNum(it.sumOf { c -> c.totalAsset }, 2)
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
                if (isFastClick()){
                    return
                }
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
        val mHeaderView =
            LayoutInflater.from(activity).inflate(R.layout.view_header_wallet, null, false)
        more = mHeaderView.findViewById<ImageView>(R.id.more)
        name = mHeaderView.findViewById<TextView>(R.id.name)
        money = mHeaderView.findViewById<TextView>(R.id.money)
        addCoin = mHeaderView.findViewById<ImageView>(R.id.addCoin)
        recyclerView.addHeaderView(mHeaderView)
    }

    override fun initData() {
        lifecycleScope.launchWhenResumed {
            BWallet.get().current.collect {
                name?.text = it.walletInfo.name
            }
        }
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
            `in`.putExtra(PWallet.PWALLET_ID, BWallet.get().getCurrentWallet()?.id ?: 0L)
            startActivityForResult(`in`, UPDATE_WALLET)
        }
        iv_back.setOnClickListener {
            requireActivity().finish()
        }
        addCoin?.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            startActivity<AddCoinActivity>()
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



    //回调 - 添加币种
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAddCoinEvent(event: AddCoinEvent) {
        mPWallet = WalletUtils.getUsingWallet()
        doAsync {

            //获取最新的数据
            val localCoinList = where(
                "pwallet_id = ? and status = ?",
                String.valueOf(mPWallet?.id),
                String.valueOf(Coin.STATUS_ENABLE)
            ).find(
                Coin::class.java, true
            )
            localCoinList.sort()
            mPWallet?.coinList?.addAll(localCoinList)
            mCoinList.clear()
            mCoinList.addAll(localCoinList)
            uiThread {
                mWalletAdapter?.notifyDataSetChanged()
                job?.start()
            }
        }
    }

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTransactionsEvent(event: TransactionsEvent) {
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