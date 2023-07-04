package com.fzm.walletdemo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityMainBinding
import com.fzm.walletdemo.ui.fragment.ExploreFragment
import com.fzm.walletdemo.ui.fragment.HomeFragment
import com.fzm.walletdemo.ui.fragment.MyFragment
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.WalletIndexFragment
import com.fzm.walletmodule.update.UpdateUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.LitePal.count

@Route(path = RouterPath.APP_MAIN)
class MainActivity : BaseActivity() {
    private var exploreFragment: ExploreFragment? = null
    private var mWalletIndexFragment: WalletIndexFragment? = null
    private var mHomeFragment: HomeFragment? = null
    private var myFragment: MyFragment? = null
    private val walletViewModel: WalletViewModel by inject(walletQualifier)

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initObserver()
        Constants.setCoins(DEFAULT_COINS)
        gotoUpdate()
    }


    private fun gotoUpdate() {
        walletViewModel.getUpdate.observe(this, Observer {
            if (it.isSucceed()) {
                it.data()?.let {
                    UpdateUtils(this).update(it, supportFragmentManager, this, true)
                }
            } else {
                toast(it.error())
            }

        })
        walletViewModel.getUpdate()
    }

    private val DEFAULT_COINS = listOf(
        Coin().apply {
            name = "BTC"
            chain = "BTC"
            platform = "btc"
            netId = "89"
        },
        Coin().apply {
            name = "ETH"
            chain = "ETH"
            platform = "ethereum"
            netId = "90"
        },
        Coin().apply {
            name = "USDT"
            chain = "ETH"
            platform = "ethereum"
            netId = "288"
        },
    )

    override fun initView() {
        setTabSelection(0)
        binding.lyHome.setOnClickListener {
            setTabSelection(0)
        }
        binding.lyExplore.setOnClickListener {
            setTabSelection(1)
        }
        binding.lyMy.setOnClickListener {
            setTabSelection(2)
        }
        binding.lyTian.setOnClickListener {
            val walletCount = LitePal.count(PWallet::class.java)
            if (walletCount == 0) {
                toast("请先在资产中创建账户")
                return@setOnClickListener
            }
            setTabSelection(3)
        }
        binding.lyTp.setOnClickListener {
            val walletCount = LitePal.count(PWallet::class.java)
            if (walletCount == 0) {
                toast("请先在资产中创建账户")
                return@setOnClickListener
            }
            setTabSelection(4)
        }
    }


    var currentTab: ViewGroup? = null
    public fun setTabSelection(index: Int) {
        // 开启一个Fragment事务
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(fragmentTransaction)
        when (index) {
            0 -> {
                currentTab?.isSelected = false
                binding.lyHome.isSelected = true
                val count: Int = count(PWallet::class.java)
                if (count > 0) {
                    showHomeFragment(fragmentTransaction)
                } else {
                    showWalletIndexFragment(fragmentTransaction)
                }
                currentTab = binding.lyHome
            }
            1 -> {
                currentTab?.isSelected = false
                binding.lyExplore.isSelected = true
                showExploreFragment(fragmentTransaction)
                currentTab = binding.lyExplore
            }
            2 -> {
                currentTab?.isSelected = false
                binding.lyMy.isSelected = true
                showMyFragment(fragmentTransaction)
                currentTab = binding.lyMy
            }
        }

    }

    override fun initObserver() {
        super.initObserver()
        LiveEventBus.get<Long>(LIVE_KEY_WALLET).observeSticky(this, Observer {
            it?.let {
                setTabSelection(0)
            }
        })
    }

    private fun hideFragments(transaction: FragmentTransaction) {
        mWalletIndexFragment?.let { transaction.hide(it) }
        mHomeFragment?.let { transaction.hide(it) }
        exploreFragment?.let { transaction.hide(it) }
        myFragment?.let { transaction.hide(it) }
    }


    private fun showWalletIndexFragment(fragmentTransaction: FragmentTransaction) {
        if (mWalletIndexFragment != null) {
            fragmentTransaction.show(mWalletIndexFragment!!)
        } else {
            mWalletIndexFragment = WalletIndexFragment()
            fragmentTransaction.add(
                R.id.fl_tabcontent,
                mWalletIndexFragment!!,
                "showWalletIndexFragment"
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
                "showHomeFragment"
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
                "showExploreFragment"
            )

        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun showMyFragment(fragmentTransaction: FragmentTransaction) {
        if (myFragment != null) {
            fragmentTransaction.show(myFragment!!)
        } else {
            myFragment = MyFragment()
            fragmentTransaction.add(
                R.id.fl_tabcontent,
                myFragment!!,
                "showMyFragment"
            )

        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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


    //app崩溃导致fragment重叠问题处理

    private var position = 0 //记录Fragment的位置

    override fun onSaveInstanceState(outState: Bundle) {
        logDebug("onSaveInstanceState")
        // 保存用户自定义的状态
        outState.putInt("position", position);
        //调用父类交给系统处理，这样系统能保存视图层次结构状态
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        logDebug("onRestoreInstanceState")
        // 总是调用超类，以便它可以恢复视图层次超级
        super.onRestoreInstanceState(savedInstanceState);
        //从已保存的实例中恢复状态成员
        position = savedInstanceState.getInt("position");
        setTabSelection(position);
    }
}
