package com.fzm.walletmodule.db.entity;


import android.util.Log;
import com.fzm.walletmodule.event.MainCloseEvent;
import com.fzm.walletmodule.utils.MMkvUtil;
import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZX on 2018/5/24.
 */
public class PWallet extends BaseBean {
    public static final String PWALLET_ID = "pwallet_id";
    public static final String PWALLET_MNEM = "pwallet_mnem";
    public static final int TYPE_CHINESE = 1;
    public static final int TYPE_ENGLISH = 0;
    public static final int TYPE_NOMAL = 2;
    public static final int TYPE_NONE = 6;
    private int type;
    private String name;
    private String password;
    private String mnem;
    //0:英文 1:中文
    private int mnemType;
    private List<Coin> coinList = new ArrayList<>();
    private boolean putpassword;

    public boolean isPutpassword() {
        return putpassword;
    }

    public void setPutpassword(boolean putpassword) {
        this.putpassword = putpassword;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        if (password == null) {
            return "";
        }
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMnem(String mnem) {
        this.mnem = mnem;
    }

    public String getMnem() {
        return mnem;
    }

    public int getMnemType() {
        return mnemType;
    }

    public void setMnemType(int mnemType) {
        this.mnemType = mnemType;
    }

    public List<Coin> getCoinList() {
        return coinList;
    }

    public void setCoinList(List<Coin> coinList) {
        this.coinList = coinList;
    }






    public static long getUsingWalletId() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                Log.e("nyb", "getUsingWalletId:post:MainCloseEvent");
                EventBus.getDefault().post(new MainCloseEvent());
            }
        }
        return mPWallet.getId();
    }

    public static long getUseWalletId() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                mPWallet.setType(PWallet.TYPE_NONE);
            }
        }
        return mPWallet.getId();
    }

    public static PWallet getUsingWallet() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                EventBus.getDefault().post(new MainCloseEvent());
            }
        }
        return mPWallet;
    }

    public static PWallet getUseWallet() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                mPWallet.setType(PWallet.TYPE_NONE);
            }
        }
        return mPWallet;
    }

    public static void setUsingWallet(PWallet pWallet) {
        if (pWallet != null) {
            MMkvUtil.INSTANCE.encode(PWallet.PWALLET_ID, pWallet.getId());
        }
    }

    @Override
    public String toString() {
        return "PWallet{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", mnem='" + mnem + '\'' +
                ", mnemType=" + mnemType +
                ", coinList=" + coinList +
                ", putpassword=" + putpassword +
                '}';
    }
}
