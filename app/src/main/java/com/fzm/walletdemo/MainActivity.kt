package com.fzm.walletdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.alpha.EmptyWallet
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.databinding.ActivityMainBinding
import com.fzm.walletdemo.fragment.ExploreFragment
import com.fzm.walletdemo.fragment.HomeFragment
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.MainCloseEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.ui.fragment.WalletIndexFragment
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.flow.collect
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.toast
import org.litepal.LitePal.count
import walletapi.Walletapi

@Route(path = RouterPath.APP_MAIN)
class MainActivity : BaseActivity() {
    private var walletFragment: WalletFragment? = null
    private var exploreFragment: ExploreFragment? = null
    private var mWalletIndexFragment: WalletIndexFragment? = null
    private var mHomeFragment: HomeFragment? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        EventBus.getDefault().register(this)
        initView()
        setTabSelection(0)
        initObserver()
        Constants.setCoins(DEFAULT_COINS)
        lifecycleScope.launchWhenResumed {
            BWallet.get().current.collect {
                if (it is EmptyWallet) {
                    setTabSelection(0)
                }
            }
        }

    }

    val DEFAULT_COINS = listOf(
        Coin().apply {
            name = "BTC"
            chain = "BTC"
            platform = "btc"
            netId = "89"
        },
        Coin().apply {
            name = "BTY"
            chain = "BTY"
            platform = "bty"
            netId = "154"
        },
        Coin().apply {
            name = "ETH"
            chain = "ETH"
            platform = "ethereum"
            netId = "90"
        },

        )

    override fun initView() {
        binding.bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.fragment_home -> {
                    Log.e("MainAc", "fragment_home")
                    setTabSelection(0)
                }
                R.id.fragment_explore -> {
                    Log.e("MainAc", "fragment_explore")
                    setTabSelection(1)
                }
            }
            true
        }
    }


    private fun setTabSelection(index: Int) {
        // 开启一个Fragment事务
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(fragmentTransaction)
        when (index) {
            0 -> {
                val count: Int = count(PWallet::class.java)
                if (count > 0) {
                    showHomeFragment(fragmentTransaction)
                } else {
                    showWalletIndexFragment(fragmentTransaction)
                }
            }
            1 -> {
                //showWalletFragment(fragmentTransaction)
                showExploreFragment(fragmentTransaction)
            }
        }

    }

    override fun initObserver() {
        super.initObserver()
        LiveEventBus.get<PWallet>(LIVE_KEY_WALLET).observeSticky(this, Observer {
            it?.let {
                setTabSelection(0)
            }
        })
    }

    private fun hideFragments(transaction: FragmentTransaction) {
        walletFragment?.let { transaction.hide(it) }
        mWalletIndexFragment?.let { transaction.hide(it) }
        mHomeFragment?.let { transaction.hide(it) }
        exploreFragment?.let { transaction.hide(it) }
    }


    private fun showWalletFragment(fragmentTransaction: FragmentTransaction) {
        if (walletFragment != null) {
            fragmentTransaction.show(walletFragment!!)
        } else {
            walletFragment = WalletFragment()
            fragmentTransaction.add(R.id.fl_tabcontent, walletFragment!!, "walletFragment")
        }
        fragmentTransaction.commitAllowingStateLoss()
    }


    private fun showWalletIndexFragment(fragmentTransaction: FragmentTransaction) {
        if (mWalletIndexFragment != null) {
            fragmentTransaction.show(mWalletIndexFragment!!)
        } else {
            mWalletIndexFragment = WalletIndexFragment()
            fragmentTransaction.add(
                R.id.fl_tabcontent,
                mWalletIndexFragment!!,
                "WalletIndexFragment"
            )

        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun showHomeFragment(fragmentTransaction: FragmentTransaction) {
        if (mHomeFragment != null) {
            fragmentTransaction.show(mHomeFragment!!)
        } else {
            mHomeFragment = HomeFragment()
            fragmentTransaction.add(
                R.id.fl_tabcontent,
                mHomeFragment!!,
                "HomeFragment"
            )

        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun showExploreFragment(fragmentTransaction: FragmentTransaction) {
        if (exploreFragment != null) {
            fragmentTransaction.show(exploreFragment!!)
        } else {
            exploreFragment = ExploreFragment()
            fragmentTransaction.add(
                R.id.fl_tabcontent,
                exploreFragment!!,
                "HomeFragment"
            )

        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setTabSelection(0)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MainCloseEvent) {
        setTabSelection(0)
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private var lastExitTime: Long = 0

    override fun onBackPressed() {
        val l = System.currentTimeMillis()
        if (l - lastExitTime > 2000) {
            toast("再按一次退出程序")
            lastExitTime = l
        } else {
            finish()
        }
    }
}