package com.fzm.walletmodule.manager

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import com.fzm.walletmodule.R
import com.fzm.walletmodule.utils.permission.AppSettingsDialog
import java.lang.ref.WeakReference

class PermissionManager(activity: AppCompatActivity) {
    private var mWeakReference: WeakReference<AppCompatActivity>? = null
    init {
        mWeakReference = WeakReference(activity)
    }

    fun showDialog(text: String) {
        val appCompatActivity = mWeakReference!!.get()
        AppSettingsDialog.Builder(appCompatActivity!!, text)
            .setTitle(appCompatActivity!!.getString(R.string.per_upload))
            .setPositiveButton(appCompatActivity.getString(R.string.ok))
            .setNegativeButton(appCompatActivity.getString(R.string.cancel), DialogInterface.OnClickListener { dialog, which -> appCompatActivity.finish() })
            .setRequestCode(REQUEST_CODE)
            .build()
            .show()
    }

    companion object {
        const val REQUEST_CODE = 1
    }
}