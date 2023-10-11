package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletdemo.databinding.FragmentMyBinding
import com.fzm.walletdemo.ui.WalletHelper
import com.fzm.walletmodule.update.UpdateUtils
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.walletmodule.vm.WalletViewModel
import org.jetbrains.anko.support.v4.toast
import org.koin.android.ext.android.inject

class MyFragment : Fragment() {

    private lateinit var binding: FragmentMyBinding

    private val walletViewModel: WalletViewModel by inject(walletQualifier)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObserver()
        if(WalletHelper.isSQ()){
            binding.tvRecover.visibility = View.GONE
            binding.llContacts.visibility = View.GONE
        }
        binding.tvCheckUpdate.text = "v" + AppUtils.getAppVersion(context)
        binding.tvShare.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_DOWNLOAD).navigation()
        }
        binding.llCheckUpdate.setOnClickListener {
            if(!WalletHelper.isSQ()){
                walletViewModel.getUpdate()
            }
        }
        binding.tvAbout.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_ABOUT).navigation()
        }
        binding.tvRecover.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_RECOVER).navigation()
        }

        binding.tvLang.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_LANGUAGE).navigation()
        }
        binding.tvContacts.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CONTACTS).navigation()
        }
        binding.tvNotice.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_MESSAGES).navigation()
        }
    }

    private fun initObserver() {
        walletViewModel.getUpdate.observe(viewLifecycleOwner, Observer {
            if (it.isSucceed()) {
                it.data()?.let {
                    activity?.let { act ->
                        UpdateUtils(act).update(it, childFragmentManager, act, false)

                    }
                }
            } else {
                toast(it.error())
            }

        })
    }


}