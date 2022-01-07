package com.fzm.wallet.sdk.bean;

import android.text.TextUtils;

import com.fzm.wallet.sdk.utils.DESUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class WithHold implements Serializable {

/*    无需代扣的返回
"data"        : {
        "id"          : 0,
                "platform"    : "",
                "address"     : "",
                "private_key" : "",
                "exer"        : ""
    }*/

    private long id;
    private String platform;
    private String address;
    private String private_key;
    private String exer;
    private double fee;
    private String tokensymbol;
    @SerializedName("bty_fee")
    private double btyFee;
    @SerializedName("coins_name")
    private String coinsName;

    public String getCoinsName() {
        return coinsName;
    }

    public void setCoinsName(String coinsName) {
        this.coinsName = coinsName;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public double getBtyFee() {
        return btyFee;
    }

    public void setBtyFee(double btyFee) {
        this.btyFee = btyFee;
    }

    public String getTokensymbol() {
        return tokensymbol;
    }

    public void setTokensymbol(String tokensymbol) {
        this.tokensymbol = tokensymbol;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPrivate_key() {
        String privateKey;
        try {
            privateKey = DESUtils.decrypt(private_key);
        } catch (Exception e) {
            return private_key;
        }

        return TextUtils.isEmpty(privateKey) ? private_key : privateKey;
    }

    public void setPrivate_key(String private_key) {
        this.private_key = private_key;
    }

    public String getExer() {
        return exer;
    }

    public void setExer(String exer) {
        this.exer = exer;
    }
}
