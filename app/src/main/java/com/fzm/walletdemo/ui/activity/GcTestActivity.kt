package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import kotlinx.android.synthetic.main.activity_gc_test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.*

class GcTestActivity : BaseActivity() {

    //平行连
    //private val dParacrossExecer = "user.p.fotchain.paracross"
    //private val dCoinsExecer = "user.p.fotchain.coins"
    //private val dNoneExecer = "user.p.fotchain.none"
    //private val dTokenSymbol = "fotchain.coins"

    //主网
    private val dParacrossExecer = "pos33"
    private val dCoinsExecer = "coins"
    private val dNoneExecer = "none"
    private val dTokenSymbol = ""

    private val dCoinType = "YCC"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gc_test)
        initView()
    }

    override fun initView() {
        super.initView()
        val coin = GoWallet.getChain(Walletapi.TypeETHString)
        coin?.let {
            tv_address.text = it.address
            getParaCrossBalance(it.address)
            tv_chain.text = dTokenSymbol

            btn_to_para.setOnClickListener {view->
                toParacross(it.address,"")
            }
        }



        btn_ok.setOnClickListener {
            val amt = et_amt.text.toString()
            if (amt.isEmpty()) {
                toast("请输入提取数量")
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
                                    todoWithDraw(it, password, amt)
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


    private fun todoWithDraw(coin: Coin, password: String, amt: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val id = MyWallet.getId()
            val wallet = LitePal.find<PWallet>(id)
            wallet?.let {
                val checked = GoWallet.checkPasswd(password, it.password)
                if (checked) {
                    val mnem = GoWallet.decMenm(GoWallet.encPasswd(password)!!, coin.getpWallet().mnem)
                    val priv = coin.getPrivkey(coin.chain, mnem)

                    val paracrossToCoinsTx = Walletapi.paracrossCoinsWithdraw(ContractTransferReq().apply {
                            amount = amt.toDouble()
                            note = "paracross->coins"
                            fromExecer = dParacrossExecer
                            toExecer = dCoinsExecer
                            chainID = 999
                            execerAddressID = 2
                        })

                    val txGroup = Walletapi.coinsWithoutTxGroup(GWithoutTx().apply {
                        feepriv = priv
                        txpriv = priv
                        noneExecer = dNoneExecer
                        rawTx = paracrossToCoinsTx
                        fee = 0.05
                        txAddressID = 2
                        feeAddressID = 2
                        execerAddressID = 2
                    })

                    val sendtx = GoWallet.sendTran(dCoinType, txGroup.signedTx, dTokenSymbol)
                    withContext(Dispatchers.Main) {
                        dismiss()
                        val view = LayoutInflater.from(this@GcTestActivity).inflate(R.layout.dialog_tip, null)
                       val dialog = AlertDialog.Builder(this@GcTestActivity).setView(view).create().apply {
                            window?.setBackgroundDrawableResource(android.R.color.transparent)
                        }
                        val tvContent = view.findViewById<TextView>(R.id.tv_content)
                        val btnOk = view.findViewById<Button>(R.id.btn_ok)
                        tvContent.text = "发送结果$sendtx"
                        btnOk.setOnClickListener {
                            dialog.dismiss()
                        }

                        dialog.show()
                    }

                }
            }


        }

    }


    private fun getParaCrossBalance(address: String) {
        try {
            val wb = WalletBalance()
            wb.cointype = dCoinType
            wb.tokenSymbol = dTokenSymbol
            wb.address = address
            wb.util = GoWallet.getUtil()
            val ei = ExtendInfo()
            ei.execer = dParacrossExecer
            wb.extendInfo = ei
            val balance = Walletapi.getbalance(wb)
            val balanceStr = Walletapi.byteTostring(balance)
            val jo = JSONObject(balanceStr)
            val result = jo.getJSONObject("result")
            val bl = result.getString("balance")
            tv_balance.text = "$bl YCC"
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }



    //转入paracross测试：
    private fun toParacross(address: String,priv:String){
        val depositTx = Walletapi.paracrossDeposit(ParaCrossReq().apply {
            addr = address
            amount = 1.0
            note = "coins->paracross"
            fromExecer = dCoinsExecer
            toExecer = dParacrossExecer
            chainID = 0
        })

        val txGroup = Walletapi.coinsWithoutTxGroup(GWithoutTx().apply {
            feepriv = ""
            feeAddressID = 0
            txpriv = priv
            txAddressID = 0
            noneExecer = dNoneExecer
            rawTx = depositTx
            fee = 0.05
        })

        val sendtx = GoWallet.sendTran(dCoinType, txGroup.signedTx, dTokenSymbol)
        Log.v("send","发送结果：$sendtx")
    }

}