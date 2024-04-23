package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityRpcBinding
import com.fzm.walletdemo.web3.bean.NodeRPC
import com.fzm.walletmodule.ui.base.BaseActivity
import com.kongzue.dialogx.dialogs.InputDialog
import com.kongzue.dialogx.dialogs.PopTip
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import org.jetbrains.anko.toast

@Route(path = RouterPath.APP_RPC)
class RPCActivity : BaseActivity() {

    private val binding by lazy { ActivityRpcBinding.inflate(layoutInflater) }
    private lateinit var rpcAdapter: CommonAdapter<*>
    private val rpcList = mutableListOf<NodeRPC>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.my_node)
        initView()
        initListener()
    }

    override fun initView() {
        super.initView()
        rpcAdapter = object : CommonAdapter<NodeRPC>(this, R.layout.item_rpc, rpcList) {
            override fun convert(holder: ViewHolder, rpc: NodeRPC, position: Int) {
                holder.setText(R.id.tv_rpc, rpc.rpc)
            }

        }
        binding.rvRpcList.layoutManager = LinearLayoutManager(this)
        binding.rvRpcList.adapter = rpcAdapter
    }


    override fun initListener() {
        super.initListener()
        binding.btnAddRpc.setOnClickListener {

            InputDialog(
                "RPC 地址","请输入RPC地址",
                getString(R.string.ok),
                getString(R.string.cancel),
                "https://"
            )
                .setOkButton { baseDialog, v, inputStr ->
                    val nr = NodeRPC()
                    nr.rpc = inputStr
                    rpcList.add(nr)
                    rpcAdapter.notifyItemInserted(rpcList.size - 1)
                    false
                }
                .show()


        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(0, 1, 0, "便捷入口")
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            toast("暂未开放")
        }
        return super.onOptionsItemSelected(item)
    }
}