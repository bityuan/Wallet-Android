package com.fzm.wallet.sdk.db.entity;

import android.text.TextUtils;

import com.fzm.wallet.sdk.bean.WithHold;
import com.fzm.wallet.sdk.utils.GoWallet;
import com.google.gson.annotations.SerializedName;

import org.litepal.annotation.Column;

import walletapi.HDWallet;
import walletapi.Walletapi;

/**
 * Created by ZX on 2018/5/30.
 */

public class Coin extends BaseBean implements Comparable<Coin>{

    public static final int STATUS_ENABLE = 1;
    public static final int STATUS_DISABLE = -1;
    //静态的私钥
    public static String mPriv;
    public static String webPriv;
    //代扣私钥内存缓存
    public static WithHold withHold;

    private PWallet pWallet;
    //拥有量
    private String balance;
    private String dexBalance;
    private String dexFrozenBalance;
    //启用：1 禁用：0
    private int status;
    private String privkey;
    private String pubkey;
    //过滤服务器返回的地址
    @SerializedName("abdce_a")
    private String address;
    private String icon;
    private String name;
    private String nickname;
    private String platform;
    private String chain;
    //合约类型
    //1、token
    //2、coins
    private String treaty;
    @SerializedName("optional_name")
    private String optionalName;
    @Column(ignore = true)
    private String scanAddress;
    private float rmb;
    @SerializedName("id")
    private String netId;
    private int sort;

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getNetId() {
        return netId;
    }

    public void setNetId(String netId) {
        this.netId = netId;
    }

    public float getRmb() {
        return rmb;
    }

    public void setRmb(float rmb) {
        this.rmb = rmb;
    }

    public String getScanAddress() {
        return scanAddress;
    }

    public void setScanAddress(String scanAddress) {
        this.scanAddress = scanAddress;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public PWallet getpWallet() {
        return pWallet;
    }

    public void setpWallet(PWallet pWallet) {
        this.pWallet = pWallet;
    }

    public String getBalance() {
        if (TextUtils.isEmpty(balance) || TextUtils.equals(balance, "%!f(int=0000)")) {
            return "0";
        }
        return balance;
    }

    public void setBalance(String balance) {
        if (TextUtils.isEmpty(balance)) {
            balance = "0";
        }
        this.balance = balance;
    }

    public String getDexBalance() {
        if (TextUtils.isEmpty(dexBalance) || TextUtils.equals(dexBalance, "%!f(int=0000)")) {
            return "0";
        }
        return dexBalance;
    }

    public void setDexBalance(String dexBalance) {
        if (TextUtils.isEmpty(dexBalance)) {
            dexBalance = "0";
        }
        this.dexBalance = dexBalance;
    }

    public String getDexFrozenBalance() {
        if (TextUtils.isEmpty(dexFrozenBalance) || TextUtils.equals(dexFrozenBalance, "%!f(int=0000)")) {
            return "0";
        }
        return dexFrozenBalance;
    }

    public void setDexFrozenBalance(String dexFrozenBalance) {
        if (TextUtils.isEmpty(dexFrozenBalance)) {
            dexFrozenBalance = "0";
        }
        this.dexFrozenBalance = dexFrozenBalance;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPrivkey(String coinType, String mnem) {
        HDWallet hdWallet = GoWallet.getHDWallet(coinType, mnem);
        try {
            byte[] bPrivkey = hdWallet.newKeyPriv(0);
            return Walletapi.byteTohex(bPrivkey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置加密后的私钥
     *
     * @param privkey
     */
    public void setPrivkey(String privkey) {
        this.privkey = privkey;
    }


    /**
     * 获取加密后的私钥
     *
     * @return
     */
    public String getEncPrivkey() {
        return privkey;
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getUIName() {
        return TextUtils.isEmpty(optionalName) ? name : optionalName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getTreaty() {
        return treaty;
    }

    public void setTreaty(String treaty) {
        this.treaty = treaty;
    }

    public String getOptionalName() {
        return optionalName;
    }

    public void setOptionalName(String optionalName) {
        this.optionalName = optionalName;
    }


    @Override
    public int compareTo(Coin o) {
        int l = sort - o.getSort();
        return l;
    }
}
