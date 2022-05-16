package com.fzm.wallet.sdk.base

import com.fzm.wallet.sdk.db.entity.Coin

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */

/**
 * 默认币种列表
 */
internal val DEFAULT_COINS
    get() = listOf(
        Coin().apply {
            chain = "BTY"
            name = "BTY"
            platform = "bty"
            netId = "154"
        },
        Coin().apply {
            chain = "ETH"
            name = "ETH"
            platform = "ethereum"
            netId = "90"
        },
        Coin().apply {
            chain = "ETH"
            name = "USDT"
            platform = "ethereum"
            netId = "288"
        },
        Coin().apply {
            chain = "BNB"
            name = "BNB"
            platform = "bnb"
            netId = "641"
        },
    )

const val REGEX_CHINESE = "[\u4e00-\u9fa5]+"
