package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityHistoryBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.TimeUtils
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

@Route(path = RouterPath.WALLET_HISTORY)
class HistoryActivity : BaseActivity() {
    private lateinit var mCommonAdapter: CommonAdapter<*>
    private val mList = ArrayList<Transactions>()
    private val binding by lazy { ActivityHistoryBinding.inflate(layoutInflater) }

    private val walletRepository: WalletRepository by inject(walletQualifier)

    @JvmField
    @Autowired(name = RouterPath.PARAM_COIN)
    var coin: Coin? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_ADDRESS)
    var to: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = getString(R.string.home_transaction_record)
        initView()
    }

    override fun initView() {
        super.initView()
        binding.swlLayout.setOnRefreshListener {
            getData()
        }
        binding.rvList.layoutManager = LinearLayoutManager(this)
        mCommonAdapter = object : CommonAdapter<Transactions>(this, R.layout.item_history, mList) {
            override fun convert(holder: ViewHolder, transaction: Transactions, position: Int) {
                holder.setVisible(R.id.tv_time, transaction.blocktime != 0L)
                holder.setText(R.id.tv_time, TimeUtils.getTime(transaction.blocktime * 1000L))
                holder.setText(
                    R.id.tv_value,
                    Transactions.OUT_STR + transaction.value + " " + coin?.name
                )
                holder.setText(R.id.tv_address, transaction.to)
            }
        }
        binding.rvList.adapter = mCommonAdapter
        binding.rvList.setOnItemClickListener { viewHolder, i ->
            ARouter.getInstance().build(RouterPath.WALLET_TRANSACTION_DETAILS)
                .withSerializable(RouterPath.PARAM_COIN, coin)
                .withSerializable(RouterPath.PARAM_TRANSACTIONS, mList[i]).navigation()
        }
        getData()
    }

    private fun getData(){
        coin?.let {
            binding.tvOutName.text = "${getString(R.string.transferred_str)}${it.oldName}"
            lifecycleScope.launch(Dispatchers.IO) {
                val result = walletRepository.queryTxHistoryDetail(
                    it.chain,
                    it.name,
                    it.address,
                    to,
                    0,
                    200,
                    0
                )
                withContext(Dispatchers.Main) {
                    binding.swlLayout.onRefreshComplete()
                    if (result.isSucceed()) {
                        result.data()?.let { his ->
                            binding.tvOutSum.text = his.totalamount
                            mList.clear()
                            mList.addAll(his.txs)
                            mCommonAdapter.notifyDataSetChanged()
                        }

                    }
                }
            }
        }
    }
}