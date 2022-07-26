package com.fzm.nft.activity

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.databinding.ActivityNftinBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.king.zxing.util.CodeUtils

@Route(path = RouterPath.NFT_IN)
class NFTInActivity : BaseActivity() {
    private val binding by lazy { ActivityNftinBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired
    var coin: Coin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        coin?.let {
            val bitmap = CodeUtils.createQRCode(it.address, 200)
            binding.ivAddress.setImageBitmap(bitmap)
            binding.tvAddress.text = it.address
            binding.ivAddress.setOnClickListener { _ ->
                ClipboardUtils.clip(this, it.address)
            }
            binding.tvAddress.setOnClickListener { _ ->
                ClipboardUtils.clip(this, it.address)
            }
        }
    }
}