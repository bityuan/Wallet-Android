package com.fzm.nft.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.databinding.ActivityNftinBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.king.zxing.util.CodeUtils

@Route(path = RouterPath.NFT_IN)
class NFTInActivity : AppCompatActivity() {
    private val binding by lazy { ActivityNftinBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired
    var coin: Coin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        doBar()
        ARouter.getInstance().inject(this)
        coin?.let {
            val bitmap = CodeUtils.createQRCode(it.address, 200)
            binding.ivAddress.setImageBitmap(bitmap)
            binding.tvAddress.text = it.address
        }
    }

    private fun doBar() {
        setSupportActionBar(binding.xbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.xbar.toolbar.setNavigationOnClickListener { onBackPressed() }
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        view.fitsSystemWindows = true
        StatusBarUtil.StatusBarLightMode(this)
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.xbar.toolbar.title = ""
        binding.xbar.tvToolbar.text = title
    }
}