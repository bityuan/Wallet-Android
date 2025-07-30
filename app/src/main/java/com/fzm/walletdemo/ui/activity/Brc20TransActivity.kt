package com.fzm.walletdemo.ui.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.bean.Brc20Tran
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityBrc20TransBinding
import com.fzm.walletdemo.databinding.ActivityExploresBinding
import com.fzm.walletdemo.ui.adapter.Brc20TransAdapter
import com.fzm.walletdemo.ui.adapter.ExploresAdapter
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.HtmlUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.google.android.material.button.MaterialButton
import com.king.zxing.util.CodeUtils
import com.kongzue.dialogx.dialogs.PopMenu
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find


@Route(path = RouterPath.APP_BRC20TRANS)
class Brc20TransActivity : BaseActivity() {

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var address: String? = null

    @JvmField
    @Autowired
    var availableBalance: String? = null

    @JvmField
    @Autowired
    var transferableBalance: String? = null

    private val tranList = mutableListOf<Brc20Tran>()

    private val binding by lazy { ActivityBrc20TransBinding.inflate(layoutInflater) }
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private var adapter: Brc20TransAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
        initView()
    }

    override fun initObserver() {
        super.initObserver()
        walletViewModel.getBrc20Balance.observe(this, Observer {
            if (it.isSucceed()) {
                val details = it.data()?.detail
                val deta = details?.find { it.ticker == name }
                binding.tvTransferable.text = "${deta?.transferableBalance}"
                binding.tvAvailable.text = "${deta?.availableBalance}"
            }
        })
        walletViewModel.getBrc20Tran.observe(this, Observer {
            binding.swipeList.isRefreshing = false
            val list = it
            tranList.clear()
            if (list != null) {
                tranList.addAll(list)
            }
            adapter?.notifyDataSetChanged()

        })
    }

    override fun initView() {
        super.initView()
        binding.tvAddress.setOnClickListener { _ ->
            ClipboardUtils.clip(this, address)
        }

        title = "$name"
        binding.btnMinke.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_BRC20_OUT_MINKE)
                .withString("name", name)
                .withString("address", address)
                .withString("availableBalance", availableBalance)
                .withString("transferableBalance", transferableBalance)
                .navigation()

        }
        binding.btnTo.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_BRC20_OUT_PRE)
                .withString("address", address)
                .withString("name", name)
                .navigation()

        }
        binding.btnReceive.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_BRC20_IN)
                .withString("name", name)
                .withString("address", address)
                .navigation()

        }
        val bitmap: Bitmap = CodeUtils.createQRCode(address, 190)
        binding.ivAddress.setImageBitmap(bitmap)
        binding.tvAddress.text = HtmlUtils.change4(address)
        binding.tvTransferable.text = "${transferableBalance}"
        binding.tvAvailable.text = "${availableBalance}"

        val coin = Coin()
        coin.name = name
        coin.address = address
        adapter = Brc20TransAdapter(tranList, coin)
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = adapter


        binding.swipeList.setOnRefreshListener {
            walletViewModel.getBrc20Balance(address!!)
            walletViewModel.getBrc20Tran(address!!, name!!)
        }
        walletViewModel.getBrc20Tran(address!!, name!!)

    }


}