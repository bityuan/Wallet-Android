package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.kongzue.dialogx.dialogs.MessageDialog
import kotlinx.android.synthetic.main.activity_lock_test.btn_all
import kotlinx.android.synthetic.main.activity_lock_test.btn_bind
import kotlinx.android.synthetic.main.activity_lock_test.btn_ok
import kotlinx.android.synthetic.main.activity_lock_test.btn_withdraw
import kotlinx.android.synthetic.main.activity_lock_test.et_amount
import kotlinx.android.synthetic.main.activity_lock_test.et_bind_addr
import kotlinx.android.synthetic.main.activity_lock_test.et_create
import kotlinx.android.synthetic.main.activity_lock_test.et_get_amount
import kotlinx.android.synthetic.main.activity_lock_test.et_origin_addr
import kotlinx.android.synthetic.main.activity_lock_test.rg_lock
import kotlinx.android.synthetic.main.activity_lock_test.tv_address
import kotlinx.android.synthetic.main.activity_lock_test.tv_frozen_balance
import kotlinx.android.synthetic.main.activity_lock_test.tv_ticket_balance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.ContractTransferReq
import walletapi.GWithoutTx
import walletapi.WalletSendTx
import walletapi.Walletapi
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.log

class LockTestActivity : BaseActivity() {
    private val walletRepository: WalletRepository by inject(walletQualifier)

    private var type: Int = 0
    private var coin: Coin? = null

