package com.fzm.walletmodule.ui.widget;

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

    private Context context;

    private Dialog lDialog;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    public InQrCodeDialogView(Context context, String address, String imgUrl, int imgId) {
        this.context = context;
        showNoticeDialogCustom(address, imgUrl, imgId);

    }

    private void showNoticeDialogCustom(final String address, final String imgUrl, int imgId) {
        lDialog = new Dialog(context,
                android.R.style.Theme_Translucent_NoTitleBar);
        lDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        lDialog.setContentView(R.layout.dialog_in_qr_code);
        lDialog.setCancelable(true);
        final ImageView imageView = lDialog.findViewById(R.id.image);
        if (!TextUtils.isEmpty(address)) {
            TextView addressTv = lDialog.findViewById(R.id.tv_address);
            addressTv.setText(HtmlUtils.change4(address));
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_app);
            Bitmap bitmap = CodeUtils.createQRCode(address, 190, logo);
            imageView.setImageBitmap(bitmap);
            if (TextUtils.isEmpty(imgUrl)) {
                if (imgId == 0) {
                    GlideUtils.intoQRBitmap(imageView, address);
                } else {
                    Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), imgId);
                    Bitmap qrBitmap = CodeUtils.createQRCode(address, 200, logoBitmap);
                    imageView.setImageBitmap(qrBitmap);
                }
            } else {
                GlideUtils.intoQRBitmap(context, imgUrl, imageView, address);

            }
            addressTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager cm = (ClipboardManager) context
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData mClipData = ClipData.newPlainText("Label", address);
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
