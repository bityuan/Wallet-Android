package com.fzm.walletmodule.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.tokenSymbol
import com.fzm.wallet.sdk.utils.totalAsset
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.WalletAdapter
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.databinding.FragmentWalletBinding
import com.fzm.walletmodule.event.*
import com.fzm.walletmodule.ui.activity.*
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.*
import com.fzm.walletmodule.vm.ParamViewModel
import com.fzm.walletmodule.vm.WalletViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
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
    private var newPosition = false

    private val paramViewModel by activityViewModels<ParamViewModel>()
    private lateinit var binding:FragmentWalletBinding

    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            // FIXME: 最好使用lifecycle2.4.0提供的repeatOnLifecycle方法
            job = lifecycleScope.launch {
                BWallet.get().getCoinBalance(0, Constants.DELAYED_TIME, false)
                    .collect {
                        //  Log.e("wallet","getCoinBalance")
                        mCoinList.clear()
                        mCoinList.addAll(it)
                        //mCoinList.sort()
                        mWalletAdapter?.notifyDataSetChanged()
                        val moneys = DecimalUtils.subWithNum(it.sumOf { c -> c.totalAsset }, 2)
                        money?.text = moneys
                        paramViewModel.walletMoney.value = moneys
                    }
            }
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            job?.cancel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        initView()
        configWallets()
        initData()
        initListener()
        viewLifecycleOwner.lifecycle.addObserver(observer)
    }

    override fun configWallets() {
        super.configWallets()
        binding.rlTop.visibility = View.GONE
        //initHeaderView()
    }

    override fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        mWalletAdapter =
            WalletAdapter(requireActivity(), R.layout.view_item_coin_info, mCoinList, this)
        mWalletAdapter?.setOnItemClickListener(object : WalletAdapter.ItemClickListener {
            override fun OnItemClick(view: View?, position: Int) {
                if (isFastClick()) {
                    return
                }
                val coinPosition = if (newPosition) position - 1 else position //减去header

                if (ListUtils.isEmpty(mCoinList) || coinPosition < 0 || coinPosition >= mCoinList.size) {
                    return
                }
                val coin: Coin = mCoinList[coinPosition]
                val intent = Intent(activity, TransactionsActivity::class.java)
                val coinToken = GoWallet.newCoinType(coin.chain, coin.tokenSymbol)
                coin.chain = coinToken.cointype
                intent.putExtra(Coin::class.java.simpleName, coin)
                startActivity(intent)
            }

            override fun OnLongItemClick(view: View?, position: Int) {}
        })
        binding.recyclerView.adapter = mWalletAdapter
    }

    private fun initHeaderView() {
        newPosition = true
        val mHeaderView =
            LayoutInflater.from(activity).inflate(R.layout.view_header_wallet, null, false)
        more = mHeaderView.findViewById<ImageView>(R.id.more)
        name = mHeaderView.findViewById<TextView>(R.id.name)
        money = mHeaderView.findViewById<TextView>(R.id.money)
        addCoin = mHeaderView.findViewById<ImageView>(R.id.addCoin)
        binding.recyclerView.addHeaderView(mHeaderView)
    }

    override fun initData() {
        lifecycleScope.launchWhenResumed {
            BWallet.get().current.collect {
                name?.text = it.walletInfo.name
                paramViewModel.walletName.value = it.walletInfo.name
            }
        }
    }

    override fun initListener() {
        binding.swlLayout.setOnRefreshListener {
            binding.swlLayout.onRefreshComplete()
        }
        more?.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_WALLET_DETAILS)
                .withLong(PWallet.PWALLET_ID, BWallet.get().getCurrentWallet()?.id ?: 0L).navigation()

        }
        binding.ivBack.setOnClickListener {
            requireActivity().finish()
        }
        addCoin?.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            startActivity<AddCoinActivity>()
        }
        binding.topLeft.setOnClickListener {
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
        binding.topRight.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            val intent = Intent()
            intent.setClass(requireActivity(), MyWalletsActivity::class.java)
            startActivity(intent)
        }
        binding.rlTop.setOnClickListener {
            if (ClickUtils.isFastClick(1000)) {
                binding.recyclerView.smoothScrollToPosition(0)
            }
        }
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