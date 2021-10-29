package com.fzm.walletmodule.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;

public class GlideUtils {
    @SuppressLint("CheckResult")
    public static RequestOptions option(int placeholder, int fallback, int error) {
        return new RequestOptions()
                .placeholder(placeholder)
                .fallback(fallback)
                .error(error);
    }

    @SuppressLint("CheckResult")
    public static RequestOptions option(int defaultImg) {
        return new RequestOptions()
                .placeholder(defaultImg)
                .fallback(defaultImg)
                .error(defaultImg);
    }

    @SuppressLint("CheckResult")
    public static RequestOptions option(int defaultImg, BitmapTransformation transformation) {
        return option(defaultImg).transform(transformation);
    }

    @SuppressLint("CheckResult")
    public static RequestOptions option(int placeholder, int fallback, int error, BitmapTransformation transformation) {
        return option(placeholder, fallback, error).transform(transformation);
    }

    public static void into(Context context, String url, ImageView imageView) {
        Glide.with(context).load(url).into(imageView);
    }

    public static void into(Context context, RequestOptions options, String url, ImageView imageView) {
        Glide.with(context)
                .load(url)
                .apply(options)
                .into(imageView);
    }

    public static void intoQRBitmap(Context context, String url, ImageView imageView, String qrValue) {

        Glide.with(context).asBitmap().load(url).into(new BitmapImageViewTarget(imageView) {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                Bitmap bitmap = CodeUtils.createQRCode(qrValue, 200, resource);
                imageView.setImageBitmap(bitmap);
            }
        });
    }
    public static void intoQRBitmap(ImageView imageView, String qrValue) {
        Bitmap bitmap = CodeUtils.createQRCode(qrValue, 200);
        imageView.setImageBitmap(bitmap);
    }


}
