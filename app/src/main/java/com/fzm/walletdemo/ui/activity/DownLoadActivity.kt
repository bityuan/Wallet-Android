package com.fzm.walletdemo.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.fzm.walletdemo.W
import com.fzm.walletdemo.databinding.ActivityDownLoadBinding
import com.fzm.walletdemo.ui.WalletHelper
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.king.zxing.util.CodeUtils
import org.jetbrains.anko.toast

@Route(path = RouterPath.APP_DOWNLOAD)
class DownLoadActivity : BaseActivity() {
    private val binding by lazy { ActivityDownLoadBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val downloadUrl = getUrl()
        val bitmap = CodeUtils.createQRCode(downloadUrl, 200)
        binding.ivDownlaod.setImageBitmap(bitmap)
        binding.btnCopy.setOnClickListener {
            ClipboardUtils.clip(this, downloadUrl)
        }
    }

    private fun getUrl():String{
        return when(W.appType){
            IPConfig.APP_YBC->IPConfig.DOWNLOAD_TP_YBC
            IPConfig.APP_YBS->IPConfig.DOWNLOAD_TP_YBSQ
            IPConfig.APP_YJM->IPConfig.DOWNLOAD_TP_YJ
            else -> IPConfig.DOWNLOAD_MYDAO
        }
    }
}