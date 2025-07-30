package com.fzm.walletdemo.ui.activity


import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.databinding.ActivityBrc20OutBinding
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.ClickUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import com.kongzue.dialogx.dialogs.MessageDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.HDWallet
import walletapi.Walletapi

@Route(path = RouterPath.APP_BRC20_OUT)
class Brc20OutActivity : BaseActivity() {


    @JvmField
    @Autowired
    var address: String? = null

    @JvmField
    @Autowired
    var inscriptionId: String? = null

    @JvmField
    @Autowired
    var tick: String? = null

    @JvmField
    @Autowired
    var amt: String? = null

    private var receive: String = ""

    private val walletViewModel by viewModel<WalletViewModel>(walletQualifier)
    private val walletRepository: WalletRepository by inject(walletQualifier)
    private val binding by lazy { ActivityBrc20OutBinding.inflate(layoutInflater) }
    private val loading by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
        initView()
        initData()
        initListener()
    }

    override fun initObserver() {
        super.initObserver()

        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            binding.etToAddress.setText(scan)
        })

        walletViewModel.out.observe(this, Observer { data ->
            Log.v("tag", "brc20 -data == $data")
            dismiss()

            val dialog = MessageDialog.build()
            dialog.isCancelable = false
            dialog.title = "转账提交成功，请等待确认..."
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
        binding.etMoney.setText("$amt $tick")
        title = "$tick"
        val pWallet: PWallet = LitePal.find(PWallet::class.java, MyWallet.getId())
        binding.tvWalletName.text = pWallet.name


        binding.ivScan.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }

        binding.btnOut.setOnClickListener {
            receive = binding.etToAddress.text.toString()
            if (receive.isEmpty()) {
                toast("请输入收款地址")
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
                        walletViewModel.out123(address, inscriptionId, tick, receive, priv, true)
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