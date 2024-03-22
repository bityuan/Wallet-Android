package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityCreateMnemTipBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import org.jetbrains.anko.toast

@Route(path = RouterPath.WALLET_CREATE_MNEM_TIP)
class CreateMnemTipActivity : BaseActivity() {

    @JvmField
    @Autowired(name = RouterPath.PARAM_WALLET)
    var mWallet: PWallet? = null

    private val binding by lazy { ActivityCreateMnemTipBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = getString(R.string.my_create_wallet)
        initView()
        initListener()
    }

    override fun initView() {
        super.initView()
    }

    override fun initListener() {
        super.initListener()
        binding.llTip1.setOnClickListener {
            binding.llTip1.setBackgroundResource(R.drawable.shape_fee_selected)
            binding.rbTip1.isChecked = true
        }
        binding.llTip2.setOnClickListener {
            binding.llTip2.setBackgroundResource(R.drawable.shape_fee_selected)
            binding.rbTip2.isChecked = true
        }
        binding.llTip3.setOnClickListener {
            binding.llTip3.setBackgroundResource(R.drawable.shape_fee_selected)
            binding.rbTip3.isChecked = true
        }

        binding.btnNext.setOnClickListener {
            if (binding.rbTip1.isChecked &&
                binding.rbTip2.isChecked &&
                binding.rbTip3.isChecked
            ) {
                mWallet?.let { w ->
                    ARouter.getInstance().build(RouterPath.WALLET_CREATE_MNEM)
                        .withSerializable(RouterPath.PARAM_WALLET, w).navigation()
                }

            } else {
                toast(getString(R.string.p_check_str))
            }
        }

    }


}