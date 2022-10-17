package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.databinding.FragmentMyBinding
import com.fzm.walletmodule.update.UpdateUtils
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.walletmodule.vm.WalletViewModel
import org.jetbrains.anko.support.v4.toast
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.compat.ScopeCompat.viewModel
import walletapi.WalletRecover

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
        binding.tvCheckUpdate.text = "v" + AppUtils.getAppVersion(context)
        binding.tvShare.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_DOWNLOAD).navigation()
        }
        binding.llCheckUpdate.setOnClickListener {
            walletViewModel.getUpdate()
        }
        binding.tvAbout.setOnClickListener {
            ARouter.getInstance().build(RouterPath.APP_ABOUT).navigation()
        }
        binding.tvRecover.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_RECOVER).navigation()
        }
        binding.tvCheckEmail.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CHECKEMAIL).navigation()
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