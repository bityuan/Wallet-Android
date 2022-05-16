package com.fzm.walletmodule.inter;

import android.graphics.drawable.Drawable;
import android.view.View.OnClickListener;

public interface IStateView {

    void showEmpty();

    void showEmpty(String text, OnClickListener listener);

    void showError(String text, OnClickListener listener);

    void showLoading();

    void showLoading(String text);

    void showCustom(int drawableResId, String text);

    void showCustom(int drawableResId, String text, OnClickListener listener);

    void showCustom(Drawable drawable, String text, OnClickListener listener);

    void showContentWithNoAnim();

    void showContent(Runnable runnable);

}
