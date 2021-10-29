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


    public static Spanned changeOrg(String text1, String text2, Drawable drawable) {
        String result = "<html>\n" +
                " <body>\n" +
                "  <font color=#ffffff>" + text1 + "</font><font color=#c7e5ff>" + text2 + "</font>\n" + drawable +
                " </body>\n" +
                "</html>";

        return Html.fromHtml(result);
    }

    public static Spanned change(String text1, String text2) {
        String result = "<html>\n" +
                " <body>\n" +
                "  <font color=#333649>" + text1 + "</font><font color=#7190FF>" + text2 + "</font>\n" +
                " </body>\n" +
                "</html>";

        return Html.fromHtml(result);
    }

    public static Spanned change4(String string) {

        String substringLeft = string.substring(0, string.length() - 4);
        String substringRight = string.substring(string.length() - 4, string.length());
        Spanned spanned = change(substringLeft, substringRight);
        return spanned;
    }

    public static Spanned change5(Context context, String string) {
        //setSpan插入内容的时候，起始位置不替换，会替换起始位置到终止位置间的内容，含终止位置。
        //Spanned.SPAN_EXCLUSIVE_EXCLUSIVE模式用来控制是否同步设置新插入的内容与start/end 位置的字体样式，此处没设置具体字体，所以可以随意设置
//        spannableString.setSpan(imageSpan, string.length()-4, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        Drawable drawable = context.getResources().getDrawable(R.mipmap.icon_copy_default);
        drawable.setBounds(0, 0, 35, 35);
        ImageSpan span = new ImageSpan(drawable);
        String substringLeft = string.substring(0, string.length() - 4);
        String substringRight = string.substring(string.length() - 4, string.length());
        SpannableString spannable = new SpannableString(substringLeft + substringRight + "aa");
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#2F86F2")), spannable.length() - 6, spannable.length() - 2, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(span, spannable.length() - 2, spannable.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    public static Spanned change4grey(String string) {

        String substringLeft = string.substring(0, string.length() - 4);
        String substringRight = string.substring(string.length() - 4, string.length());
        Spanned spanned = changeGrey(substringLeft, substringRight);
        return spanned;
    }

    public static Spanned changeGrey(String text1, String text2) {
        String result = "<html>\n" +
                " <body>\n" +
                "  <font color=#8E92A3>" + text1 + "</font><font color=#7190FF>" + text2 + "</font>\n" +
                " </body>\n" +
                "</html>";

        return Html.fromHtml(result);
    }

}
