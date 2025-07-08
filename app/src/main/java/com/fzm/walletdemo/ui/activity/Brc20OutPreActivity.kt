package com.fzm.walletdemo.ui.activity


import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.RouterPath.APP_BRC20_OUT
import com.fzm.wallet.sdk.bean.Brc20Tran
import com.fzm.wallet.sdk.bean.TransferAble
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.walletdemo.databinding.ActivityBrc20OutBinding
import com.fzm.walletdemo.databinding.ActivityBrc20OutPreBinding
import com.fzm.walletdemo.ui.adapter.Brc20OutPreAdapter
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.vm.WalletViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

@Route(path = RouterPath.APP_BRC20_OUT_PRE)
class Brc20OutPreActivity : BaseActivity() {

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var address: String? = null

    private var adapter: Brc20OutPreAdapter? = null
    private val ableList = mutableListOf<TransferAble>()

    private val walletViewModel by viewModel<WalletViewModel>(walletQualifier)
    private val binding by lazy { ActivityBrc20OutPreBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
        initView()
    }


    override fun initObserver() {
        super.initObserver()
        walletViewModel.transferAbles.observe(this, Observer {
            val list = it.data()?.detail
            ableList.clear()
            list?.let {
                ableList.addAll(it)
            }
            adapter?.notifyDataSetChanged()

        })
    }

    override fun initView() {
        super.initView()
        address?.let { walletViewModel.transferAble(it,name!!) }
        binding.rvList.layoutManager = GridLayoutManager(this, 3)
        adapter = Brc20OutPreAdapter(this, ableList)
        binding.rvList.adapter = adapter
        adapter?.setOnItemClickListener {
            val item = ableList[it]
            ARouter.getInstance().build(APP_BRC20_OUT)
                .withString("address",address)
                .withString("inscriptionId",item.inscriptionId)
                .withString("tick",item.data.tick)
                .withString("amt",item.data.amt)
                .navigation()
        }
    }


}