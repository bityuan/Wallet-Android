package com.fzm.walletdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.alpha.EmptyWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.fragment.HomeFragment
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.InitPasswordEvent
import com.fzm.walletmodule.event.MainCloseEvent
import com.fzm.walletmodule.event.MyWalletEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.ui.fragment.WalletIndexFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.collect
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.litepal.LitePal.count

class MainActivity : BaseActivity() {
    private var walletFragment: WalletFragment? = null
    private var mWalletIndexFragment: WalletIndexFragment? = null
    private var mHomeFragment: HomeFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        initView()
        setTabSelection(0)
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
        }, Coin().apply {
            name = "ETH"
            chain = "ETH"
            platform = "ethereum"
            netId = "90"
        },Coin().apply {
            name = "YCC"
            chain = "BTC"
            platform = "btc"
            netId = "727"
        },Coin().apply {
            name = "YCC"
            chain = "ETH"
            platform = "ethereum"
            netId = "155"
        }

    )

    override fun initView() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
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
            }
        }

    }

    private fun hideFragments(transaction: FragmentTransaction) {
        walletFragment?.let { transaction.hide(it) }
        mWalletIndexFragment?.let { transaction.hide(it) }
        mHomeFragment?.let { transaction.hide(it) }
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setTabSelection(0)
    }


    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMyWalletEvent(event: MyWalletEvent) {
        if (null == event.mPWallet) {
            return
        } else {
            setTabSelection(0)
        }

        if (!event.isChoose) {
            val privkey = BWallet.get().getBtyPrikey()
            Log.v("tag", privkey + "")
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInitPasswordEvent(event: InitPasswordEvent) {
        val password = event.password
        Log.v("zx", password)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MainCloseEvent) {
        setTabSelection(0)
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}