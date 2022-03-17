package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.CaptureEvent
import com.fzm.walletmodule.event.TransactionsEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.TransactionFragment
import com.fzm.walletmodule.ui.widget.InQrCodeDialogView
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.DecimalUtils
import com.fzm.walletmodule.utils.GlideUtils
import com.fzm.walletmodule.utils.isFastClick
import com.google.android.material.tabs.TabLayout
import com.king.zxing.util.CodeUtils
import kotlinx.android.synthetic.main.activity_transactions.*
import kotlinx.android.synthetic.main.layout_bar.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.uiThread
import walletapi.Walletapi


class TransactionsActivity : BaseActivity() {

    private lateinit var transactionFragment0: TransactionFragment
    private lateinit var transactionFragment1: TransactionFragment
    private lateinit var transactionFragment2: TransactionFragment
    private var mDialogView: InQrCodeDialogView? = null
    private lateinit var coin: Coin
    private lateinit var pagerAdapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(R.color.color_333649)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)
        setCustomToobar(my_toolbar, R.drawable.ic_back_white)
        initIntent()
        initView()
        initListener()
        initData()
    }

    override fun onResume() {
        super.onResume()
        doRefreshBalance()
    }


    override fun initIntent() {
        super.initIntent()
        coin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin
    }


    fun needExchange(): Boolean {
        if (Walletapi.TypeTrxString == coin.chain && USDT == coin.name) {
            return true
        } else if (Walletapi.TypeBnbString == coin.chain && USDT == coin.name) {
            return true
        } else if (Walletapi.TypeBnbString == coin.chain && BTY == coin.name) {
            return true
        } else if (Walletapi.TypeBtyString == coin.chain && BTY == coin.name) {
            return true
        }

        return false
    }


    override fun initView() {
        setupViewPager()
        iv_exchange.visibility = if (needExchange()) View.VISIBLE else View.GONE
    }

    override fun initListener() {
        super.initListener()
        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabTextView(tab, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabTextView(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
        ll_out.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            startActivity<OutActivity>(
                Pair(Constants.FROM, OutActivity.FROM_TRANSACTION),
                Coin::class.java.simpleName to coin
            )
        }
        iv_exchange.setOnClickListener {
            startActivity<ExchangeActivity>(
                Coin::class.java.simpleName to coin
            )
        }
        ll_in.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            startActivity<InActivity>(Coin::class.java.simpleName to coin)
        }
        tv_address.setOnClickListener {
            ClipboardUtils.clip(this, tv_address.text.toString())
        }
        iv_er_code.setOnClickListener {
            if (mDialogView == null) {
                mDialogView = InQrCodeDialogView(this, coin.address, coin.icon)
            } else {
                mDialogView?.show()
            }
        }
        iv_t_scan.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            startActivity<CaptureCustomActivity>(CaptureCustomActivity.REQUST_CODE to CaptureCustomActivity.REQUESTCODE_TRANSACTIONS)
        }
    }


    private fun setupViewPager() {
        transactionFragment0 = TransactionFragment.newInstance(0, coin)
        transactionFragment1 = TransactionFragment.newInstance(1, coin)
        transactionFragment2 = TransactionFragment.newInstance(2, coin)
        pagerAdapter = Adapter(supportFragmentManager)
        pagerAdapter.addFragment(transactionFragment0, getString(R.string.trans_all))
        pagerAdapter.addFragment(transactionFragment1, getString(R.string.home_transfer))
        pagerAdapter.addFragment(transactionFragment2, getString(R.string.home_receipt))
        view_pager.adapter = pagerAdapter
        tab_layout.setupWithViewPager(view_pager)
        setCustomViews()
        updateTabTextView(tab_layout.getTabAt(0)!!, true)

    }

    private fun setCustomViews() {
        for (i in 0 until tab_layout.tabCount) {
            val tab = tab_layout.getTabAt(i)
            tab?.customView = getTabView(tab)
        }
    }

    private fun getTabView(tab: TabLayout.Tab?): View {
        val view = LayoutInflater.from(this).inflate(R.layout.tab_view_transaction, null)
        val tvTab = view.findViewById<TextView>(R.id.tv_tab)
        tvTab.text = tab?.text.toString()
        return view
    }

    private fun updateTabTextView(tab: TabLayout.Tab, isSelect: Boolean) {
        if (tab.customView != null) {
            val tvTab = tab.customView!!.findViewById<TextView>(R.id.tv_tab)

            if (isSelect) {
                tvTab.setBackgroundResource(R.drawable.shape_gray_6)
                tvTab.setTextColor(resources.getColor(R.color.white))
            } else {
                tvTab.setBackgroundResource(R.color.white)
                tvTab.setTextColor(resources.getColor(R.color.gray_8e))
            }
        }

    }


    override fun initData() {
        super.initData()
        title = if (coin.nickname.isNullOrEmpty()) coin.name else "${coin.name}(${coin.nickname})"
        tv_balance.text = DecimalUtils.subZeroAndDot(coin.balance)
        tv_address.text = coin.address
        if (TextUtils.isEmpty(coin.icon)) {
            iv_er_code.setImageBitmap(CodeUtils.createQRCode(coin.address, 200))
        } else {
            Glide.with(this).load(coin.icon).into(iv_b_name)
            GlideUtils.intoQRBitmap(this, coin.icon, iv_er_code, coin.address)
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: CaptureEvent) {
        val type = event.type
        val text = event.text
        if (type == CaptureCustomActivity.RESULT_SUCCESS) {
            if (event.requstCode == CaptureCustomActivity.REQUESTCODE_TRANSACTIONS) {
                try {
                    coin.scanAddress = text
                    startActivity<OutActivity>(
                        Constants.FROM to OutActivity.FROM_SCAN,
                        Coin::class.java.simpleName to coin
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitles[position]
        }
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        my_toolbar.title = ""
        tv_title.text = title
    }


    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTransactionsEvent(event: TransactionsEvent) {
        Handler().postDelayed({
            transactionFragment0.refresh()
            transactionFragment1.refresh()
        }, 2000)
    }

    fun doRefreshBalance() {
        doAsync {
            val balance = GoWallet.handleBalance(coin)
            uiThread {
                coin.balance = balance
                tv_balance.text = DecimalUtils.subZeroAndDot(coin.balance)
            }
        }
    }


    companion object {
        val USDT = "USDT"
        val YCC = "YCC"
        val BTY = "BTY"
    }
}
