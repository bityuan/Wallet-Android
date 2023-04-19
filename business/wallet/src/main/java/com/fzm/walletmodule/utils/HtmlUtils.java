package com.fzm.walletmodule.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;

import com.fzm.walletmodule.R;


/**
 * Created by zx on 2016/6/16.
 */
public class HtmlUtils {


    public static String change(String text1, String text2) {
        String result = "<html>\n" +
                " <body>\n" +
                "  <font color=#333649>" + text1 + "</font><font color=#7190FF>" + text2 + "</font>\n" +
                " </body>\n" +
                "</html>";
        //Html.fromHtml会在结尾添加空格,所以需要trim
        return Html.fromHtml(result).toString().trim();
    }

    public static String change4(String string) {
        String substringLeft = string.substring(0, string.length() - 4);
        String substringRight = string.substring(string.length() - 4, string.length());
        return change(substringLeft, substringRight);
    }

}
