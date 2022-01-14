package com.fzm.wallet.sdk.utils

import com.fzm.wallet.sdk.db.entity.Coin

/**
 * @author zhengjy
 * @since 2022/01/14
 * Description:
 */

val Coin.totalAsset: Double get() = rmb * balance.toDouble()