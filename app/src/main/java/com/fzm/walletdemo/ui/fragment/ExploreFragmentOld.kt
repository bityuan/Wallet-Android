package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentExploreBinding
import com.fzm.walletdemo.ui.adapter.ExploreAdapter
import com.fzm.walletdemo.ui.adapter.ExploreDiffCallBack
import com.fzm.walletmodule.vm.WalletViewModel
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

class ExploreFragmentOld : Fragment() {
    private lateinit var binding: FragmentExploreBinding
    private var oldList: MutableList<ExploreBean.AppsBean>? = null
    private var newList: MutableList<ExploreBean.AppsBean>? = null
    private lateinit var adapter: ExploreAdapter
    private val walletViewModel: WalletViewModel by inject(walletQualifier)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            binding.llSearch.setOnClickListener {
                ARouter.getInstance().build(RouterPath.APP_SEARCH_DAPP).navigation()
            }
            val netIndex = MMkvUtil.decodeInt(GoWallet.CHAIN_NET)
            binding.incExTitle.tvChainNet.text = GoWallet.getChainNet(netIndex)
            binding.incExTitle.llChooseNet.setOnClickListener {
                val menu =
                    PopMenu.show(listOf(GoWallet.NET_BTY, GoWallet.NET_ETH, GoWallet.NET_BNB))
                menu.onMenuItemClickListener = OnMenuItemClickListener { dialog, text, index ->
                    MMkvUtil.encode(GoWallet.CHAIN_NET, index)
                    binding.incExTitle.tvChainNet.text = GoWallet.getChainNet(index)
                    false
                }
                menu.onIconChangeCallBack = object : OnIconChangeCallBack<PopMenu>() {
                    override fun getIcon(dialog: PopMenu?, index: Int, menuText: String?): Int {
                        return when (index) {
                            0 -> R.mipmap.my_wallet_bty
                            1 -> R.mipmap.my_wallet_eth
                            2 -> R.mipmap.my_wallet_bnb
                            else -> R.mipmap.my_wallet_eth
                        }
                    }
                }
            }



            oldList = mutableListOf()
            val gridLayoutManager = GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val item = newList!![position]
                    //style为1，则占用4列
                    return if (item.style == 1) {
                        4
                    } else {
                        1
                    }
                }

            }
            binding.rvExplore.layoutManager = gridLayoutManager
            adapter = ExploreAdapter(it)
            binding.rvExplore.adapter = adapter
        }
        binding.swipeExplore.setOnRefreshListener {
            getExploreAll()
        }
        getExploreAll()
    }


    private fun getExploreAll() {
        lifecycleScope.launch {
            val list = walletViewModel.getExploreList()
            withContext(Dispatchers.Main) {
                binding.swipeExplore.isRefreshing = false
                newList = null
                newList = mutableListOf()
                for (l in list) {
                    //在数据上做文章，添加第一条为title
                    //title的style都设置为1
                    val titleBean = ExploreBean.AppsBean().apply {
                        id = -1
                        name = l.name
                        style = 1
                    }
                    newList?.add(titleBean)

                    for (app in l.apps) {
                        app.style = l.style
                        newList?.add(app)
                    }


                }
                val diffCallBack = ExploreDiffCallBack(oldList!!, newList!!)
                val diffResult = DiffUtil.calculateDiff(diffCallBack)
                diffResult.dispatchUpdatesTo(adapter)
                adapter.setData(newList!!)
                oldList = newList
            }
        }

        adapter.setOnItemClickListener {
            try {
                oldList?.let { list ->
                    val app = list[it]
                    val appId = "${app.id}"
                    if (MMkvUtil.decodeBoolean(appId)) {
                        gotoDapp(app)
                    } else {
                        context?.let { it1 ->
                            MaterialDialog.Builder(it1)
                                .negativeText(getString(R.string.cancel))
                                .positiveText(getString(R.string.ok))
                                .title(getString(R.string.explore_title))
                                .content(getString(R.string.explore_disclaimer)).checkBoxPrompt(
                                    getString(R.string.no_dotip), false
                                ) { buttonView, isChecked ->
                                    MMkvUtil.encode(appId, isChecked)
                                }.onNegative { dialog, which ->
                                }.onPositive { dialog, which ->
                                    gotoDapp(app)
                                }.build().show()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun gotoDapp(app: ExploreBean.AppsBean) {
        app.let { appBean ->
            if (appBean.type == 1) {
                val count = LitePal.count<PWallet>()
                if (count == 0) {
                    toast(getString(R.string.create_wallet_pre))
                    return@let
                }
            }
            val id = MyWallet.getId()
            val wallet = LitePal.find<PWallet>(id)
            if (wallet?.type == PWallet.TYPE_ADDR_KEY) {
                toast(getString(R.string.str_addr_no))
                return@let
            }

            ARouter.getInstance().build(RouterPath.APP_DAPP).withString("name", appBean.name)
                .withString("url", appBean.app_url).navigation()
        }
    }

}