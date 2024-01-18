package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ToastUtils
import com.google.zxing.Result
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.zxing.CameraScan
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.DefaultCameraScan
import com.king.zxing.analyze.MultiFormatAnalyzer
import com.king.zxing.config.ResolutionCameraConfig
import com.king.zxing.util.CodeUtils
import kotlinx.android.synthetic.main.activity_capture_custom.my_toolbar
import kotlinx.android.synthetic.main.activity_capture_custom.tv_picture
import kotlinx.android.synthetic.main.include_scan.previewView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Route(path = RouterPath.WALLET_CAPTURE)
class CaptureCustomActivity : BaseActivity(),
    CameraScan.OnScanResultCallback {
    private var mCameraScan: CameraScan? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        mStatusColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_custom)
        initMyToolbar()
        initScanUI()
        initListener()
    }

    override fun initListener() {
        super.initListener()
        tv_picture.setOnClickListener {
            startPhotoCode()
        }
    }

    private fun initScanUI() {
        setCaptureHelper()
    }

    private fun setCaptureHelper() {
        //初始化解码配置
        val decodeConfig = DecodeConfig()
        decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS).isFullAreaScan =
            true //设置是否全区域识别，默认false


        mCameraScan = DefaultCameraScan(this, previewView)
        mCameraScan!!.setOnScanResultCallback(this)
            .setAnalyzer(MultiFormatAnalyzer(decodeConfig))
            .setVibrate(true)
            .setCameraConfig(
                ResolutionCameraConfig(
                    this,
                    ResolutionCameraConfig.IMAGE_QUALITY_720P
                )
            )
            .startCamera()
    }

    private fun initMyToolbar() {
        setSupportActionBar(my_toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayUseLogoEnabled(false)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white)
        }
        my_toolbar.setNavigationOnClickListener { finish() }
    }

    private fun startPhotoCode() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, REQUEST_IMAGE)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_IMAGE -> parsePhoto(data)
            }

        }
    }


    //解析相册二维码结果
    private fun parsePhoto(data: Intent) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
        //异步解析
        lifecycleScope.launch(Dispatchers.IO) {
            val result = CodeUtils.parseCode(bitmap)
            withContext(Dispatchers.Main){
                if (TextUtils.isEmpty(result)) {
                    ToastUtils.show(this@CaptureCustomActivity, getString(R.string.config_code))
                } else {
                    post(result)
                    finish()
                }
            }
        }

    }

    //重启当前activity
    private fun reStartActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }



    private fun post(result: String) {
        LiveEventBus.get<String>(LIVE_KEY_SCAN).post(result)
    }


    companion object {
        val REQUEST_IMAGE = 112
    }

    override fun onScanResultCallback(result: Result): Boolean {
        post(result.text)
        return false
    }
}