package com.fzm.walletmodule.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import androidx.appcompat.widget.AppCompatEditText;


public class LimitEditText extends AppCompatEditText {

    //默认只能输入汉字
    private String mRegex = REGEX_CHINESE;


    public static final String REGEX_CHINESE = "[\u4e00-\u9fa5]+";
    //A前面要有个空格，注意
    public static final String REGEX_ENGLISH = "^[ A-Za-z]*$";

    //A前面要有个空格，注意
    public static final String REGEX_CHINESE_ENGLISH = "^[\u4e00-\u9fa5 A-Za-z]*$";

    //私钥公钥地址的正则
    public static final String REGEX_ENGLISH_AND_NUM = "^[A-Za-z0-9]*$";

    public void setRegex(String regex) {
        mRegex = regex;
    }

    /**
     * 是否符合正则，复制进来的字符串可能不匹配
     * @return
     */
    public boolean isRegex() {
        return getText().toString().matches(mRegex);
    }

    public LimitEditText(Context context) {
        super(context);
    }

    public LimitEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LimitEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new mInputConnecttion(super.onCreateInputConnection(outAttrs),
                false);
    }

    class mInputConnecttion extends InputConnectionWrapper implements
            InputConnection {

        public mInputConnecttion(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {

            if (!text.toString().matches(mRegex)) {
                return false;
            }
            return super.commitText(text, newCursorPosition);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean setSelection(int start, int end) {
            return super.setSelection(start, end);
        }

    }


}
