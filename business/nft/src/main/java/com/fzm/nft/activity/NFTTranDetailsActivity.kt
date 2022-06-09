package com.fzm.nft.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.NftTran
import com.fzm.nft.R
import com.fzm.nft.databinding.ActivityNftoutBinding
import com.fzm.nft.databinding.ActivityNfttranDetailsBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin

@Route(path = RouterPath.NFT_TRAN_DETAIL)
class NFTTranDetailsActivity : AppCompatActivity() {
    @JvmField
    @Autowired
    var nftTran: NftTran? = null

    private val binding by lazy { ActivityNfttranDetailsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)


    }
}