package com.fzm.walletdemo.ui.activity

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.utils.LocalManageUtil
import com.fzm.walletdemo.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Route(path = RouterPath.APP_SPLASH)
class SplashActivity : BaseActivity() {

    private val requestList = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        requestPermission()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestList.add(Manifest.permission.READ_MEDIA_IMAGES)
            requestList.add(Manifest.permission.READ_MEDIA_AUDIO)
            requestList.add(Manifest.permission.READ_MEDIA_VIDEO)
        }else {
            requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }


        PermissionX.init(this)
            .permissions(requestList)
            .onExplainRequestReason { scope, deniedList ->
                val message = getString(R.string.need_agree_pers)
                scope.showRequestReasonDialog(deniedList, message, getString(R.string.dialog_approve), getString(R.string.dialog_reject))
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    gotoMain()
                } else {
                    Toast.makeText(this, "${getString(R.string.refuse_pers)}：$deniedList", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun gotoMain() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ARouter.getInstance().build(RouterPath.APP_MAIN).navigation()
            finish()
        }
    }

}