package com.fzm.walletmodule.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Vibrator
import android.provider.MediaStore
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import com.fzm.walletmodule.R
import com.fzm.walletmodule.event.CaptureEvent
import com.fzm.walletmodule.manager.PermissionManager
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.UriUtils
import com.fzm.walletmodule.utils.permission.EasyPermissions
import com.king.zxing.CaptureHelper
import com.king.zxing.OnCaptureCallback
import com.king.zxing.util.CodeUtils
import kotlinx.android.synthetic.main.activity_capture_custom.*
import kotlinx.android.synthetic.main.include_scan.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync

class CaptureCustomActivity : BaseActivity(), EasyPermissions.PermissionCallbacks,
    OnCaptureCallback {
    private var mRequstCode = -1
    private var mCaptureHelper: CaptureHelper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        mStatusColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_custom)
        initIntent()
        initMyToolbar()
        initScanUI()
        requestPermission()
        initListener()
    }

    override fun initListener() {
        super.initListener()
        tv_picture.setOnClickListener {
            startPhotoCode()
        }
    }

    private fun initScanUI() {
        mCaptureHelper = CaptureHelper(this, surfaceView!!, viewfinderView)
        mCaptureHelper!!.onCreate()
        mCaptureHelper!!.setOnCaptureCallback(this)
        setCaptureHelper(true, false)
        tv_tip.text = getString(R.string.my_scan_toast)


    }

    private fun setCaptureHelper(isVibrate: Boolean, isContinuous: Boolean) {
        mCaptureHelper!!.vibrate(isVibrate)
            .fullScreenScan(true)//全屏扫码
            .supportVerticalCode(true)//支持扫垂直条码，建议有此需求时才使用。
            .continuousScan(isContinuous) //连续扫码,默认false
    }

    override fun initIntent() {
        super.initIntent()
        mRequstCode = intent.getIntExtra(REQUST_CODE, -1)
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
        my_toolbar.setNavigationOnClickListener(View.OnClickListener { onBackPressed() })
    }

    private fun startPhotoCode() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, REQUEST_IMAGE)
    }


    fun requestPermission() {
        if (EasyPermissions.hasPermissions(this, *PERMISSIONS)) {

        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.home_scan_toast),
                RC_CAMERA, *PERMISSIONS
            )
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_IMAGE -> parsePhoto(data)
            }

        }
    }

    private fun asyncThread(runnable: Runnable) {
        Thread(runnable).start()
    }

    //解析相册二维码结果
    private fun parsePhoto(data: Intent) {
        val path = UriUtils.INSTANCE.getImagePath(this, data)
        if (TextUtils.isEmpty(path)) {
            return
        }
        //异步解析
        doAsync {
            val result = CodeUtils.parseCode(path)
            runOnUiThread {
                if (TextUtils.isEmpty(result)) {
                    ToastUtils.show(this@CaptureCustomActivity, getString(R.string.config_code))
                } else {
                    post(result)
                    finish()
                }
            }
        }
    }


//-----------------------权限处理-------------------------


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        reStartActivity()
    }

    //重启当前activity
    private fun reStartActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            PermissionManager(this@CaptureCustomActivity).showDialog(getString(R.string.home_scan_toast))
        }
    }


    override fun onResume() {
        super.onResume()
        if (EasyPermissions.hasPermissions(this, *PERMISSIONS)) {
            mCaptureHelper!!.onResume()
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.home_scan_toast),
                RC_CAMERA, *PERMISSIONS
            )
        }

    }

    override fun onPause() {
        super.onPause()
        mCaptureHelper!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCaptureHelper!!.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mCaptureHelper!!.onTouchEvent(event)
        return super.onTouchEvent(event)
    }



    //扫一扫二维码结果
    override fun onResultCallback(result: String): Boolean {
        post(result)
        return false
    }


    private fun showVibrator() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(100L)
    }

    private fun post(result: String) {
        EventBus.getDefault().post(CaptureEvent(mRequstCode, RESULT_SUCCESS, result))
    }


    companion object {

        val RESULT_SUCCESS = 1
        private val RC_CAMERA = 111
        val REQUEST_IMAGE = 112

        private val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val REQUST_CODE = "requstcode"

        val REQUESTCODE_OUT = 1
        val REQUESTCODE_HOME = 2
        val REQUESTCODE_CONTRACTS = 3
        val REQUESTCODE_CONTRACTS_ET = 4
        val REQUESTCODE_TRANSACTIONS = 5

    }
}