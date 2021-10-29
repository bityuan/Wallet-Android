package com.fzm.walletmodule.ui.widget

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.fzm.walletmodule.R


class LoadingView constructor(val cancel: Boolean = false, var full: Boolean = false) : DialogFragment() {


    fun setFullscreen(full: Boolean) {
        this.full = full
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (full) {
            setStyle(STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar_Fullscreen);
        }
    }
/*    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val loadingView = activity?.layoutInflater?.inflate(R.layout.layout_loading, null)
        val builder = AlertDialog.Builder(context)
        builder.setView(loadingView)
        val dialog = builder.create()
        val window = dialog.getWindow()
        window!!.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = cancel
        return dialog
    }*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val window = this.dialog!!.window
        isCancelable = cancel
        if (!full) {
            window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }
        val view = inflater.inflate(R.layout.layout_loading, null) //自己的布局文件
        return view
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            manager.beginTransaction().remove(this).commit()
            super.show(manager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}

fun configWindow(alertDialog: AlertDialog) {
    configWindow(alertDialog, Gravity.BOTTOM)
}

fun configWindow(alertDialog: AlertDialog, gravity: Int) {
    val window = alertDialog.window
    window?.setBackgroundDrawableResource(android.R.color.transparent)
    window?.decorView?.setPadding(0, 0, 0, 0)
    window?.setGravity(gravity)
    val lp = window?.attributes
    lp?.width = WindowManager.LayoutParams.MATCH_PARENT
    lp?.height = WindowManager.LayoutParams.WRAP_CONTENT
    window?.attributes = lp
}
fun configWindowDim(alertDialog: AlertDialog, gravity: Int) {
    val window = alertDialog.window
    window?.setBackgroundDrawableResource(android.R.color.transparent)
    window?.decorView?.setPadding(0, 0, 0, 0)
    window?.setGravity(gravity)
    window?.setDimAmount(0f)//去除蒙层
    val lp = window?.attributes
    lp?.width = WindowManager.LayoutParams.MATCH_PARENT
    lp?.height = WindowManager.LayoutParams.WRAP_CONTENT
    window?.attributes = lp
}