    private var balance: BigDecimal? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_test)
        initView()
    }


    override fun initView() {
        super.initView()
        coin = GoWallet.getChain(Walletapi.TypeBtyString)
        coin?.let {
            tv_address.text = it.address
            et_origin_addr.setText(it.address)
        }
        rg_lock.setOnCheckedChangeListener { radioGroup, i ->
            type = 0
            if (radioGroup.id == R.id.rb_0x) {
                coin = GoWallet.getChain(Walletapi.TypeETHString)
                coin?.let {
                    tv_address.text = it.address
                    et_origin_addr.setText(it.address)
                }

            } else if (radioGroup.id == R.id.rb_1x) {
                type = 1
                coin = GoWallet.getChain(Walletapi.TypeBtyString)
                coin?.let {
                    tv_address.text = it.address
                    et_origin_addr.setText(it.address)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            coin?.let {
                val banResult = walletRepository.chain33Balance(it.address)
                if (banResult.isSucceed()) {
                    withContext(Dispatchers.Main) {
                        val data = banResult.data()?.get(0)
                        data?.let { ba ->
                            //balance = ba.balance.div(100000000.toBigDecimal())

                            balance = ba.balance.divide(100000000.toBigDecimal(), 4, RoundingMode.HALF_UP)


                            val frozen = ba.frozen.divide(100000000.toBigDecimal(),4,RoundingMode.HALF_UP)
                            tv_ticket_balance.text = balance.toString()
                            tv_frozen_balance.text = frozen.toString()
                        }
                    }


                }

            }
        }
        btn_all.setOnClickListener {
            et_get_amount.setText(balance.toString())
        }




        btn_ok.setOnClickListener {
            val create = et_create.text.toString()
            if (create.isEmpty()) {
                toast("请输入构造数据")
                return@setOnClickListener
            }
            val mEditDialogFragment = EditDialogFragment()
            mEditDialogFragment.setType(1)
                .setRightButtonStr(getString(R.string.home_confirm))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View) {}
                    override fun onRightButtonClick(v: View) {
                        coin?.let {
                            val etPassword: EditText = mEditDialogFragment.getEtInput()
                            val password = etPassword.text.toString()
                            val localPassword: String = it.getpWallet().password
                            showLoading()
                            lifecycleScope.launch(Dispatchers.IO) {
                                val result = GoWallet.checkPasswd(password, localPassword)
                                if (result) {
                                    toSend(it, password, create)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        dismiss()
                                        toast(getString(R.string.pwd_fail_str))
                                    }
                                }
                            }
                        }

                    }
                })
            mEditDialogFragment.showDialog("tag", supportFragmentManager)
        }

        btn_bind.setOnClickListener {
            val mEditDialogFragment = EditDialogFragment()
            mEditDialogFragment.setType(1)
                .setRightButtonStr(getString(R.string.home_confirm))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View) {}
                    override fun onRightButtonClick(v: View) {
                        coin?.let {
                            val etPassword: EditText = mEditDialogFragment.getEtInput()
                            val password = etPassword.text.toString()
                            val localPassword: String = it.getpWallet().password
                            showLoading()
                            lifecycleScope.launch(Dispatchers.IO) {
                                val result = GoWallet.checkPasswd(password, localPassword)
                                if (result) {
                                    createSignSend(it, password, 1)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        dismiss()
                                        toast(getString(R.string.pwd_fail_str))
                                    }
                                }
                            }
                        }

                    }
                })
            mEditDialogFragment.showDialog("tag", supportFragmentManager)
        }
        btn_withdraw.setOnClickListener {
            val mEditDialogFragment = EditDialogFragment()
            mEditDialogFragment.setType(1)
                .setRightButtonStr(getString(R.string.home_confirm))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View) {}
                    override fun onRightButtonClick(v: View) {
                        coin?.let {
                            val etPassword: EditText = mEditDialogFragment.getEtInput()
                            val password = etPassword.text.toString()
                            val localPassword: String = it.getpWallet().password
                            showLoading()
                            lifecycleScope.launch(Dispatchers.IO) {
                                val result = GoWallet.checkPasswd(password, localPassword)
                                if (result) {
                                    createSignSend(it, password, 2)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        dismiss()
                                        toast(getString(R.string.pwd_fail_str))
                                    }
                                }
                            }
                        }

                    }
                })
            mEditDialogFragment.showDialog("tag", supportFragmentManager)
        }
    }


    private fun toSend(coin: Coin, password: String, create: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val id = MyWallet.getId()
            val wallet = LitePal.find<PWallet>(id)
            wallet?.let {
                val checked = GoWallet.checkPasswd(password, it.password)
                if (checked) {
                    val mnem =
                        GoWallet.decMenm(GoWallet.encPasswd(password)!!, coin.getpWallet().mnem)
                    val priKey = coin.getPrivkey(coin.chain, mnem)
                    var addressid = 0
                    if (type == 0) {
                        addressid = 2
                    } else {
                        addressid = 0
                    }
                    val signTx = GoWallet.signTxGroupHX(
                        null,
                        create,
                        priKey,
                        priKey,
                        0.001,
                        addressid,
                        0
                    )
                    val result = walletRepository.sendTransaction(signTx)
                    withContext(Dispatchers.Main) {
                        dismiss()
                        toast("发送成功==" + result)
                    }

                }
            }


        }

    }

    private fun createSignSend(coin: Coin, password: String, from: Int) {
        val amount = et_amount.text.toString()
        val getAmount = et_get_amount.text.toString()
        val bindAddr = et_bind_addr.text.toString()
        val originAddr = et_origin_addr.text.toString()
        if (amount.isEmpty() || bindAddr.isEmpty() || originAddr.isEmpty()) {
            toast("请输入")
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val id = MyWallet.getId()
            val wallet = LitePal.find<PWallet>(id)
            wallet?.let {
                val checked = GoWallet.checkPasswd(password, it.password)
                var createHex: String? = ""
                if (checked) {
                    when (from) {
                        1 -> {
                            val createResult =
                                walletRepository.createBindMiner(
                                    amount.toLong(),
                                    bindAddr,
                                    originAddr
                                )
                            if (createResult.isSucceed()) {
                                createHex = createResult.data()?.txHex!!
                            }
                        }

                        2 -> {
                            val createResult =
                                walletRepository.chain33CreateRaw(
                                    getAmount.toBigDecimal().multiply(100000000.toBigDecimal()).toBigInteger()
                                )
                            if (createResult.isSucceed()) {
                                createHex = createResult.data()
                            }

                        }

                    }
                }

                createHex?.let { txHex ->
                    logDebug("Create = $txHex")

                    val mnem =
                        GoWallet.decMenm(GoWallet.encPasswd(password)!!, coin.getpWallet().mnem)
                    val priKey = coin.getPrivkey(coin.chain, mnem)
                    var addressid = 0
                  /*  if (type == 0) {
                        addressid = 2
                    } else {
                        addressid = 0
                    }*/
                    val signTx = txHex.let { hex ->
                        GoWallet.signTxGroupHX(
                            null,
                            hex,
                            priKey,
                            priKey,
                            0.001,
                            addressid,
                            0
                        )
                    }
                    logDebug("signTx = $signTx")
                    val result = walletRepository.sendTransactionTest(signTx)
                    logDebug("send = $result")
                    withContext(Dispatchers.Main) {
                        toast("send = $result")
                        dismiss()
                        val dialog = MessageDialog.build()
                        dialog.title = "发送成功"
                        dialog.message = result.toString()
                        dialog.okButton = "确定"
                        dialog.setOkButtonClickListener { dialog, v ->
                            dialog.dismiss()
                            false
                        }
                        dialog.show()
                    }


                }
            }


        }

    }


}