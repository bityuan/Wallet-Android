package com.fzm.walletmodule.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.bean.response.TransactionResponse
import com.fzm.wallet.sdk.db.entity.Address
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.databinding.FragmentTransactionBinding
import com.fzm.walletmodule.ui.activity.TransactionsActivity
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.NetWorkUtils
import com.fzm.walletmodule.utils.TimeUtils
import com.fzm.walletmodule.utils.isFastClick
import com.google.gson.Gson
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.find


class TransactionFragment : BaseFragment() {

    private var mIndex: Long = 0
    private var mType: Int = 0
    private lateinit var coin: Coin
    private var mList = ArrayList<Transactions>()
    private var mTokenFeeList = ArrayList<Transactions>()
    private lateinit var mCommonAdapter: CommonAdapter<Transactions>
    private var isCanLoadMore = false

    private lateinit var binding: FragmentTransactionBinding

    //默认不隐藏
    private var dState: Boolean = false


    companion object {
        private val TYPE = "type"
        private val COIN = "coin"

        fun newInstance(type: Int, coin: Coin): TransactionFragment {
            val bundle = Bundle()
            bundle.putInt(TYPE, type)
            bundle.putSerializable(COIN, coin)
            val transactionFragment = TransactionFragment()
            transactionFragment.arguments = bundle
            return transactionFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mType = arguments?.getInt(TYPE) as Int
        coin = arguments?.getSerializable(COIN) as Coin
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //会调用2次是因为viewpager默认预加载2页
        initObserver()
        initData()
        initRefresh()
    }


    override fun initData() {
        super.initData()
        binding.rvList.layoutManager = LinearLayoutManager(activity)
        mCommonAdapter =
            object : CommonAdapter<Transactions>(activity, R.layout.listitem_coin_details, mList) {
                override fun convert(holder: ViewHolder, transaction: Transactions, position: Int) {

                    holder.setVisible(R.id.tv_time, transaction.blocktime != 0L)
                    //时间
                    holder.setText(R.id.tv_time, TimeUtils.getTime(transaction.blocktime * 1000L))
                    //金额
                    val inOut =
                        if (transaction.type == Transactions.TYPE_SEND) Transactions.OUT_STR else Transactions.IN_STR
                    holder.setText(R.id.tv_money, inOut + transaction.value + " " + coin.uiName)
                    //地址
                    val otherAddress =
                        if (transaction.type == Transactions.TYPE_SEND) transaction.to else transaction.from
                    holder.setText(R.id.tv_address, otherAddress)

                    //状态
                    val pedding = Color.parseColor("#7190FF")
                    val success = Color.parseColor("#37AEC4")
                    val fail = Color.parseColor("#EC5151")

                    when (transaction.status) {
                        -1 -> handleStatus(holder, getString(R.string.home_transaction_fails), fail)
                        0 -> handleStatus(holder, getString(R.string.home_confirming), pedding)
                        1 -> handleStatus(
                            holder, getString(R.string.home_transaction_success), success
                        )
                    }
                }

                private fun handleStatus(holder: ViewHolder, status: String, color: Int) {
                    holder.setTextColor(R.id.tv_status, color)
                    holder.setText(R.id.tv_status, status)

                }

            }
        binding.rvList.adapter = mCommonAdapter

        binding.rvList.setOnItemClickListener { holder, position ->
            if (isFastClick()) {
                return@setOnItemClickListener
            }
            handleOnItemClick(position)
        }
    }

    private fun handleOnItemClick(position: Int) {
        val transactions = mList[position]
        val height = transactions.height
        for (t in mTokenFeeList) {
            if (t.height == height && "token fee" == t.note) {
                transactions.fee = t.value.toString()
                break
            }
        }
        ARouter.getInstance().build(RouterPath.WALLET_TRANSACTION_DETAILS)
            .withSerializable(RouterPath.PARAM_COIN, coin)
            .withSerializable(RouterPath.PARAM_TRANSACTIONS, transactions).navigation()
    }

    override fun initRefresh() {
        super.initRefresh()
        binding.swlLayout.setOnRefreshListener {
            doRefresh()
        }
        doRefresh()
        //binding.swlLayout.autoRefresh()

        binding.rvList.setOnLoadMoreListener {
            getDatas(mIndex)
        }
    }

    private fun doRefresh() {
        getDatas(0)
        (activity as TransactionsActivity).doRefreshBalance()
    }


    private fun getDatas(index: Long) {
        if (coin.name == "USDT") {
            dState = MMkvUtil.decodeBoolean(Constants.TRAN_STATE_KEY)
        }
        val tokensymbol = GoWallet.getTokensymbol(coin)
        var datas: String?
        lifecycleScope.launch(Dispatchers.IO) {
            if (index == 0L && !NetWorkUtils.isConnected(context)) {
                datas = MMkvUtil.decodeString(getKey(tokensymbol))
            } else {
                val coinToken = coin.newChain
                datas = GoWallet.getTranList(
                    coin.address,
                    coinToken.cointype,
                    coinToken.tokenSymbol,
                    mType.toLong(),
                    index,
                    Constants.PAGE_LIMIT
                )

                logDebug("net tran = $datas")
            }

            //cache one page
            if (index == 0L && datas != null) {
                MMkvUtil.encode(getKey(tokensymbol), datas)
            }

            try {
                val response = Gson().fromJson(datas, TransactionResponse::class.java)
                if (response.result.isNullOrEmpty()) {
                    binding.swlLayout.onRefreshComplete()
                    binding.rvList.onLoadMoreComplete()
                } else {
                    val list = response.result as List<Transactions>
                    mIndex = index + Constants.PAGE_LIMIT
                    isCanLoadMore = list.size < Constants.PAGE_LIMIT
                    withContext(Dispatchers.Main) {
                        if (index == 0L) {
                            mList.clear()
                            binding.swlLayout.onRefreshComplete()
                        }

                        addList(list)
                        binding.rvList.setHasLoadMore(!isCanLoadMore)
                        binding.rvList.onLoadMoreComplete()
                        mCommonAdapter.notifyDataSetChanged()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("error", e.toString())
            }


        }

    }

    private fun getKey(coinName: String?): String {
        return coin.chain + coin.address + coinName + mType
    }


    private fun addList(list: List<Transactions>) {
        if (GoWallet.isPara(coin)) {
            for (transactions in list) {
                if (transactions.type == "send" && transactions.note == "token fee" && transactions.status == 1) {//发送的手续费记录
                    mTokenFeeList.add(transactions)
                    continue
                }
                if (dState && transactions.value?.toDouble()!! < 1 && coin.name == "USDT") {
                    continue
                }
                mList.add(transactions)
            }
        } else {
            for (transactions in list) {
                if (dState && transactions.value?.toDouble()!! < 1 && coin.name == "USDT") {
                    continue
                }
                mList.add(transactions)
            }

        }
    }


    private fun getWalletNames(address: String): String {
        var names = ""
        val coinList = LitePal.where("address = ?", address).find<Coin>(true)
        for (coin in coinList) {
            val walletName = coin.getpWallet().name
            names += "$walletName,"
        }
        return names
    }

    private fun getContacts(address: String): String {
        var names = ""
        val addrList = LitePal.where("address = ?", address).find<Address>(true)
        for (coin in addrList) {
            val nickName = coin.contacts?.nickName
            names += "$nickName,"
        }
        return names
    }


    fun doAsset() {
        doRefresh()
    }


}
