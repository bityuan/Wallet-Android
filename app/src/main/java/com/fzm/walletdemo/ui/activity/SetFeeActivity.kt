package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.FEE_CUSTOM_POSITION
import com.fzm.wallet.sdk.base.LIVE_KEY_FEE
import com.fzm.wallet.sdk.ext.toPlainStr
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.DGear
import com.fzm.walletdemo.Gear
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivitySetfeeBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import kotlin.math.pow

//最大gas和price 限制
@Route(path = RouterPath.APP_SETFEE)
class SetFeeActivity : BaseActivity() {
    private val va9 = 10.0.pow(9.0)
    private val va18 = 10.0.pow(18.0)
    private lateinit var mCommonAdapter: CommonAdapter<*>
    private val gearList = mutableListOf<Gear>()

    private val binding by lazy { ActivitySetfeeBinding.inflate(layoutInflater) }
    private var selectedPosition: Int = -1
    private var selectedCustom = false

    private val walletRepository: WalletRepository by inject(walletQualifier)

    @JvmField
    @Autowired(name = RouterPath.PARAM_FEE_POSITION)
    var feePosition: Int = -1

    @JvmField
    @Autowired(name = RouterPath.PARAM_CHAIN_ID)
    var chainId: Long = -1L

    @JvmField
    @Autowired(name = RouterPath.PARAM_GAS)
    var gas: Long = GoWallet.GAS_OUT

    @JvmField
    @Autowired(name = RouterPath.PARAM_GAS_PRICE)
    var gasPrice: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = getString(R.string.fee_setting)
        initView()
        initData()
        initListener()
    }

    override fun initView() {
        super.initView()
        binding.rvGear.layoutManager = LinearLayoutManager(this)
        mCommonAdapter = object : CommonAdapter<Gear>(this, R.layout.item_gear, gearList) {
            override fun convert(holder: ViewHolder, gear: Gear, position: Int) {
                holder.setText(R.id.tv_speed, gear.speed)
                holder.setText(R.id.tv_new_gas, gear.newGas)
                holder.setText(R.id.tv_content, gear.content)
                holder.setBackgroundRes(
                    R.id.ll_fee,
                    if (position == selectedPosition) R.drawable.shape_fee_selected else R.drawable.shape_fee
                )
            }
        }
        binding.rvGear.adapter = mCommonAdapter
        binding.rvGear.setOnItemClickListener { _, index ->
            chooseList(index)
        }
    }


    override fun initData() {
        super.initData()
        if (feePosition == FEE_CUSTOM_POSITION) {
            chooseCustom(1)
        } else {
            selectedPosition = feePosition
        }
        gearList.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            if (chainId == GoWallet.CHAIN_ID_BTY_L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val gasPriceResult = walletRepository.getGasPrice()
                        withContext(Dispatchers.Main) {
                            if (gasPriceResult.isSucceed()) {
                                gasPriceResult.data()?.let {
                                    val gasPrice = it.substringAfter("0x").toLong(16)
                                    addGears(gasPrice.toBigInteger())
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val web3Url = GoWallet.getWeb3UrlL(chainId)
                        val web3j = Web3j.build(HttpService(web3Url))
                        val gasPriceResult = web3j.ethGasPrice().send()
                        withContext(Dispatchers.Main) {
                            addGears(gasPriceResult.gasPrice)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }

    private fun addGears(gasPrice: BigInteger) {

        val highPrice = (gasPrice.toLong() * 1.8)
        val middlePrice = (gasPrice.toLong() * 1.4)
        val lowPrice = gasPrice.toDouble()

        val high = configGear(getString(R.string.high_str), highPrice.toLong().toBigInteger(), highPrice)
        val middle = configGear(getString(R.string.standard_str), middlePrice.toLong().toBigInteger(), middlePrice)
        val low = configGear(getString(R.string.low_str), lowPrice.toLong().toBigInteger(), lowPrice)
        gearList.add(high)
        gearList.add(middle)
        gearList.add(low)
        mCommonAdapter.notifyDataSetChanged()
    }


    private fun configGear(speed: String, gasPrice: BigInteger, gasPriceL: Double): Gear {
        val netName = GoWallet.CHAIN_ID_MAPS_L[chainId]
        val bigGas = gas.toBigInteger()
        val gasStr = gas.toString()
        val gasPriceStr = "${gasPriceL / va9}".toPlainStr(2)


        val dGas = (gasPriceL * gas) / va18
        val newGas = "$dGas".toPlainStr(6)
        val content = "$newGas $netName = Gas($gas)*GasPrice($gasPriceStr GWEI)"
        return Gear(speed, bigGas, gasPrice, gasStr, gasPriceStr, "$newGas $netName", content)
    }

    override fun initListener() {
        super.initListener()
        binding.llCustom.setOnClickListener {
            chooseCustom(2)
        }

        binding.btnOk.setOnClickListener {
            post()
            finish()
        }

    }

    private fun chooseList(index: Int) {
        selectedCustom = false
        binding.llCustom.setBackgroundResource(R.drawable.shape_fee)
        binding.llCustomInput.visibility = View.GONE
        selectedPosition = index
        mCommonAdapter.notifyDataSetChanged()
    }

    private fun chooseCustom(from: Int) {
        if (from == 1) {
            val gasPriceStr = "${gasPrice / va9}".toPlainStr(2)
            binding.etGasPrice.setText(gasPriceStr)
            binding.etGas.setText(gas.toString())

        } else {
            val gear = gearList[selectedPosition]
            binding.etGasPrice.setText(gear.gasPriceStr)
            binding.etGas.setText(gear.gasStr)
        }
        selectedPosition = -1
        mCommonAdapter.notifyDataSetChanged()
        selectedCustom = true
        binding.llCustom.setBackgroundResource(R.drawable.shape_fee_selected)
        binding.llCustomInput.visibility = View.VISIBLE
    }

    private fun post() {
        if (selectedCustom) {
            val gas = binding.etGas.text.toString()
            val gasPrice = binding.etGasPrice.text.toString()
            if (gas.isEmpty()) {
                toast("${getString(R.string.enter_str)} gas")
                return
            } else if (gasPrice.isEmpty()) {
                toast("${getString(R.string.enter_str)} gasPrice")
                return
            }
            val price = gasPrice.toLong() * va9
            val dGear =
                DGear(gas.toBigInteger(), price.toLong().toBigInteger(), FEE_CUSTOM_POSITION)
            LiveEventBus.get<DGear>(LIVE_KEY_FEE).post(dGear)
        } else {
            val gear = gearList[selectedPosition]
            val dGear = DGear(gear.gas, gear.gasPrice, selectedPosition)
            LiveEventBus.get<DGear>(LIVE_KEY_FEE).post(dGear)
        }


    }
}