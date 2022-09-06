package com.fzm.walletdemo.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.db.entity.PWallet.*
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentHomeBinding
import com.fzm.walletdemo.ui.activity.MainActivity
import com.fzm.walletmodule.ui.activity.AddCoinActivity
import com.fzm.walletmodule.ui.activity.MyWalletsActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.utils.ClickUtils
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.support.v4.startActivity
import org.litepal.LitePal
import walletapi.Walletapi

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var cWalletId: Long = -1

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
        //adapter.addFragment(NFTFragment(), "NFT")
        binding.vpHome.adapter = adapter
        binding.tabHome.setupWithViewPager(binding.vpHome)

        /*if (BWallet.get().getCurrentWallet()?.type != 2) {
            if (binding.tabHome.tabCount > 1) {
                binding.tabHome.getTabAt(1)?.view?.visibility = View.GONE
            }
        } else {
            if (binding.tabHome.tabCount > 1) {
                binding.tabHome.getTabAt(1)?.view?.visibility = View.VISIBLE
            }
        }*/



        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    cWalletId = MyWallet.getId()
                    val pWallet = LitePal.find(PWallet::class.java, cWalletId, true)
                    if (pWallet != null) {
                        refreshWallet(pWallet)
                    } else {
                        activity?.let {
                            (it as MainActivity).setTabSelection(0)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
                .withLong(PWallet.PWALLET_ID, cWalletId)
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
        binding.header.tvName.text = pWallet.name
        when (pWallet.type) {
            TYPE_PRI_KEY -> {
                val chain = pWallet.coinList[0].chain
                binding.header.rlWalletBg.backgroundResource = getWalletBg(chain)
                binding.header.tvWalletName.text = "私钥账户"
            }
            TYPE_RECOVER -> {
                val chain = pWallet.coinList[0].chain
                binding.header.rlWalletBg.backgroundResource = getWalletBg(chain)
                binding.header.tvWalletName.text = "找回账户"
            }
            TYPE_NOMAL -> {
                binding.header.tvWalletName.text = "助记词账户"
                binding.header.rlWalletBg.backgroundResource = R.mipmap.header_wallet_hd_wallet
            }
        }
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }

        fun removeFragment(fragment: Fragment, title: String) {
            mFragments.remove(fragment)
            mFragmentTitles.remove(title)
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