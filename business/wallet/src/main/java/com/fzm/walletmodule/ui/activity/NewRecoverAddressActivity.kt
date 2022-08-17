package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityNewRecoverAddressBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ListUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.WalletRecover
import walletapi.WalletRecoverParam
import walletapi.Walletapi

@Route(path = RouterPath.WALLET_NEW_RECOVER_ADDRESS)
class NewRecoverAddressActivity : BaseActivity() {

    @JvmField
    @Autowired(name = PWallet.PWALLET_ID)
    var walletid: Long = 0
    private var checkValue = 1
    private var recoverTime: Long = 0//单位为秒

    companion object {
        const val OFFICIAL_ADDRESS = "1NinUtSXP2wE6tJDMEpJwA8UBpyskSo8yd"
        const val OFFICIAL_PUB =
            "037f0cc5b5033e2a3a448c58987b987c801bb4632c1789184858e9e43ce8004fff"
    }

    private val binding by lazy { ActivityNewRecoverAddressBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = "生成找回地址"
        initView()
    }

    override fun initView() {
        super.initView()
        binding.tvBackAddressDefault.text = OFFICIAL_ADDRESS
        binding.rgDay.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_7day -> {
                    recoverTime = 7
                }
                R.id.rb_30day -> {
                    recoverTime = 30
                }
                R.id.rb_90day -> {
                    recoverTime = 90
                }

            }
        }
        binding.rgMenu.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_default -> {
                    checkValue = 1
                    binding.llDefault.visibility = View.VISIBLE
                    binding.llCustom.visibility = View.GONE
                }
                R.id.rb_custom -> {
                    checkValue = 2
                    binding.llCustom.visibility = View.VISIBLE
                    binding.llDefault.visibility = View.GONE
                }

            }
        }

        binding.btnOk.setOnClickListener {
            var myBackPub = ""
            if (checkValue == 1) {
                myBackPub = OFFICIAL_PUB

            } else if (checkValue == 2) {
                val pub1 = binding.etBackPub1.text.toString()
                val pub2 = binding.etBackPub2.text.toString()
                if (pub1.isEmpty()) {
                    toast("请输入第一个备份地址的公钥")
                    return@setOnClickListener
                } else if (pub2.isEmpty()) {
                    toast("请输入第二个备份地址的公钥")
                    return@setOnClickListener
                }
                myBackPub = "$pub1,$pub2"
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val chains = LitePal.where(
                    "pwallet_id=? and chain = ?",
                    walletid.toString(),
                    Walletapi.TypeBtyString
                ).find<Coin>()
                withContext(Dispatchers.Main) {
                    if (!ListUtils.isEmpty(chains)) {
                        val walletRecover = WalletRecover().apply {
                            param = WalletRecoverParam().apply {
                                ctrPubKey = chains[0].pubkey
                                backupPubKeys = myBackPub
                                addressID = 0
                                chainID = 0
                                relativeDelayTime = recoverTime
                            }
                        }
                        binding.tvNewAddress.text = walletRecover.walletRecoverAddr

                    }

                }
            }


        }
    }
}