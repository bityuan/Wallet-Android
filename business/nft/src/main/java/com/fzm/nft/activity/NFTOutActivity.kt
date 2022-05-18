package com.fzm.nft.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.nft.R
import com.fzm.nft.databinding.ActivityNftoutBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin

@Route(path = RouterPath.NFT_OUT)
class NFTOutActivity : AppCompatActivity() {
    private val binding by lazy { ActivityNftoutBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired
    var coin: Coin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        coin?.let {

        }
    }
}