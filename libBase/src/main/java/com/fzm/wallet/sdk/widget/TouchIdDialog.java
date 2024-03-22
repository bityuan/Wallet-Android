package com.fzm.wallet.sdk.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fzm.wallet.sdk.R;


/**
 * author: NYB
 * created on: 2020-02-11
 * description:
 */
public class TouchIdDialog extends Dialog implements View.OnClickListener {
    private View mCancelTv;
    private TextView mPassWordTv;
    private Context mContext;
    private ImageView mTouchIdImg;

    private DialogListener dialogListener;
    private boolean showPasswordPay = true;

    public interface DialogListener {
        void whichClick();
    }

    public void setDialogListener(DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    public TouchIdDialog(@NonNull Context context) {
        this(context, 0);
    }

    public TouchIdDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_touch_id);
        initView();
    }

    private void initView() {
        mCancelTv = findViewById(R.id.close);
        mCancelTv.setOnClickListener(this);
        mPassWordTv = findViewById(R.id.passwordPay);
        mPassWordTv.setOnClickListener(this);
        mTouchIdImg = findViewById(R.id.fingerImg);
        mPassWordTv.setVisibility(showPasswordPay ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close) {
            TouchIdDialog.this.dismiss();
        } else if (id == R.id.passwordPay) {
            TouchIdDialog.this.dismiss();
            if (dialogListener != null) {
                dialogListener.whichClick();
            }
        }
    }

    public void showPasswordPay(boolean showPasswordPay) {
        this.showPasswordPay = showPasswordPay;
    }

    public void startIconShackAnimation() {
        Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.shack_animation);
        anim.setFillAfter(false);
        mTouchIdImg.startAnimation(anim);
    }

}