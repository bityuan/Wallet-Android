package com.fzm.walletdemo.ui.activity

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Route(path = RouterPath.APP_SPLASH)
class SplashActivity : AppCompatActivity() {

    private val PERMISSIONS = listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        requestPermission()
    }

    private fun requestPermission() {
        PermissionX.init(this)
            .permissions(PERMISSIONS)
            .onExplainRequestReason { scope, deniedList ->
                val message = "需要您同意以下权限才能正常使用"
                scope.showRequestReasonDialog(deniedList, message, "统一", "拒绝")
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    gotoMain()
                } else {
                    Toast.makeText(this, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
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