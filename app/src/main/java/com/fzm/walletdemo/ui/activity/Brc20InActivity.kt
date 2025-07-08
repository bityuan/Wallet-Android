package com.fzm.walletdemo.ui.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.databinding.ActivityBrc20InBinding
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.HtmlUtils
import com.king.zxing.util.CodeUtils
import org.litepal.LitePal.find

@Route(path = RouterPath.APP_BRC20_IN)
class Brc20InActivity : BaseActivity() {

    private val binding by lazy { ActivityBrc20InBinding.inflate(layoutInflater) }


    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var address: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        mStatusColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        setToolBar(R.id.toolbar, R.id.tv_title)
        initView()
    }


    override fun initView() {
        binding.tvTitle.text = "$name ${getString(R.string.home_receipt_currency)}"
        val pWallet: PWallet = find(PWallet::class.java, MyWallet.getId())
        binding.tvWalletName.text = pWallet.name
        val bitmap: Bitmap = CodeUtils.createQRCode(address, 190)
        binding.ivAddress.setImageBitmap(bitmap)
        binding.tvAddress.text = HtmlUtils.change4(address)


        binding.ivAddress.setOnClickListener {
            ClipboardUtils.clip(this, binding.tvAddress.text.toString())
        }
        binding.tvAddress.setOnClickListener {
            ClipboardUtils.clip(this, binding.tvAddress.text.toString())
        }

    }


}