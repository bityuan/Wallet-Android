package com.fzm.walletmodule.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fzm.walletmodule.R;
import com.fzm.walletmodule.inter.IStateView;

public class StateView extends FrameLayout implements IStateView {

    private static final String TAG = StateView.class.getSimpleName();

    private static final int NONE = 0;
    private static final int LOADING = 1;
    private static final int CUSTOM = 2;

    private int mState = NONE;

    private View mStateView;
    private View mContentView;
    private View reLoading;
    private TextView mTextView;
    private ImageView mImageView;
    private FrameLayout mAVLoadingIndicatorView;

    public StateView(Context context) {
        this(context, null);
    }

    public StateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStateView = LayoutInflater.from(getContext()).inflate(R.layout.stateview_indicator, null);
        mContentView = mStateView.findViewById(R.id.stateview_content);
        reLoading = mStateView.findViewById(R.id.reLoading);
        mTextView = mStateView.findViewById(R.id.stateview_textView);
        mImageView = mStateView.findViewById(R.id.stateview_imageView);
        mAVLoadingIndicatorView = mStateView.findViewById(R.id.fl_loading);
        //mAVLoadingIndicatorView.setIndicator("PacmanIndicator");
        // mAVLoadingIndicatorView.setIndicator(new PacmanIndicator());
        addView(mStateView, 0, new ViewGroup.LayoutParams(-1, -1));
        mStateView.setVisibility(View.GONE);
    }

    public FrameLayout getAVLoadingIndicatorView() {
        return mAVLoadingIndicatorView;
    }

    @Override
    public void showEmpty() {
        showEmpty(null, null);
    }

    public void showEmpty(OnClickListener listener) {
        showEmpty(null, listener);
    }

    public void showEmpty(String text) {
        showEmpty(text, null);
    }

    public void showWelcome(int drawableResId, String text) {
        showCustom(drawableResId, text);
    }

    @Override
    public void showEmpty(String text, OnClickListener listener) {
        showCustom(R.mipmap.bg_state_empty, text, listener);
    }

    public void showError() {
        showError(null, null);
    }

    public void showError(OnClickListener listener) {
        showError(null, listener);
    }

    public void showError(String text) {
        showError(text, null);
    }

    @Override
    public void showError(String text, OnClickListener listener) {
        showCustom(R.mipmap.bg_state_error_net, text, listener);
    }

    public void showError(int drawableResId, String text, OnClickListener listener) {
        showCustom(drawableResId, text, listener);
    }

    @Override
    public void showLoading() {
        showLoading(null);
    }

    @Override
    public void showLoading(String text) {
        showLoadingInternal(text);
    }

    public void showCustom(int drawableResId) {
        showCustom(drawableResId, null, null);
    }

    public void showCustom(int drawableResId, OnClickListener listener) {
        showCustom(drawableResId, null, listener);
    }

    @Override
    public void showCustom(int drawableResId, String text) {
        showCustom(drawableResId, text, null);
    }

    @Override
    public void showCustom(int drawableResId, String text, OnClickListener listener) {
        showCustom(getContext().getResources().getDrawable(drawableResId), text, listener);
    }

    public void showCustom(Drawable drawable) {
        showCustom(drawable, null, null);
    }

    public void showCustom(Drawable drawable, OnClickListener listener) {
        showCustom(drawable, null, listener);
    }

    public void showCustom(Drawable drawable, String text) {
        showCustom(drawable, text, null);
    }

    @Override
    public void showCustom(final Drawable drawable, final String text, final OnClickListener listener) {
        showCustomInternal(drawable, text, listener);
    }

    public void showContent() {
        showContent(null);
    }

    @Override
    public void showContentWithNoAnim() {
        if (mState == NONE) {
            return;
        }
        mState = NONE;
        showContentInternal();
    }

    @Override
    public void showContent(final Runnable runnable) {
        if (mState == NONE) {
            return;
        }
        mState = NONE;
        showViewAnimation(android.R.anim.fade_out, new Runnable() {
            @Override
            public void run() {
                showContentInternal();
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }

    private void showLoadingInternal(String text) {
        showViewInternal(null);
        if (mAVLoadingIndicatorView != null) {
            if (mAVLoadingIndicatorView.getVisibility() != View.VISIBLE) {
                mAVLoadingIndicatorView.setVisibility(View.VISIBLE);
            }
        }
        if (mImageView != null) {
            if (mImageView.getVisibility() != View.GONE) {
                mImageView.setVisibility(View.GONE);
            }
        }
        if (mTextView != null) {
            if (mTextView.getVisibility() != View.VISIBLE) {
                mTextView.setVisibility(View.VISIBLE);
            }
            mTextView.setText(text);
        }
        if (mState != LOADING) {
            showViewAnimation(android.R.anim.fade_in, null);
        }
        mState = LOADING;
        reLoading.setVisibility(View.GONE);
    }

    private void showCustomInternal(Drawable drawable, String text, OnClickListener listener) {
        showViewInternal(listener);
        if (mContentView != null) {
            mContentView.clearAnimation();
        }
        if (mAVLoadingIndicatorView != null) {
            if (mAVLoadingIndicatorView.getVisibility() != View.GONE) {
                mAVLoadingIndicatorView.setVisibility(View.GONE);
            }
        }
        if (mImageView != null) {
            if (mImageView.getVisibility() != View.VISIBLE) {
                mImageView.setVisibility(View.VISIBLE);
            }
            mImageView.setImageDrawable(drawable);
        }
        if (mTextView != null) {
            if (mTextView.getVisibility() != View.VISIBLE) {
                mTextView.setVisibility(View.VISIBLE);
            }
            mTextView.setText(text);
        }
        mState = CUSTOM;
    }

    private void showViewInternal(OnClickListener listener) {
        hideContentInternal();
        mStateView.setClickable(true);
        if (listener != null) {
            reLoading.setVisibility(VISIBLE);
        }
        reLoading.setOnClickListener(listener);
    }

    private void showViewAnimation(int animationResId, final Runnable runnable) {
        Animation animation = null;
        if (animationResId > 0) {
            try {
                animation = AnimationUtils.loadAnimation(getContext(), animationResId);
            } catch (Exception e) {
                ;
            }
        }
        if (animation != null && mContentView != null) {
            animation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mContentView.clearAnimation();
            mContentView.startAnimation(animation);
        } else {
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    private void showContentInternal() {
        mStateView.setVisibility(View.GONE);
        int childCount = getChildCount();
        for (int i = 1; i < childCount; i++) {
            getChildAt(i).setVisibility(View.VISIBLE);
        }
    }

    private void hideContentInternal() {
        mStateView.setVisibility(View.VISIBLE);
        int childCount = getChildCount();
        for (int i = 1; i < childCount; i++) {
            getChildAt(i).setVisibility(View.GONE);
        }
    }

}
