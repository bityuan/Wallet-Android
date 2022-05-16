package com.fzm.wallet.sdk

import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import org.litepal.LitePal

/**
 * @author zhengjy
 * @since 2022/02/21
 * Description:
 */
object MnemonicManager : MnemonicStore {

    internal val DEFAULT_STORE = object : MnemonicStore {

        override suspend fun getMnemonicWords(password: String): String {
            val pass = GoWallet.encPasswd(password) ?: return ""
            val id = BWallet.get().getCurrentWallet()?.id ?: return ""
            val wallet = LitePal.find(PWallet::class.java, id) ?: return ""
            return GoWallet.decMenm(pass, wallet.mnem)
        }

        override suspend fun saveMnemonicWords(mnemonic: String, password: String): Boolean {
            val pWallet = PWallet()
            val encPasswd = GoWallet.encPasswd(password) ?: return false
            pWallet.password = GoWallet.passwdHash(encPasswd) ?: return false
            pWallet.mnem = GoWallet.encMenm(encPasswd, mnemonic) ?: return false
            pWallet.isPutpassword = true
            val id = BWallet.get().getCurrentWallet()?.id ?: return false
            pWallet.update(id)
            return true
        }

        override suspend fun checkPassword(password: String): Boolean {
            val id = BWallet.get().getCurrentWallet()?.id ?: return false
            val wallet = LitePal.find(PWallet::class.java, id) ?: return false
            return GoWallet.checkPasswd(password, wallet.password)
        }

        override fun hasPassword(): Boolean {
            val id = BWallet.get().getCurrentWallet()?.id ?: return false
            val wallet = LitePal.find(PWallet::class.java, id) ?: return false
            return wallet.isPutpassword
        }
    }

    var store: MnemonicStore = DEFAULT_STORE

    override suspend fun getMnemonicWords(password: String): String {
        return store.getMnemonicWords(password)
    }

    override suspend fun saveMnemonicWords(mnemonic: String, password: String): Boolean {
        return store.saveMnemonicWords(mnemonic, password)
    }

    override suspend fun checkPassword(password: String): Boolean {
        return store.checkPassword(password)
    }

    override fun hasPassword(): Boolean {
        return store.hasPassword()
    }
}