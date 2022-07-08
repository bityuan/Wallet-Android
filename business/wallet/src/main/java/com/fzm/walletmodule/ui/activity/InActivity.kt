package com.fzm.walletmodule.ui.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityInBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.HtmlUtils
import com.king.zxing.util.CodeUtils
import kotlinx.android.synthetic.main.activity_in.*
import org.litepal.LitePal.find

@Route(path = RouterPath.WALLET_IN)
class InActivity : BaseActivity() {

    private val binding by lazy { ActivityInBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired
    var coin: Coin? = null

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
        coin?.let {
            binding.tvTitle.text = it.uiName + getString(R.string.home_receipt_currency)
            val pWallet: PWallet = find(PWallet::class.java, it.getpWallet().id)
            binding.tvWalletName.text = pWallet.name
            val bitmap: Bitmap = CodeUtils.createQRCode(it.address, 190)
            binding.ivMyWallet.setImageBitmap(bitmap)
            binding.tvAddress.text = HtmlUtils.change4(it.address)
        }

        binding.ivMyWallet.setOnClickListener {
            ClipboardUtils.clip(this, tv_address.text.toString())
        }
        binding.tvAddress.setOnClickListener {
            ClipboardUtils.clip(this, tv_address.text.toString())
        }


    }

}