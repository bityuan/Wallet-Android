package com.fzm.walletdemo.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cn.finalteam.loadingviewfinal.RecyclerViewFinal
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.Node
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentExploreNewBinding
import com.fzm.walletdemo.databinding.ViewExploreBinding
import com.fzm.walletmodule.vm.WalletViewModel
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.PopMenu
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundResource
import org.koin.android.ext.android.inject

class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreNewBinding
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private val nodeList = mutableListOf<Node>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreNewBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeExplore.setOnRefreshListener {
            getExploreAll()
        }
        getExploreAll()
        binding.llSearch.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_SEARCH_DAPP).navigation()
        }
        val netIndex = MMkvUtil.decodeInt(GoWallet.CHAIN_NET)
        binding.incExTitle.tvChainNet.text = GoWallet.getChainNet(netIndex)
        binding.incExTitle.llChooseNet.setOnClickListener {
            val menu = PopMenu.show(listOf(GoWallet.NET_BTY, GoWallet.NET_ETH, GoWallet.NET_BNB))
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
    }


    private fun getExploreAll() {
        lifecycleScope.launch {
            val list = walletViewModel.getExploreList()
            withContext(Dispatchers.Main) {
                binding.swipeExplore.isRefreshing = false
                binding.llExplore.removeAllViews()

                for (ex in list) {
                    val exploreBinding = ViewExploreBinding.inflate(layoutInflater)
                    exploreBinding.tvTitle.text = ex.name
                    val bg = when (ex.id) {
                        1 -> R.mipmap.bg_explore_eth
                        2 -> R.mipmap.bg_explore_bty
                        3 -> R.mipmap.bg_explore_ycc
                        else -> R.mipmap.bg_explore_eth
                    }
                    exploreBinding.ivBg.backgroundResource = bg
                    exploreBinding.ivBg.setOnClickListener {
                        ARouter.getInstance().build(RouterPath.APP_EXPLORES)
                            .withInt(RouterPath.PARAM_APPS_ID, ex.id).navigation()

                    }
                    binding.llExplore.addView(exploreBinding.root)
                }


            }
        }

    }


    // chooseWeb3Chain

    private fun initNodeDatas() {
        nodeList.clear()
        val btyNode = Node()
        btyNode.name = GoWallet.NET_BTY
        btyNode.rpcUrl = GoWallet.WEB3_BTY
        btyNode.chainId = GoWallet.CHAIN_ID_BTY_L
        btyNode.symbol = "BTY"
        btyNode.browser = IPConfig.BROWSER_BTY

        val ethNode = Node()
        ethNode.name = GoWallet.NET_ETH
        ethNode.rpcUrl = GoWallet.WEB3_ETH
        ethNode.chainId = GoWallet.CHAIN_ID_ETH_L
        ethNode.symbol = "ETH"
        ethNode.browser = IPConfig.BROWSER_ETH

        val bnbNode = Node()
        bnbNode.name = GoWallet.NET_BNB
        bnbNode.rpcUrl = GoWallet.WEB3_BNB
        bnbNode.chainId = GoWallet.CHAIN_ID_BNB_L
        bnbNode.symbol = "BNB"
        bnbNode.browser = IPConfig.BROWSER_BNB
        nodeList.add(btyNode)
        nodeList.add(ethNode)
        nodeList.add(bnbNode)
    }

    private fun chooseWeb3Chain(data: List<Node>) {
        val builder = AlertDialog.Builder(activity)
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_web3_chain, null)
        builder.setView(view)
        val alertDialog = builder.create()
        val window = alertDialog.window
        window!!.setBackgroundDrawableResource(R.color.transparent)
        alertDialog.show()
        window.decorView.setPadding(0, 0, 0, 0)
        window.setGravity(Gravity.BOTTOM)
        val lp = window.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = lp
        val tvClose = view.findViewById<TextView>(R.id.tv_close)
        val rvList: RecyclerViewFinal = view.findViewById(R.id.rv_list)
        tvClose.setOnClickListener { alertDialog.dismiss() }
        rvList.layoutManager = LinearLayoutManager(activity)
        rvList.adapter =
            object : CommonAdapter<Node>(activity, R.layout.item_web3_chain, data) {
                override fun convert(holder: ViewHolder, node: Node, position: Int) {
                    holder.setText(R.id.tv_chain, node.name)

                }
            }
        rvList.setOnItemClickListener { holder, position ->

        }
    }


}