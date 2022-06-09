package com.fzm.walletdemo.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.fzm.nft.fragment.NFTFragment
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.databinding.FragmentHomeBinding
import com.fzm.walletmodule.ui.activity.AddCoinActivity
import com.fzm.walletmodule.ui.activity.MyWalletsActivity
import com.fzm.walletmodule.ui.activity.WalletDetailsActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.utils.ClickUtils
import com.fzm.walletmodule.vm.ParamViewModel
import org.jetbrains.anko.support.v4.startActivity

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
            val intent = Intent(requireActivity(), WalletDetailsActivity::class.java)
                .apply {
                    putExtra(PWallet.PWALLET_ID, BWallet.get().getCurrentWallet()?.id ?: 0L)
                }
            startActivityForResult(intent, WalletFragment.UPDATE_WALLET)
        }
        binding.header.ivAddCoin.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            startActivity<AddCoinActivity>()
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

}