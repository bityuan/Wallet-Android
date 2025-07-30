package com.fzm.walletdemo.ui.activity


import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.RouterPath.APP_BRC20_OUT
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.bean.Brc20Tran
import com.fzm.wallet.sdk.bean.TransferAble
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.databinding.ActivityBrc20OutBinding
import com.fzm.walletdemo.databinding.ActivityBrc20OutMinkeBinding
import com.fzm.walletdemo.databinding.ActivityBrc20OutPreBinding
import com.fzm.walletdemo.ui.adapter.Brc20OutPreAdapter
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.vm.WalletViewModel
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.MessageDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.HDWallet
import walletapi.Walletapi

@Route(path = RouterPath.APP_BRC20_OUT_MINKE)
class Brc20OutMinkeActivity : BaseActivity() {

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var address: String? = null

    @JvmField
    @Autowired
    var availableBalance: String? = null

    @JvmField
    @Autowired
    var transferableBalance: String? = null

    private var amt: String = ""

    private val walletViewModel by viewModel<WalletViewModel>(walletQualifier)
    private val binding by lazy { ActivityBrc20OutMinkeBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
        initView()
    }


    override fun initObserver() {
        super.initObserver()
        walletViewModel.transfer123.observe(this, Observer { data ->
            Log.v("tag", "brc20 -data == $data")
            dismiss()

            val dialog = MessageDialog.build()
            dialog.isCancelable = false
            dialog.title = "铭刻提交成功，请等待确认..."
            dialog.okButton = "确定并关闭"
            dialog.setOkButtonClickListener { dialog, v ->
                dialog.dismiss()
                finish()
                false
            }
            dialog.show()

        })
    }

    override fun initView() {
        super.initView()
        binding.tvAmt.text = availableBalance

        binding.btnOk.setOnClickListener {
            amt = binding.etAmt.text.toString()
            if (amt.isEmpty()) {
                toast("请输入铭刻数量")
                return@setOnClickListener
            }

            val editDialogFragment =
                EditDialogFragment()
            editDialogFragment.setTitle(getString(R.string.my_wallet_detail_password))
            editDialogFragment.setHint(getString(R.string.my_wallet_detail_password))
            editDialogFragment.setAutoDismiss(false)
            editDialogFragment.setType(1)
                .setRightButtonStr(getString(R.string.ok))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {}
                    override fun onRightButtonClick(v: View?) {
                        val etInput: EditText = editDialogFragment.etInput
                        val value = etInput.text.toString()
                        if (TextUtils.isEmpty(value)) {
                            toast(getString(R.string.rsp_dialog_input_password))
                            return
                        }
                        editDialogFragment.dismiss()
                        handlePasswordAfter(value)
                    }
                })
            editDialogFragment.showDialog("tag", supportFragmentManager)
        }
    }


    fun handlePasswordAfter(password: String) {
        showLoading()
        doAsync {
            try {
                val pWallet = LitePal.find<PWallet>(MyWallet.getId())
                val bPassword: ByteArray? = GoWallet.encPasswd(password)
                val mnem: String = GoWallet.decMenm(bPassword!!, pWallet!!.mnem)
                if (!TextUtils.isEmpty(mnem)) {
                    val hdWallet: HDWallet? = GoWallet.getHDWallet("BTC", mnem)
                    val priv = Walletapi.byteTohex(hdWallet!!.newKeyPriv(100))
                    uiThread {
                        walletViewModel.transfer123(
                            address!!,
                            name!!,
                            amt.toInt(),
                            priv,
                            true
                        )

                    }
                } else {
                    uiThread {
                        dismiss()
                        toast(getString(R.string.my_wallet_detail_wrong_password))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}