package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants.Companion.TRAN_STATE_KEY
import com.fzm.walletmodule.databinding.ActivityTransactionsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.TransactionFragment
import com.fzm.walletmodule.ui.widget.InQrCodeDialogView
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.DecimalUtils
import com.fzm.walletmodule.utils.isFastClick
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.zxing.util.CodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast

@Route(path = RouterPath.WALLET_TRANSACTIONS)
class TransactionsActivity : BaseActivity() {

    private lateinit var transactionFragment0: TransactionFragment
    private lateinit var transactionFragment1: TransactionFragment
    private lateinit var transactionFragment2: TransactionFragment
    private var mDialogView: InQrCodeDialogView? = null
    private lateinit var pagerAdapter: Adapter
    private val binding by lazy { ActivityTransactionsBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired(name = RouterPath.PARAM_COIN)
    var coin: Coin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(R.color.color_333649)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        setCustomToobar(binding.bar.myToolbar, R.drawable.ic_back_white)
        initView()
        initObserver()
        initListener()
        initData()
    }


    override fun initView() {
        coin?.let {
            if (it.name == "USDT") {
                binding.cbRecord.visibility = View.VISIBLE
                binding.tvLowUsd.visibility = View.VISIBLE
                val state = MMkvUtil.decodeBoolean(TRAN_STATE_KEY)
                binding.cbRecord.isChecked = state
            } else {
                binding.cbRecord.visibility = View.GONE
                binding.tvLowUsd.visibility = View.GONE
            }
        }
        setupViewPager()
        Glide.with(this).load(coin?.icon).into(binding.ivBName)
    }

    override fun initObserver() {
        super.initObserver()
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            ARouter.getInstance().build(RouterPath.WALLET_OUT)
                .withSerializable(RouterPath.PARAM_COIN, coin)
                .withString(RouterPath.PARAM_ADDRESS, scan).navigation()
        })
    }

    override fun initListener() {
        super.initListener()
        binding.llOut.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            coin?.let {
                if (it.getpWallet().type == PWallet.TYPE_ADDR_KEY) {
                    toast(getString(R.string.str_addr_no))
                    return@setOnClickListener
                }
            }

            ARouter.getInstance().build(RouterPath.WALLET_OUT)
                .withSerializable(RouterPath.PARAM_COIN, coin).navigation()
        }
        binding.llIn.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_IN)
                .withSerializable(RouterPath.PARAM_COIN, coin).navigation()
        }
        binding.tvAddress.setOnClickListener {
            ClipboardUtils.clip(this, binding.tvAddress.text.toString())
        }
        binding.ivErCode.setOnClickListener {
            if (mDialogView == null) {
                mDialogView = InQrCodeDialogView(
                    this,
                    coin?.address
                )
            } else {
                mDialogView?.show()
            }
        }

        binding.ivTScan.setOnClickListener {
            coin?.let {
                if (it.getpWallet().type == PWallet.TYPE_ADDR_KEY) {
                    toast(getString(R.string.str_addr_no))
                    return@setOnClickListener
                }
            }
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (coin?.name == "USDT") {
                    when (tab.position) {
                        0 -> transactionFragment0.doAsset()
                        1 -> transactionFragment1.doAsset()
                        2 -> transactionFragment2.doAsset()
                    }
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }

        })

        binding.cbRecord.setOnCheckedChangeListener { compoundButton, check ->
            if (coin?.name == "USDT") {
                MMkvUtil.encode(TRAN_STATE_KEY, check)
                when (binding.viewPager.currentItem) {
                    0 -> transactionFragment0.doAsset()
                    1 -> transactionFragment1.doAsset()
                    2 -> transactionFragment2.doAsset()
                }
            }

        }
    }


    private fun setupViewPager() {
        coin?.let {
            transactionFragment0 = TransactionFragment.newInstance(0, it)
            transactionFragment1 = TransactionFragment.newInstance(1, it)
            transactionFragment2 = TransactionFragment.newInstance(2, it)
        }

        binding.viewPager.offscreenPageLimit = 2
        pagerAdapter = Adapter(supportFragmentManager)
        pagerAdapter.addFragment(transactionFragment0, getString(R.string.trans_all))
        pagerAdapter.addFragment(transactionFragment1, getString(R.string.home_transfer))
        pagerAdapter.addFragment(transactionFragment2, getString(R.string.home_receipt))
        binding.viewPager.adapter = pagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }


    override fun initData() {
        super.initData()
        coin?.let {
            title = if (it.nickname.isNullOrEmpty()) it.name else "${it.name}(${it.nickname})"
            binding.tvBalance.text = DecimalUtils.subZeroAndDot(it.balance)
            binding.tvAddress.text = it.address
            binding.ivErCode.setImageBitmap(CodeUtils.createQRCode(it.address, 200))
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

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitles[position]
        }
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.bar.myToolbar.title = ""
        binding.bar.tvTitle.text = title
    }


    fun doRefreshBalance() {
        lifecycleScope.launch(Dispatchers.IO) {
            coin?.let {
                val balance = GoWallet.handleBalance(it)
                withContext(Dispatchers.Main) {
                    it.balance = balance
                    it.update(it.id)
                    binding.tvBalance.text = DecimalUtils.subZeroAndDot(it.balance)
                }
            }

        }
    }
}
