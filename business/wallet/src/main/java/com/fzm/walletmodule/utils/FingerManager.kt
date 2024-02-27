package com.fzm.walletmodule.utils

import androidx.appcompat.app.AppCompatActivity
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.widget.TouchIdDialog
import com.fzm.walletmodule.R
import com.tencent.soter.core.model.ConstantsSoter
import com.tencent.soter.wrapper.wrap_biometric.SoterBiometricCanceller
import com.tencent.soter.wrapper.wrap_biometric.SoterBiometricStateCallback
import com.tencent.soter.wrapper.wrap_task.AuthenticationParam

class FingerManager {

    //-------------------------------------finger-------------------------------------

    private var canceller: SoterBiometricCanceller? = null
    fun getAuthParam(act: AppCompatActivity?, showPassword: Boolean = true): AuthenticationParam {
        canceller = SoterBiometricCanceller()
        canceller!!.refreshCancellationSignal()
        val param = AuthenticationParam.AuthenticationParamBuilder()
            .setScene(IPConfig.FIGER_KEY)
            .setContext(act)
            .setBiometricType(ConstantsSoter.FINGERPRINT_AUTH)
            .setSoterBiometricCanceller(canceller)
            .setPrefilledChallenge(IPConfig.PREFILLED_CHALLENGE)
            .setSoterBiometricStateCallback(object : SoterBiometricStateCallback {
                override fun onStartAuthentication() {
                    showFingerPayDialog(act, showPassword)
                }

                override fun onAuthenticationHelp(
                    helpCode: Int,
                    helpString: CharSequence?
                ) {
                    fingerPayDialog?.dismiss()
                }

                override fun onAuthenticationSucceed() {
                    fingerPayDialog?.dismiss()
                }

                override fun onAuthenticationFailed() {
                    fingerPayDialog?.startIconShackAnimation()
                }

                override fun onAuthenticationCancelled() {
                    //用户主动取消，可能会切换密码
                    //fingerPayDialog?.dismiss()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errorString: CharSequence?
                ) {
                    fingerPayDialog?.dismiss()
                }

            }).build()


        return param

    }


    var fingerPayDialog: TouchIdDialog? = null
    private fun showFingerPayDialog(activity: AppCompatActivity?, showPassword: Boolean) {
        if (fingerPayDialog == null) {
            fingerPayDialog = activity?.let { TouchIdDialog(it, R.style.TouchIdDialog) }
            fingerPayDialog?.setOnDismissListener {
                cancelFirst()
            }
        }
        fingerPayDialog?.showPasswordPay(showPassword)
        fingerPayDialog?.show()
        fingerPayDialog?.setDialogListener {
            cancelFirst()
            upListener?.onPassword()
        }

    }


    private fun cancelFirst() {
        if (canceller == null) {
            return
        }
        if (canceller?.signalObj?.isCanceled == false) {
            canceller?.asyncCancelBiometricAuthentication()
        }
    }

    interface UserPasswordListener {
        fun onPassword()
    }

    private var upListener: UserPasswordListener? = null
    fun setOnPasswordListener(l: UserPasswordListener?) {
        this.upListener = l
    }
}