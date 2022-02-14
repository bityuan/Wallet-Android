package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.AddCoinEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.HomeCoinFragment
import com.fzm.walletmodule.ui.fragment.TokenCoinFragment
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.WalletUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.android.synthetic.main.activity_add_coin.*
import kotlinx.android.synthetic.main.activity_my_wallets.*
import kotlinx.android.synthetic.main.layout_search_coin.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
import org.litepal.LitePal.select
import org.litepal.LitePal.where
import walletapi.HDWallet
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class AddCoinActivity : BaseActivity() {
    private val GUIDE_ADD_COIN = "guide_add_coin5"
    private val data: ArrayList<Coin> = ArrayList<Coin>()
    private val mStatusMap = HashMap<String, Int>()
    private val mCoinsMap = HashMap<String, Coin>()
    private lateinit var homeData: List<Coin>
    private lateinit var mPWallet: PWallet
    private var mCommonAdapter: CommonAdapter<Coin>? = null
    private var homeCoinFragment: HomeCoinFragment? = null
    private val mTabTradeType = mutableListOf<Fragment>()
    private val tabBeans: ArrayList<AddCoinTabBean> = ArrayList<AddCoinTabBean>()
    val titles = ArrayList<String>()
    private var chain: String = ""
    private var platform: String = ""
    private var keyWord = ""
    private var page_temp = 1
    private var mAdapter: FragmentPagerAdapter? = null
    private var page = 0
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_coin)
        mDelaySearchHandler = DelaySearchHandler()
        initView()
        initTab()
        initListener()
        initData()

    }

    override fun initView() {
        ll_search_coin.visibility = View.GONE
        ctl_addcoin.visibility = View.VISIBLE
        swipe_target.layoutManager = LinearLayoutManager(this)
        mCommonAdapter = object : CommonAdapter<Coin>(this, R.layout.listitem_addcoin, data) {
            override fun convert(holder: ViewHolder, coin: Coin, position: Int) {
                val ivAddIcon = holder.getView<ImageView>(R.id.iv_addcoin_icon)
                Glide.with(mContext)
                    .load(coin.icon)
                    .into(ivAddIcon)
                holder.setText(R.id.tv_addcoin_name, coin.uiName)
                holder.setText(R.id.tv_addcoin_name_cn, coin.nickname)
                val status: Int? = getStatus(coin.netId)
                if (null == status) {
                    coin.status = 0
                } else {
                    coin.status = status
                    coin.id = getCoin(coin.netId)!!.id
                }
                holder.setImageResource(
                    R.id.iv_add_remove_coin,
                    if (coin.status == Coin.STATUS_ENABLE) R.mipmap.icon_removecoin else R.mipmap.icon_addcoin
                )
                holder.setOnClickListener(R.id.iv_add_remove_coin) {
                    Log.e(
                        "pass",
                        "password = " + mPWallet.password
                            .toString() + "   id =" + mPWallet.id
                    )
                    if (coin.status == Coin.STATUS_ENABLE) {
                        updateCoin(coin, false, false)
                    } else {
                        //0表示本地没有，那就是保存，1或者-1表示本地有，就更新
                        val isSave = coin.status == 0
                        if (isSave) {
                            checkCoin(coin)
                        } else {
                            updateCoin(coin, false, true)
                        }
                    }
                }
            }
        }
        swipe_target.adapter = mCommonAdapter
    }

    private fun initTab() {
        titles.clear()
        titles.add(getString(R.string.add_coin_tab_one))
        homeCoinFragment = HomeCoinFragment()
        mTabTradeType.clear()
        mTabTradeType.add(homeCoinFragment!!)
        mAdapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return mTabTradeType[position]
            }

            override fun getCount(): Int {
                return mTabTradeType.size
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return titles[position]
            }
        }
        vp_addcoins.offscreenPageLimit = 3
        vp_addcoins.adapter = mAdapter
        tl_addcoins.setupWithViewPager(vp_addcoins)
        vp_addcoins.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    homeCoinFragment!!.refresh()
                } else {
                    (mTabTradeType[position] as TokenCoinFragment).refresh()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }


    override fun initData() {
        showLoading()
        mPWallet = WalletUtils.getUsingWallet()
        homeData = where(
            "pwallet_id = ?",
            java.lang.String.valueOf(mPWallet.id)
        ).find(Coin::class.java, true)

        if (ListUtils.isEmpty(homeData)) {
            chain = ""
            platform = ""
        } else {
            chain = if (homeData[0] != null) homeData[0].chain else ""
            platform = if (homeData[0] != null) homeData[0].platform else ""
        }
        for (coin in homeData) {
            if (coin.netId != null) {
                mStatusMap[coin.netId] = coin.status
                mCoinsMap[coin.netId] = coin
            }
        }
        walletViewModel.getTabData()
    }

    override fun initListener() {
        et_search.doOnTextChanged { text, start, before, count ->
            val string: String = text.toString()
            if (mDelaySearchHandler!!.hasMessages(MSG_SEARCH)) {
                mDelaySearchHandler!!.removeMessages(MSG_SEARCH)
            }
            keyWord = string.trim { it <= ' ' }
            mDelaySearchHandler!!.sendEmptyMessageDelayed(MSG_SEARCH, 500)
        }
        swipeLayout.setOnRefreshListener {
            swipe_target.visibility = View.VISIBLE
            // 保存页码，方便请求列表数据失败后恢复页码
            page_temp = page
            page = 1
            walletViewModel.searchCoinList(
                page,
                Constants.PAGE_LIMIT.toInt(),
                keyWord,
                "",
                ""
            )

        }
        swipe_target.setOnLoadMoreListener {
            swipe_target.visibility = View.VISIBLE
            // 保存页码，方便请求列表数据失败后恢复页码
            page_temp = page
            page++
            walletViewModel.searchCoinList(
                page,
                Constants.PAGE_LIMIT.toInt(),
                keyWord,
                "",
                ""
            )
        }
        iv_add_coin_back.setOnClickListener {
            finish()
        }
        ll_search.setOnClickListener {
            showSearch()
        }
        tv_add_coin_cancel.setOnClickListener { hideSearch() }

        walletViewModel.getTabData.observe(this, Observer {
            dismiss()
            if (it.isSucceed()) {
                //        tabBeans.addAll(coinList);
                tabBeans.clear()
                val coinList = it.data()
                for (coinTabBean in coinList!!) {
                    if (ListUtils.isEmpty(coinTabBean.items)) {
                        continue
                    }
                    val localBean = AddCoinTabBean()
                    val localList = ArrayList<Coin>()
                    localBean.items = localList
                    for (coin in coinTabBean.items!!) {
                        localList.add(coin)
                    }
                    if (!ListUtils.isEmpty(localList)) {
                        titles.add(coinTabBean.name)
                        tabBeans.add(localBean)
                    }
                }
                for (i in tabBeans.indices) {
                    val fragment = TokenCoinFragment.newInstance(tabBeans[i].items!!)
                    mTabTradeType.add(fragment)
                }
                mAdapter!!.notifyDataSetChanged()
            } else {
                toast(it.error())
            }
        })

        walletViewModel.searchCoinList.observe(this, Observer {
            if (it.isSucceed()) {
                val coinList = it.data()
                tv_search_tip.visibility = View.GONE
                feedBackLayout.visibility = View.GONE
                val list: List<Coin> = coinList!!
                if (page == 1) {
                    data.clear()
                }
                //这次请求到的有数据
                if (list.isNotEmpty()) {
                    if (data.size <= 0) {
                        data.addAll(list)
                        swipeLayout.visibility = View.VISIBLE
                    } else {
                        data.addAll(list)
                    }
                } else { //本次请求没有数据
                    // 并且本来的列表也没有数据，说明数据为空。
                    if (data.size <= 0) {
                        swipeLayout.visibility = View.GONE
                    } else { //原始有数据，说明没有可以加载的数据。
                        // 没有更多数据了，恢复页码（否则第一次加载更多没有更多数据是请求第2页，第二次加载更多没有更多数据是请求第3页）
                        page = page_temp
                        swipe_target.setHasLoadMore(true)
                    }
                    if (page == 1) {
                        feedBackLayout.visibility = View.VISIBLE
                    }
                }
                swipeLayout.isRefreshing = false
                swipe_target.setHasLoadMore(false)
                if (data.size < page * Constants.PAGE_LIMIT) {
                    swipe_target.setHasLoadMore(false)
                } else {
                    swipe_target.setHasLoadMore(true)
                }
                mCommonAdapter!!.notifyDataSetChanged()
            } else {
                Log.e("addCoin","请求失败${it.error()}")
                toast(it.error())
            }
        })
    }

    private fun showSearch() {
        et_search.requestFocus()
        ll_search.visibility = View.GONE
        ll_show_search.visibility = View.VISIBLE
        ctl_addcoin.visibility = View.GONE
        ll_search_coin.visibility = View.VISIBLE
        showKeyboard(et_search)
        tv_search_tip.visibility = View.VISIBLE

    }

    private fun hideSearch() {
        ll_search.visibility = View.VISIBLE
        ll_show_search.visibility = View.GONE
        hideKeyboard(et_search)
        et_search.setText("")
        ctl_addcoin.visibility = View.VISIBLE
        ll_search_coin.visibility = View.GONE
        feedBackLayout.visibility = View.GONE
    }


    //不返回列表，连续添加BTY-LM-SG和YX，后添加的币种地址为空，（原因是homedata是一打开添加币种页面就查询了，如果没返回，那么homeData就没更新，所以查不到主链）
    private fun updateCoin(coin: Coin, save: Boolean, visible: Boolean) {
        homeData = where(
            "pwallet_id = ?",
            java.lang.String.valueOf(mPWallet.id)
        ).find(Coin::class.java, true)
        coin.status = if (visible) Coin.STATUS_ENABLE else Coin.STATUS_DISABLE
        mStatusMap[coin.netId] = coin.status
        mCoinsMap[coin.netId] = coin
        for (homeCoin in homeData) {
            if (TextUtils.equals(coin.chain, homeCoin.chain)) {
                coin.address = homeCoin.address
                coin.pubkey = homeCoin.pubkey
                coin.setPrivkey(homeCoin.encPrivkey)
                break
            }
        }
        if (save) {
            //新增
            coin.setpWallet(mPWallet)
            coin.sort = homeData.size
            coin.save()
        } else {
            //更新
            coin.update(coin.id)
        }
        mCommonAdapter!!.notifyDataSetChanged()
        homeCoinFragment!!.refresh()
        EventBus.getDefault().post(AddCoinEvent())
    }

    private fun checkCoin(coin: Coin) {
        if (!TextUtils.isEmpty(coin.chain)) {
            val chainCoin = select().where(
                "chain = ? and pwallet_id = ?",
                coin.chain,
              mPWallet.id.toString()
            ).findFirst(
                Coin::class.java
            )
            if (chainCoin == null) {
                showPwdDialog(coin)
            } else {
                updateCoin(coin, true, true)
            }
        }
    }

    private fun showPwdDialog(coin: Coin) {
        val editDialogFragment = EditDialogFragment()
        editDialogFragment.setTitle(getString(R.string.my_wallet_detail_password))
        editDialogFragment.setHint(getString(R.string.my_wallet_detail_password))
        editDialogFragment.setAutoDismiss(false)
        editDialogFragment.setType(1)
            .setRightButtonStr(getString(R.string.ok))
            .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                override fun onLeftButtonClick(v: View?) {}
                override fun onRightButtonClick(v: View?) {
                    val etInput: EditText = editDialogFragment.etInput
                    val value = etInput.text.toString()
                    if (TextUtils.isEmpty(value)) {
                        ToastUtils.show(
                            this@AddCoinActivity,
                            getString(R.string.rsp_dialog_input_password)
                        )
                        return
                    }
                    editDialogFragment.dismiss()
                    handlePasswordAfter(coin, value)
                }
            })
        editDialogFragment.showDialog("tag", supportFragmentManager)
    }

    fun handlePasswordAfter(coin: Coin, password: String) {
        showLoading()
        doAsync {
            try {
                val bPassword: ByteArray? = GoWallet.encPasswd(password)
                val mnem: String = GoWallet.decMenm(bPassword!!, mPWallet.mnem)
                if (!TextUtils.isEmpty(mnem)) {
                    val hdWallet: HDWallet? = GoWallet.getHDWallet(coin.chain, mnem)
                    val address = hdWallet!!.newAddress_v2(0)
                    val pubkey = hdWallet.newKeyPub(0)
                    val pubkeyStr: String = GoWallet.encodeToStrings(pubkey)
                    coin.address = address
                    coin.pubkey = pubkeyStr
                    uiThread {
                        dismiss()
                        updateCoin(coin, true, true)
                    }
                } else {
                    uiThread {
                        dismiss()
                        ToastUtils.show(
                            this@AddCoinActivity,
                            getString(R.string.my_wallet_detail_wrong_password)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val MSG_SEARCH = 1
    private var mDelaySearchHandler: DelaySearchHandler? = null

    inner class DelaySearchHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.e("addCoin","handleMessage")
            page = 1
            walletViewModel.searchCoinList(
                page,
                Constants.PAGE_LIMIT.toInt(),
                keyWord,
                "",
                ""
            )
        }
    }

    private fun getStatus(sid: String): Int? {
        return mStatusMap[sid]
    }

    private fun getCoin(sid: String): Coin? {
        return mCoinsMap[sid]
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    override fun finish() {
        hideKeyboard(et_search)
        if (homeCoinFragment != null && homeCoinFragment!!.needUpdate) {
            EventBus.getDefault().post(AddCoinEvent())
        }
        super.finish()
    }
}