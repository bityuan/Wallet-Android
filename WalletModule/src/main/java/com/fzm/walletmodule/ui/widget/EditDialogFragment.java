package com.fzm.walletmodule.ui.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fzm.walletmodule.R;


public class EditDialogFragment extends DialogFragment {

    private Button mBtnLeft;
    private Button mBtnRight;

    private String mLeftButtonStr;
    private String mTitle;
    private String mHint;
    private String mInput;
    //要一起写才能气作用
    private int mInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private int maxlength = 0;
    private String mRightButtonStr;
    private int type = 2; // 1= 1个按钮  2= 2个按钮
    private EditText mEtInput;
    private TextView mTvTitle;
    private boolean isAutoDismiss = true;
    private boolean goneClose = false;

    public void setMaxlength(int maxlength) {
        this.maxlength = maxlength;
    }

    public void setGoneClose(boolean goneClose) {
        this.goneClose = goneClose;
    }

    public void setAutoDismiss(boolean autoDismiss) {
        isAutoDismiss = autoDismiss;
    }

    public void setInputType(int inputType) {
        mInputType = inputType;
    }

    public void setInput(String input) {
        mInput = input;
    }

    public void setHint(String hint) {
        mHint = hint;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public EditText getEtInput() {
        return mEtInput;
    }

    public int getType() {
        return type;
    }

    public EditDialogFragment setType(int type) {
        this.type = type;
        return this;
    }

    private OnButtonClickListener mOnButtonClickListener;
    private OnCloseListener mOnCloseListener;

    public EditDialogFragment setOnCloseListener(OnCloseListener onCloseListener) {
        mOnCloseListener = onCloseListener;
        return this;
    }

    public EditDialogFragment setLeftButtonStr(String leftButtonStr) {
        this.mLeftButtonStr = leftButtonStr;
        return this;
    }

    public EditDialogFragment setRightButtonStr(String rightButtonStr) {
        this.mRightButtonStr = rightButtonStr;
        return this;
    }

    public static EditDialogFragment newInstance() {
       EditDialogFragment fragment = new EditDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static EditDialogFragment newInstance(int _type) {
       EditDialogFragment fragment = new EditDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.setType(_type);
        return fragment;
    }

    public void setOnButtonClickListener(OnButtonClickListener l) {
        this.mOnButtonClickListener = l;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.dialog_fragment_edit, null);


        ImageView ivClose = (ImageView) rootView.findViewById(R.id.iv_close);
        mTvTitle = (TextView) rootView.findViewById(R.id.tv_title);
        mEtInput = (EditText) rootView.findViewById(R.id.et_input);
        mBtnLeft = (Button) rootView.findViewById(R.id.btn_left);
        mBtnRight = (Button) rootView.findViewById(R.id.btn_right);
        if (!TextUtils.isEmpty(mTitle)) {
            mTvTitle.setText(mTitle);
            if (mTitle.length() >= 8) {
                mTvTitle.setTextSize(12);
            } else if (mTitle.length() >= 6) {
                mTvTitle.setTextSize(14);
            }
        }
        if (TextUtils.isEmpty(mInput)) {
            mEtInput.setHint(mHint);
            if (!TextUtils.isEmpty(mHint) && mHint.length() > 20) {
                mEtInput.setTextSize(15);
            }
        } else {
            mEtInput.setText(mInput);
        }
        if (maxlength != 0) {
            mEtInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxlength)});
        }
        mEtInput.setInputType(mInputType);
        mEtInput.setTypeface(Typeface.DEFAULT);
        if (mInputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD) || mInputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            mEtInput.setTransformationMethod(new PasswordTransformationMethod());
        }
        if (!TextUtils.isEmpty(mInput)) {
            mEtInput.setSelection(mInput.length());
        }
        mBtnLeft.setText(mLeftButtonStr);
        mBtnRight.setText(mRightButtonStr);
        ivClose.setVisibility(goneClose ? View.INVISIBLE : View.VISIBLE);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                doClose();
            }
        });
        mBtnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAutoDismiss) {
                    dismiss();
                }
                doLeftButtonClick(v);
            }
        });
        mBtnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAutoDismiss) {
                    dismiss();
                }
                doRightButtonClick(v);
            }
        });
        builder.setView(rootView);
        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        //alertDialog.show();
        return alertDialog;
    }

    private void doLeftButtonClick(View v) {
        if (mOnButtonClickListener != null) {
            mOnButtonClickListener.onLeftButtonClick(v);
        }
    }

    private void doRightButtonClick(View v) {
        if (mOnButtonClickListener != null) {
            mOnButtonClickListener.onRightButtonClick(v);
        }
    }

    public interface OnButtonClickListener {
        void onLeftButtonClick(View v);

        void onRightButtonClick(View v);
    }

    private void doClose() {
        if (mOnCloseListener != null) {
            mOnCloseListener.close();
        }
    }

    public interface OnCloseListener {
        void close();
    }

    public void showDialog(String tag, FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

}