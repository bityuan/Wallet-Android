package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.base.LIVE_KEY_CHOOSE_CHAIN
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.exception.ImportWalletException
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.databinding.ActivityImportWalletBinding
import com.fzm.walletmodule.databinding.ViewImport0Binding
import com.fzm.walletmodule.databinding.ViewImport1Binding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.LimitEditText
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import com.google.android.material.tabs.TabLayout
import com.jeremyliao.liveeventbus.LiveEventBus
import com.snail.antifake.jni.EmulatorDetectUtil
import kotlinx.android.synthetic.main.view_import1.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.startActivity
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find
import walletapi.Walletapi
import java.util.ArrayList

class ImportWalletActivity : BaseActivity() {

    private val wallet: BWallet get() = BWallet.get()
    private val views by lazy { listOf(mnemBinding.root, privateKeyBinding.root) }
    private val binding by lazy { ActivityImportWalletBinding.inflate(layoutInflater) }
    private val mnemBinding by lazy { ViewImport0Binding.inflate(layoutInflater) }
    private val privateKeyBinding by lazy { ViewImport1Binding.inflate(layoutInflater) }
    private val titleList = listOf("导入助记词", "导入私钥")
    private var importType: Int = 0//0导入助记次 1导入私钥 2导入地址
    private var chooseChain: Coin? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initData()
        initListener()
        initObserver()
    }

    override fun initView() {
        super.initView()
        binding.viewPager.adapter = ImportAdapter()
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    override fun initData() {
        val count = LitePal.count<PWallet>()
        val name = getString(R.string.import_wallet_wallet_name) + (count + 1)
        binding.etWalletName.setText(name)
        mnemBinding.etMnem.setRegex(LimitEditText.REGEX_CHINESE_ENGLISH)
        privateKeyBinding.etInput.setRegex(LimitEditText.REGEX_ENGLISH_AND_NUM)
    }

    override fun initListener() {
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                importType = position
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

        })
        mnemBinding.etMnem.doOnTextChanged { text, start, count, after ->
            val lastString = text.toString()
            if (!TextUtils.isEmpty(lastString)) {
                val first = lastString.substring(0, 1)
                if (first.matches(LimitEditText.REGEX_CHINESE.toRegex())) {
                    val afterString = lastString.replace(" ".toRegex(), "")
                    if (!TextUtils.isEmpty(afterString)) {
                        val stringBuffer = StringBuffer()
                        for (i in afterString.indices) {
                            if ((i + 1) % 3 == 0) {
                                stringBuffer.append(afterString[i])
                                if (i != afterString.length - 1) {
                                    stringBuffer.append(" ")
                                }
                            } else {
                                stringBuffer.append(afterString[i])
                            }
                        }
                        if (!TextUtils.equals(lastString, stringBuffer)) {
                            mnemBinding.etMnem.setText(stringBuffer.toString())
                            mnemBinding.etMnem.setSelection(stringBuffer.toString().length)
                        }
                    }
                }
            }
        }

        binding.etWalletPassword.doOnTextChanged { text, start, count, after ->
            if (TextUtils.isEmpty(text)) {
                binding.tvPasswordTip.visibility = View.INVISIBLE
            } else {
                binding.tvPasswordTip.visibility = View.VISIBLE
                binding.tvPasswordTip.text = getString(R.string.set_wallet_password)
            }
        }
        binding.etWalletPasswordAgain.doOnTextChanged { text, start, count, after ->
            if (TextUtils.isEmpty(text)) {
                binding.tvPasswordAgainTip.visibility = View.INVISIBLE
            } else {
                binding.tvPasswordAgainTip.visibility = View.VISIBLE
                binding.tvPasswordAgainTip.text = getString(R.string.confirm_wallet_password)
            }
        }

        privateKeyBinding.rlChooseChain.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CHOOSE_CHAIN).navigation()
        }

        binding.btnImport.setOnClickListener {
            hideKeyboard(binding.btnImport)
            if (EmulatorDetectUtil.isEmulator(this)) {
                ToastUtils.show(this, "检测到您使用模拟器创建账户，请切换到真机")
            } else {
                finishTask()
            }

        }
    }


    override fun initObserver() {
        super.initObserver()
        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            when (importType) {
                0 -> mnemBinding.etMnem.setText(scan)
                1 -> privateKeyBinding.etInput.setText(scan)
            }

        })
        LiveEventBus.get<Coin>(LIVE_KEY_CHOOSE_CHAIN).observe(this, Observer { coin ->
            chooseChain = coin
            privateKeyBinding.tvChain.text = coin.name
            privateKeyBinding.ivChain.visibility = View.VISIBLE
            Glide.with(this).load(coin.icon).into(privateKeyBinding.ivChain)
        })
    }


    private fun finishTask() {
        if (isFastClick()) {
            return
        }
        val name = binding.etWalletName.text.toString()
        val password = binding.etWalletPassword.text.toString()
        val passwordAgain = binding.etWalletPasswordAgain.text.toString()
        if (!checked(name, password, passwordAgain)) {
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {

            var id: Long = -1
            when (importType) {
                0 -> {
                    val mnem = mnemBinding.etMnem.text.toString()
                    if (mnem.isNullOrEmpty()) {
                        ToastUtils.show(
                            this@ImportWalletActivity,
                            getString(R.string.please_input_mnem)
                        )
                        return@launch
                    }
                    val job1 = lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            showLoading()
                            id = wallet.importWallet(
                                WalletConfiguration.mnemonicWallet(
                                    mnem,
                                    name,
                                    password,
                                    Constants.getCoins()
                                )
                            )

                        } catch (e: ImportWalletException) {
                            dismiss()
                            ToastUtils.show(this@ImportWalletActivity, e.message)
                        }
                    }

                    job1.join()
                }
                1 -> {
                    val privateKey = privateKeyBinding.etInput.text.toString()
                    if (chooseChain == null) {
                        Toast.makeText(this@ImportWalletActivity, "请先选择主链", Toast.LENGTH_SHORT)
                            .show()
                        return@launch
                    } else if (privateKey.isNullOrEmpty()) {
                        ToastUtils.show(
                            this@ImportWalletActivity,
                            getString(R.string.please_input_prikey)
                        )
                        return@launch
                    }

                    chooseChain?.let { chooseChain ->
                        val job2 = lifecycleScope.launch(Dispatchers.Main) {
                            showLoading()
                            try {
                                id = wallet.importWallet(
                                    WalletConfiguration.privateKeyWallet(
                                        privateKey, name, password,
                                        listOf(chooseChain)
                                    )
                                )
                            } catch (e: ImportWalletException) {
                                dismiss()
                                ToastUtils.show(this@ImportWalletActivity, e.message)
                            }
                        }

                        job2.join()
                    }

                }
            }
            if (id != MyWallet.ID_DEFAULT) {
                MyWallet.setId(id)
                dismiss()
                LiveEventBus.get<Long>(LIVE_KEY_WALLET).post(id)
                ToastUtils.show(
                    this@ImportWalletActivity,
                    getString(R.string.my_import_success)
                )
                closeSomeActivitys()
                finish()
            }
        }

    }


    private fun checked(name: String, password: String, passwordAgain: String): Boolean {
        val pWallets = LitePal.where("name = ?", name).find<PWallet>()
        var checked = true
        if (TextUtils.isEmpty(name)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name))
            checked = false
        } else if (!ListUtils.isEmpty(pWallets)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name_exist))
            checked = false
        } else if (TextUtils.isEmpty(password)) {
            ToastUtils.show(this, getString(R.string.my_wallet_set_password))
            checked = false
        } else if (TextUtils.isEmpty(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_change_password_again))
            checked = false
        } else if (password.length !in 8..16 || passwordAgain.length !in 8..16) {
            ToastUtils.show(this, getString(R.string.my_create_letter))
            checked = false
        } else if (password != passwordAgain) {
            ToastUtils.show(this, getString(R.string.my_set_password_different))
            checked = false
        } else if (!AppUtils.ispassWord(password) || !AppUtils.ispassWord(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_set_password_number_letter))
            checked = false
        }
        return checked
    }

    inner class ImportAdapter : PagerAdapter() {

        override fun getPageTitle(position: Int): CharSequence? {
            return titleList[position]
        }

        override fun getCount(): Int {
            return titleList.size
        }

        override fun isViewFromObject(view: View, o: Any): Boolean {
            return view === o
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            container.addView(views[position])
            return views[position]
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.add(0, 1, 0, getString(R.string.my_scan))
        menuItem.setIcon(R.mipmap.import_wallet_right)
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            startActivity<CaptureCustomActivity>()
        }
        return super.onOptionsItemSelected(item)
    }
}
