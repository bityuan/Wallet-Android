package com.fzm.walletmodule.utils

import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import walletapi.HDWallet
import java.lang.Exception

class AddCoinUtils {
    companion object {
        fun updateCoin() {

        }

        fun showPwdDialog(activity: BaseActivity, coin: Coin, pWallet: PWallet) {
            val editDialogFragment = EditDialogFragment()
            editDialogFragment.setTitle(activity.getString(R.string.my_wallet_detail_password))
            editDialogFragment.setHint(activity.getString(R.string.my_wallet_detail_password))
            editDialogFragment.setAutoDismiss(false)
            editDialogFragment.setType(1)
                .setRightButtonStr(activity.getString(R.string.ok))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {}
                    override fun onRightButtonClick(v: View?) {
                        val etInput: EditText = editDialogFragment.etInput
                        val value = etInput.text.toString()
                        if (TextUtils.isEmpty(value)) {
                            ToastUtils.show(
                                activity,
                                activity.getString(R.string.rsp_dialog_input_password)
                            )
                            return
                        }
                        editDialogFragment.dismiss()
                        handlePasswordAfter(activity, coin, value, pWallet)
                    }
                })
            editDialogFragment.showDialog("tag", activity.supportFragmentManager)
        }

        fun handlePasswordAfter(
            activity: BaseActivity,
            coin: Coin,
            password: String,
            pWallet: PWallet
        ) {
            activity.showLoading()
            doAsync {
                try {
                    val bPassword: ByteArray? = GoWallet.encPasswd(password)
                    val mnem: String = GoWallet.decMenm(bPassword!!, pWallet.mnem)
                    if (!TextUtils.isEmpty(mnem)) {
                        val hdWallet: HDWallet? = GoWallet.getHDWallet(coin.chain, mnem)
                        val address = hdWallet!!.newAddress_v2(0)
                        val pubkey = hdWallet.newKeyPub(0)
                        val pubkeyStr: String = GoWallet.encodeToStrings(pubkey)
                        coin.address = address
                        coin.pubkey = pubkeyStr
                        uiThread {
                            activity.dismiss()
                        //    updateCoin(coin, true, true)
                        }
                    } else {
                        uiThread {
                            activity.dismiss()
                            ToastUtils.show(activity,activity.getString(R.string.my_wallet_detail_wrong_password))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}