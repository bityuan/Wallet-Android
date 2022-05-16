package com.fzm.walletmodule.ui.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fzm.walletmodule.R;


public class CommonDialogFragment extends DialogFragment {

    private TextView mTvResult;
    private TextView mTvResultDetails;
    private Button mBtnLeft;
    private Button mBtnRight;

    private String mResult;
    private String mResultDetails;
    private String mLeftButtonStr;
    private String mRightButtonStr;
    private DialogInterface.OnDismissListener mOnDismissListener;
    private int type = 2;  // 1= 1个按钮  2= 2个按钮
    private int resultColor = -1;
    private View mVMiddleLine;
    private AlertDialog mAlertDialog;
    private boolean isAutoDismiss = true;
    private boolean isShowClose = false;

    public void setShowClose(boolean showClose) {
        isShowClose = showClose;
    }

    public CommonDialogFragment setAutoDismiss(boolean autoDismiss) {
        isAutoDismiss = autoDismiss;
        return this;
    }

    public int getResultColor() {
        return resultColor;
    }

    public CommonDialogFragment setResultColor(int resultColor) {
        this.resultColor = resultColor;
        return this;
    }

    public TextView getTvResult() {
        return mTvResult;
    }

    public int getType() {
        return type;
    }

    public CommonDialogFragment setType(int type) {
        this.type = type;
        return this;
    }

    private OnButtonClickListener mOnButtonClickListener;

    public CommonDialogFragment setResult(String result) {
        this.mResult = result;
        return this;
    }

    public CommonDialogFragment setResultDetails(String resultDetails) {
        this.mResultDetails = resultDetails;
        return this;
    }

    public CommonDialogFragment setLeftButtonStr(String leftButtonStr) {
        this.mLeftButtonStr = leftButtonStr;
        return this;
    }

    public CommonDialogFragment setRightButtonStr(String rightButtonStr) {
        this.mRightButtonStr = rightButtonStr;
        return this;
    }

    public CommonDialogFragment setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.mOnDismissListener = listener;
        return this;
    }

    public static CommonDialogFragment newInstance() {
        CommonDialogFragment fragment = new CommonDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static CommonDialogFragment newInstance(int _type) {
        CommonDialogFragment fragment = new CommonDialogFragment();
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
        View rootView = inflater.inflate(R.layout.dialog_fragment_common, null);
        ImageView ivClose = (ImageView) rootView.findViewById(R.id.iv_close);
        ivClose.setVisibility(isShowClose ? View.VISIBLE : View.GONE);
        mTvResult = (TextView) rootView.findViewById(R.id.tv_result);
        mTvResultDetails = (TextView) rootView.findViewById(R.id.tv_result_details);
        mBtnLeft = (Button) rootView.findViewById(R.id.btn_left);
        mBtnRight = (Button) rootView.findViewById(R.id.btn_right);
        mVMiddleLine = rootView.findViewById(R.id.v_middle_line);
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
        mBtnLeft.setText(mLeftButtonStr);
        mBtnRight.setText(mRightButtonStr);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
        mBtnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAutoDismiss) {
                    dismissAllowingStateLoss();
                }
                doLeftButtonClick(v);
            }
        });
        mBtnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAutoDismiss) {
                    dismissAllowingStateLoss();
                }
                doRightButtonClick(v);
            }
        });
        builder.setView(rootView);
        if (type == 1) {
            mBtnLeft.setVisibility(View.GONE);
            mVMiddleLine.setVisibility(View.GONE);
            mBtnRight.setBackgroundResource(R.drawable.selector_dialog_btn_bottom);
        } else {
            mBtnLeft.setVisibility(View.VISIBLE);
            mBtnRight.setBackgroundResource(R.drawable.selector_dialog_btn_right);
        }
        mAlertDialog = builder.create();
        mAlertDialog.setOnDismissListener(mOnDismissListener);
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