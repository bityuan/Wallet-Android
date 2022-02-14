package com.fzm.walletmodule.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.bean.response.TransactionResponse
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.ui.activity.TransactionDetailsActivity
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.*
import com.google.gson.Gson
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.android.synthetic.main.fragment_transaction.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.uiThread


class TransactionFragment : BaseFragment() {

    private var mIndex: Long = 0
    private var mType: Int = 0
    private lateinit var coin: Coin
    private var mList = ArrayList<Transactions>()
    private var mTokenFeeList = ArrayList<Transactions>()
    private lateinit var mCommonAdapter: CommonAdapter<Transactions>
    private var isCanLoadMore = false


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

    override fun getLayout(): Int {
        return R.layout.fragment_transaction
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
        rv_list.layoutManager = LinearLayoutManager(activity)
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
                            holder,
                            getString(R.string.home_transaction_success),
                            success
                        )
                    }
                }

                private fun handleStatus(holder: ViewHolder, status: String, color: Int) {
                    holder.setTextColor(R.id.tv_status, color)
                    holder.setText(R.id.tv_status, status)

                }

            }
        rv_list.adapter = mCommonAdapter

        rv_list.setOnItemClickListener { holder, position ->
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
                transactions.fee = t.value
                break
            }
        }
        startActivity<TransactionDetailsActivity>(
            Transactions::class.java.simpleName to transactions,
            Coin::class.java.simpleName to coin, Constants.FROM to "list"
        )
    }

    override fun initRefresh() {
        super.initRefresh()
        swl_layout.setOnRefreshListener {
            getDatas(0)
        }
        swl_layout.autoRefresh()

        rv_list.setOnLoadMoreListener {
            getDatas(mIndex)
        }
    }


    fun getDatas(index: Long) {
        var coinName = coin.name
        if (GoWallet.isBTYChild(coin)) {
            if (coin.treaty == "1") {
                coinName = coin.platform + "." + coin.name
            } else {
                coinName = coin.platform + ".coins"
            }
        }

        doAsync {
            var datas: String?
            if (index == 0L) {
                if (!NetWorkUtils.isConnected(context)) {
                    datas = MMkvUtil.decodeString(getKey(coinName))
                } else {
                    datas = GoWallet.getTranList(
                        coin.address, coin.chain, coinName, mType.toLong(), index,
                        Constants.PAGE_LIMIT
                    )
                }
            } else {
                datas = GoWallet.getTranList(
                    coin.address, coin.chain, coinName, mType.toLong(), index,
                    Constants.PAGE_LIMIT
                )
            }
            val query = query(datas)
            uiThread {
                try {
                    if (index == 0L) {
                        if (datas != null) {
                            MMkvUtil.encode(getKey(coinName), datas)
                        }
                    }
                    updateList(query!!, index)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun getKey(coinName: String?): String {
        return coin.chain + coin.address + coinName + mType
    }

    private fun query(datas: String?): List<Transactions>? {
        val gson = Gson()
        val response = gson.fromJson(datas, TransactionResponse::class.java)
        return response.result
    }

    @Synchronized
    private fun updateList(list: List<Transactions>, index: Long) {
        if (activity == null) {
            return
        }
        mIndex = index + Constants.PAGE_LIMIT
        isCanLoadMore = list.size < Constants.PAGE_LIMIT
        if (index == 0L) {
            mList.clear()
            swl_layout?.onRefreshComplete()
        }

        addList(list)
        rv_list?.setHasLoadMore(!isCanLoadMore)
        rv_list?.onLoadMoreComplete()
        mCommonAdapter.notifyDataSetChanged()
    }


    private fun addList(list: List<Transactions>) {
        if (GoWallet.isBTYChild(coin)) {
            for (transactions in list) {
                if (transactions.type == "send"
                    && transactions.note == "token fee"
                    && transactions.status == 1
                ) {//发送的手续费记录
                    mTokenFeeList.add(transactions)
                    continue
                }
                mList.add(transactions)
            }
        } else {
            mList.addAll(list)
        }
    }

    fun refresh() {
        if (swl_layout != null) {
            getDatas(0)
        }
    }


}
