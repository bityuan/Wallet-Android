package com.fzm.wallet.sdk

import com.fzm.wallet.sdk.db.entity.PWallet
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2022/01/20
 * Description:
 */
data class WalletBean(
    /**
     * 账户id
     */
    val id: Long,
    /**
     * 账户名称
     */
    val name: String,
    /**
     * 账户所属用户
     */
    val user: String,
    /**
     * 账户类型
     * 2：普通账户
     * 4：私钥账户
     */
    val type: Int,
    /**
     * 助记词
     */
    val mnem: String,
    /**
     * 密码
     */
    val password: String,
) : Serializable

internal fun PWallet.toWalletBean(): WalletBean {
    return WalletBean(id, name, user, type, mnem, password)
}