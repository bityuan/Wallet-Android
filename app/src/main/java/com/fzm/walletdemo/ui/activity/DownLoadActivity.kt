package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.ProConfig
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.databinding.ActivityDownLoadBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.king.zxing.util.CodeUtils

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

    private fun getUrl(): String {
        return DOWNLOAD_URL
    }

    companion object {
        const val DOWNLOAD_URL = "https://..."
    }
}