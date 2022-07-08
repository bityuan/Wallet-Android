package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityTransactionsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.TransactionFragment
import com.fzm.walletmodule.ui.widget.InQrCodeDialogView
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.DecimalUtils
import com.fzm.walletmodule.utils.GlideUtils
import com.fzm.walletmodule.utils.isFastClick
import com.king.zxing.util.CodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TransactionsActivity : BaseActivity() {

    private lateinit var transactionFragment0: TransactionFragment
    private lateinit var transactionFragment1: TransactionFragment
    private lateinit var transactionFragment2: TransactionFragment
    private var mDialogView: InQrCodeDialogView? = null
    private lateinit var coin: Coin
    private lateinit var pagerAdapter: Adapter
    private val binding by lazy { ActivityTransactionsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(R.color.color_333649)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCustomToobar(binding.bar.myToolbar, R.drawable.ic_back_white)
        initIntent()
        initView()
        initListener()
        initData()
    }

    override fun initIntent() {
        super.initIntent()
        coin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin
    }


    override fun initView() {
        setupViewPager()
    }

    override fun initListener() {
        super.initListener()
        binding.llOut.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
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
                    coin.address,
                    coin.icon
                )
            } else {
                mDialogView?.show()
            }
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
        binding.viewPager.adapter = pagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }


    override fun initData() {
        super.initData()
        title = if (coin.nickname.isNullOrEmpty()) coin.name else "${coin.name}(${coin.nickname})"
        binding.tvBalance.text = DecimalUtils.subZeroAndDot(coin.balance)
        binding.tvAddress.text = coin.address
        if (TextUtils.isEmpty(coin.icon)) {
            binding.ivErCode.setImageBitmap(CodeUtils.createQRCode(coin.address, 200))
        } else {
            Glide.with(this).load(coin.icon).into(binding.ivBName)
            GlideUtils.intoQRBitmap(this, coin.icon, binding.ivErCode, coin.address)
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
            val balance = GoWallet.handleBalance(coin)
            withContext(Dispatchers.Main) {
                coin.balance = balance
                binding.tvBalance.text = DecimalUtils.subZeroAndDot(coin.balance)
            }
        }
    }
}
