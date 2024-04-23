package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Node
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityNodeSettingsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import org.jetbrains.anko.toast

@Route(path = RouterPath.APP_NODE_SETTINGS)
class NodeSettingsActivity : BaseActivity() {

    private val binding by lazy { ActivityNodeSettingsBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.my_node)
        initListener()
    }


    override fun initListener() {
        super.initListener()
        binding.btnOk.setOnClickListener {
           val name = binding.etName.text.toString()
           val rpcUrl = binding.etRpcUrl.text.toString()
           val chainId = binding.etChainId.text.toString()
           val symbol = binding.etSymbol.text.toString()
           val browser = binding.etBrowser.text.toString()
            val node = Node()
            node.name = name
            node.rpcUrl = rpcUrl
            node.chainId = chainId.toLong()
            node.symbol = symbol
            node.browser = browser
            //node.save()
            toast(getString(R.string.save_suc))
            finish()

        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(0, 1, 0, getString(R.string.c_entrance))
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == 1){
            toast(getString(R.string.n_yopen))
        }
        return super.onOptionsItemSelected(item)
    }
}