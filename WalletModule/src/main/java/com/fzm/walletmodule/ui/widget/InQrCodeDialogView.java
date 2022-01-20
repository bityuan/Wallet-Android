package com.fzm.walletmodule.ui.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.fzm.walletmodule.R;
import com.fzm.walletmodule.utils.GlideUtils;
import com.fzm.walletmodule.utils.HtmlUtils;
import com.fzm.walletmodule.utils.ToastUtils;
import com.king.zxing.util.CodeUtils;

public class InQrCodeDialogView {

    private Activity context;

    private Dialog lDialog;

    public Context getContext() {
        return context;
    }

    public void setContext(Activity context) {
        this.context = context;
    }


    public InQrCodeDialogView(Activity context, String address, String imgUrl) {
        this.context = context;
        showNoticeDialogCustom(address, imgUrl);

    }

    private void showNoticeDialogCustom(final String url, final String imgUrl) {
        lDialog = new Dialog(context,
                android.R.style.Theme_Translucent_NoTitleBar);
        lDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        lDialog.setContentView(R.layout.dialog_in_qr_code);
        lDialog.setCancelable(true);
        final ImageView imageView = lDialog.findViewById(R.id.image);
        if (!TextUtils.isEmpty(url)) {
            TextView addressTv = lDialog.findViewById(R.id.tv_address);
            addressTv.setText(HtmlUtils.change4(url));
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_app);
            Bitmap bitmap = CodeUtils.createQRCode(url, 190, logo);
            imageView.setImageBitmap(bitmap);
            if (TextUtils.isEmpty(imgUrl)) {
                GlideUtils.intoQRBitmap(imageView, url);
            } else {
                GlideUtils.intoQRBitmap(context, imgUrl, imageView, url);
            }
            addressTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager cm = (ClipboardManager) context
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData mClipData = ClipData.newPlainText("Label", url);
                    cm.setPrimaryClip(mClipData);
                    ToastUtils.show(context, R.string.copy_success);
                }
            });
        }
        lDialog.findViewById(R.id.close)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        lDialog.dismiss();
                    }
                });
        lDialog.show();
    }

    public void show() {
        if (lDialog != null) {
            lDialog.show();
        }

    }

}
