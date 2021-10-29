package com.fzm.walletmodule.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.activity.CreateWalletActivity
import com.fzm.walletmodule.ui.activity.ImportWalletActivity
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.ToastUtils
import kotlinx.android.synthetic.main.fragment_wallet_index.*


/**
 * 账户入口fragment
 */
class WalletIndexFragment : BaseFragment() {

    override fun getLayout(): Int {
        return R.layout.fragment_wallet_index
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    override fun initListener() {
        walletCreate.setOnClickListener {

            startActivity(Intent(activity, CreateWalletActivity::class.java))
        }
        walletImport.setOnClickListener {

            val intent = Intent(activity, ImportWalletActivity::class.java)
            startActivity(intent)
        }

    }
}