package com.fzm.walletmodule.ui.widget


import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.TextView
import com.fzm.walletmodule.R

class RemarksTipsDialogView(context: Context, isLocal: Boolean) {
    var context: Context? = null

    init {
        this.context = context
        showNoticeDialogCustom(isLocal)
    }

    private fun showNoticeDialogCustom(isLocal: Boolean) {
        val lDialog = Dialog(context!!, android.R.style.Theme_Translucent_NoTitleBar)
        lDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        lDialog.setContentView(R.layout.remarks_tips_dialogview)
        lDialog.setCancelable(true)
        val title = lDialog.findViewById<TextView>(R.id.title)
        val desc = lDialog.findViewById<TextView>(R.id.desc)
        if (isLocal) {
            title.text = context!!.getString(R.string.home_local_remarks)
            desc.text = context!!.getString(R.string.local_remarks_tips)
        } else {
            title.text = context!!.getString(R.string.home_chain_remarks)
            desc.text = context!!.getString(R.string.chain_remarks_tips)
        }
        (lDialog.findViewById<View>(R.id.ok) as TextView)
            .setOnClickListener { lDialog.dismiss() }
        lDialog.show()
    }

}