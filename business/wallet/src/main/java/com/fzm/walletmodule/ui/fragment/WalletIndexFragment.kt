package com.fzm.walletmodule.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletmodule.databinding.FragmentWalletIndexBinding
import com.fzm.walletmodule.ui.activity.ImportWalletActivity
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.isFastClick


class WalletIndexFragment : BaseFragment() {

    private lateinit var binding: FragmentWalletIndexBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletIndexBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    override fun initListener() {
        binding.walletCreate.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_CREATE_WALLET).navigation()
        }
        binding.walletImport.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            val intent = Intent(activity, ImportWalletActivity::class.java)
            startActivity(intent)
        }

    }
}