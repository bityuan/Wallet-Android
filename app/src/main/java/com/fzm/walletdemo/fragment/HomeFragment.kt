package com.fzm.walletdemo.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.fragment.NFTFragment
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentHomeBinding
import com.fzm.walletmodule.ui.activity.AddCoinActivity
import com.fzm.walletmodule.ui.activity.MyWalletsActivity
import com.fzm.walletmodule.ui.activity.WalletDetailsActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.utils.ClickUtils
import com.fzm.walletmodule.vm.ParamViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.layout_header.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.support.v4.startActivity
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.Walletapi

class HomeFragment : Fragment() {

    private val paramViewModel by activityViewModels<ParamViewModel>()
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = Adapter(childFragmentManager)
        adapter.addFragment(WalletFragment(), "资产")
        adapter.addFragment(NFTFragment(), "NFT")
        binding.vpHome.adapter = adapter
        binding.tabHome.setupWithViewPager(binding.vpHome)

        paramViewModel.walletName.observe(viewLifecycleOwner, Observer {
            binding.header.tvName.text = it
        })
        paramViewModel.walletMoney.observe(viewLifecycleOwner, Observer {
            binding.header.tvMoney.text = it


        })

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val id = BWallet.get().getCurrentWallet()?.id ?: 0
                val pWallet = LitePal.find(PWallet::class.java, id, true)
                refreshWallet(pWallet)
            }
        }

        binding.header.ivChangeWallet.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            val intent = Intent()
            intent.setClass(requireActivity(), MyWalletsActivity::class.java)
            startActivity(intent)
        }
        binding.header.ivMore.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_WALLET_DETAILS)
                .withLong(PWallet.PWALLET_ID, BWallet.get().getCurrentWallet()?.id ?: 0L)
                .navigation()
        }
        binding.header.ivAddCoin.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            startActivity<AddCoinActivity>()
        }


    }


    private fun refreshWallet(pWallet: PWallet) {
        if (pWallet.type == PWallet.TYPE_PRI_KEY) {
            val chain = pWallet.coinList[0].chain
            binding.header.rlWalletBg.backgroundResource = getWalletBg(chain)
            binding.header.tvWalletName.text = "私钥账户"
        } else {
            binding.header.tvWalletName.text = "助记词账户"
            binding.header.rlWalletBg.backgroundResource = R.mipmap.header_wallet_hd_wallet
        }
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitles[position]
        }
    }


    private fun getWalletBg(chain: String): Int {
        return when (chain) {
            Walletapi.TypeBitcoinString -> R.mipmap.header_wallet_btc_wallet
            Walletapi.TypeBtyString -> R.mipmap.header_wallet_bty_wallet
            Walletapi.TypeDcrString -> R.mipmap.header_wallet_dcr_wallet
            Walletapi.TypeETHString -> R.mipmap.header_wallet_eth_wallet
            Walletapi.TypeLitecoinString -> R.mipmap.header_wallet_ltc_wallet
            Walletapi.TypeEtherClassicString -> R.mipmap.header_wallet_etc_wallet
            Walletapi.TypeZcashString -> R.mipmap.header_wallet_zec_wallet
            Walletapi.TypeNeoString -> R.mipmap.header_wallet_neo_wallet
            Walletapi.TypeBchString -> R.mipmap.header_wallet_bch_wallet
            Walletapi.TypeTrxString, Walletapi.TypeBnbString -> R.mipmap.header_wallet_trx_wallet
            Walletapi.TypeAtomString -> R.mipmap.header_wallet_atom_wallet
            Walletapi.TypeHtString -> R.mipmap.header_wallet_ht_wallet
            else -> R.mipmap.header_wallet_bty_wallet
        }
    }

}