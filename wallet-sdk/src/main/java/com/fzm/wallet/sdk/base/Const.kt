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
        },
        Coin().apply {
            chain = "ETH"
            name = "ETH"
            platform = "ethereum"
        },
        Coin().apply {
            chain = "BTC"
            name = "BTC"
            platform = "btc"
        },
        Coin().apply {
            chain = "ETH"
            name = "YCC"
            platform = "ethereum"
        },
        Coin().apply {
            chain = "DCR"
            name = "DCR"
            platform = "dcr"
        }
    )

const val REGEX_CHINESE = "[\u4e00-\u9fa5]+"
