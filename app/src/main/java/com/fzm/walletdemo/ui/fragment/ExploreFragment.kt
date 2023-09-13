package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundResource
import org.koin.android.ext.android.inject

class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreNewBinding
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
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


}