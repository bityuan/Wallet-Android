package com.fzm.walletmodule.update;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fzm.walletmodule.R;


public class UpdateDialogFragment extends DialogFragment {

    private TextView mTvResult;
    private TextView mTvResultDetails;
    private Button mBtnLeft;
    private Button mBtnRight;

    private String mResult;
    private String mResultDetails;
//    private String mLeftButtonStr;
//    private String mRightButtonStr;
    private int type = 2;// 1=1个按钮，2=2个按钮
    private int resultColor = -1;
    private AlertDialog mAlertDialog;
    private boolean isAutoDismiss = true;

    public UpdateDialogFragment setAutoDismiss(boolean autoDismiss) {
        isAutoDismiss = autoDismiss;
        return this;
    }

    public int getResultColor() {
        return resultColor;
    }

    public UpdateDialogFragment setResultColor(int resultColor) {
        this.resultColor = resultColor;
        return this;
    }

    public TextView getTvResult() {
        return mTvResult;
    }

    public int getType() {
        return type;
    }

    public UpdateDialogFragment setType(int type) {
        this.type = type;
        return this;
    }

    private OnButtonClickListener mOnButtonClickListener;

    public UpdateDialogFragment setResult(String result) {
        this.mResult = result;
        return this;
    }

    public UpdateDialogFragment setResultDetails(String resultDetails) {
        this.mResultDetails = resultDetails;
        return this;
    }


    public static UpdateDialogFragment newInstance() {
        UpdateDialogFragment fragment = new UpdateDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static UpdateDialogFragment newInstance(int _type) {
        UpdateDialogFragment fragment = new UpdateDialogFragment();
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
        View rootView = inflater.inflate(R.layout.dialog_fragment_updata, null);
        mTvResult = (TextView) rootView.findViewById(R.id.tv_result);
        mTvResultDetails = (TextView) rootView.findViewById(R.id.tv_result_details);
        mBtnLeft = (Button) rootView.findViewById(R.id.btn_left);
        mBtnRight = (Button) rootView.findViewById(R.id.btn_right);
        mTvResult.setText(mResult);
        if (resultColor != -1) {
            mTvResult.setTextColor(resultColor);
        }
        mTvResultDetails.setText(mResultDetails);
        if (TextUtils.isEmpty(mResultDetails)) {
            mTvResultDetails.setVisibility(View.GONE);
        } else {
            mTvResultDetails.setVisibility(View.VISIBLE);
        }
//        mBtnLeft.setText(mLeftButtonStr);
//        mBtnRight.setText(mRightButtonStr);
        mBtnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAutoDismiss) {
                    dismiss();
                }
                doLeftButtonClick(v);
            }
        });
        mBtnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAutoDismiss) {
                    dismiss();
                }
                doRightButtonClick(v);
            }
        });
        builder.setView(rootView);
        if (type == 1) {
            mBtnLeft.setVisibility(View.GONE);
        } else {
            mBtnLeft.setVisibility(View.VISIBLE);
        }
        mAlertDialog = builder.create();
        mAlertDialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        //alertDialog.show();
        return mAlertDialog;
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

    public void showDialog(String tag, FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    public boolean isShowing() {
        return mAlertDialog.isShowing();
    }

}