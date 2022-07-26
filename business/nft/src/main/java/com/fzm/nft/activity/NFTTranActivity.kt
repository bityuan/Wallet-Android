package com.fzm.nft.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.fzm.nft.databinding.ActivityNfttranBinding
import com.fzm.nft.fragment.NFTTranFragment
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ClipboardUtils
import com.king.zxing.util.CodeUtils

@Route(path = RouterPath.NFT_TRAN)
class NFTTranActivity : BaseActivity() {

    @JvmField
    @Autowired
    var coin: Coin? = null

    private val binding by lazy { ActivityNfttranBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initView()
    }

     override fun initView() {
        coin?.let {
            Glide.with(this).load(it.icon).apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(binding.ivIcon)
            binding.tvBalance.text = it.balance
            binding.tvRmb.text = "￥${it.rmb}"
            val bitmap = CodeUtils.createQRCode(it.address, 200)
            binding.ivAddress.setImageBitmap(bitmap)
            binding.tvAddress.text = it.address

            val adapter = Adapter(supportFragmentManager)
            adapter.addFragment(NFTTranFragment.newInstance(0, it), "全部")
            adapter.addFragment(NFTTranFragment.newInstance(1, it), "转账")
            adapter.addFragment(NFTTranFragment.newInstance(2, it), "收款")
            binding.vpTran.adapter = adapter
            binding.tabTran.setupWithViewPager(binding.vpTran)
        }

        binding.btnTo.setOnClickListener {
            ARouter.getInstance().build(RouterPath.NFT_OUT)
                .withSerializable(RouterPath.PARAM_COIN, coin)
                .navigation()

        }
        binding.btnReceive.setOnClickListener {
            ARouter.getInstance().build(RouterPath.NFT_IN)
                .withSerializable(RouterPath.PARAM_COIN, coin)
                .navigation()
        }

        binding.tvAddress.setOnClickListener {
            ClipboardUtils.clip(this, coin?.address)
        }

    }


    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitles[position]
        }
    }

}