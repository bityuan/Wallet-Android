package com.fzm.walletmodule.update;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;


import com.afollestad.materialdialogs.MaterialDialog;
import com.fzm.wallet.sdk.bean.AppVersion;
import com.fzm.walletmodule.utils.AppUtils;
import com.fzm.walletmodule.utils.NetWorkUtils;
import com.fzm.walletmodule.utils.PreferencesUtils;

import java.io.File;
import java.lang.ref.WeakReference;


public class UpdateUtils {

    private AppVersion mData;
    private FragmentManager mFragmentManager;
    private Context mContext;
    public static final String DOWNLOAD_ID = "download_id";
    private DownloadChangeObserver downloadObserver;
    private static long lastDownloadId = 0;
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");
    private MaterialDialog materialDialog;

    WeakReference<AppCompatActivity> mWeakReference;

    public UpdateUtils(Context context) {
        mContext = context;
    }

    public void update(AppVersion data, FragmentManager fragmentManager, FragmentActivity activity, boolean isMain) {
        mWeakReference = new WeakReference(activity);
        this.mData = data;
        this.mFragmentManager = fragmentManager;
        if (mData == null) {
            Toast.makeText(mContext, "服务异常", Toast.LENGTH_SHORT).show();
        }
        if (AppUtils.getVersionCode(mContext) == mData.getVersion_code()) {
            if (!isMain) {
                Toast.makeText(mContext, "当前已是最新版本", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        //服务器的code必须和服务器的apk的code保持一致，并且必须大于本地的code，不然不可安装
        if (mData.getVersion_code() > AppUtils.getVersionCode(mContext)) {
            updating();
        }
    }

    //4代表强制更新，1代表普通更新
    public void updating() {
        final UpdateDialogFragment fragment = new UpdateDialogFragment();
        fragment.setResult("版本更新" + mData.getVersion())
                .setResultDetails(mData.getLog())
                .setOnButtonClickListener(new UpdateDialogFragment.OnButtonClickListener() {
                    @Override
                    public void onLeftButtonClick(View v) {
                    }

                    @Override
                    public void onRightButtonClick(View v) {
                        updateVersion();
                    }
                });
        if (mData.getStatus() == 4) {
            //强制更新
            fragment.setCancelable(false);
            fragment.setType(1);
        }
        fragment.showDialog("检查更新", mFragmentManager);
    }

    /**
     * 从一个apk文件去获取该文件的版本信息
     *
     * @param context         本应用程序上下文
     * @param archiveFilePath APK文件的路径。如：/sdcard/download/XX.apk
     * @return
     */
    public static PackageInfo getVersionNameFromApk(Context context, String archiveFilePath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo packInfo = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        return packInfo;
    }


    private void updateVersion() {
        File targetApkFile = UpdataBroadcastReceiver.queryDownloadedApk(mContext);
        // 文件已存在
        if (targetApkFile != null && targetApkFile.exists()) {
            //还要判断下这个本地的file和当前版本是否一样的，如果一样的，不可更新
            PackageInfo packInfo = getVersionNameFromApk(mContext, targetApkFile.getPath());
            int versionCode = AppUtils.getVersionCode(mContext);
            int fileCode = packInfo.versionCode;

            if (fileCode <= versionCode) {
                initDownNewAPK();
            } else {
                AppUtils.install(targetApkFile, mContext);
            }

        } else {
            initDownNewAPK();
        }
    }

    private void initDownNewAPK() {
        if (NetWorkUtils.isConnected(mContext)) {
            downloadNewVersion();
        } else {
            Toast.makeText(mContext, "请检查网络设置", Toast.LENGTH_SHORT).show();
        }
    }


    private void downloadNewVersion() {
        if (queryDownloadingApk(mContext)) {
            return;
        }
        FragmentActivity activity = mWeakReference.get();
        if (activity != null) {
            if (materialDialog == null) {
                materialDialog = new MaterialDialog.Builder(activity)
                        .title("版本升级")
                        .content("正在下载安装包，请稍候")
                        .progress(false, 100, false)
                        .cancelable(false)
                        .show();

            }
        }


        DownloadManager dowanloadmanager = (DownloadManager) mContext.getSystemService(mContext.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mData.getDownload_url()));
        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, "pwallet" + mData.getVersion() + ".apk");
        request.setTitle("账户" + mData.getVersion());
        request.setDescription(mData.getLog());
        request.setMimeType("application/vnd.android.package-archive");
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.allowScanningByMediaScanner();
        request.setVisibleInDownloadsUi(true);
        lastDownloadId = dowanloadmanager.enqueue(request);
        PreferencesUtils.putLong(mContext, DOWNLOAD_ID, lastDownloadId);
        downloadObserver = new DownloadChangeObserver(null);
        mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);

        //取消下载
        // downManager.remove(id);
    }

    class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(lastDownloadId);
            DownloadManager dManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            final Cursor cursor = dManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                final int totalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                final int currentColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int totalSize = cursor.getInt(totalColumn);
                int currentSize = cursor.getInt(currentColumn);
                float percent = (float) currentSize / (float) totalSize;
                int progress = Math.round(percent * 100);
                materialDialog.setProgress(progress);
                if (progress == 100) {
                    materialDialog.dismiss();
                }
            }

        }
    }

    public boolean queryDownloadingApk(Context context) {
        if (lastDownloadId == -1) {
            return false;
        }
        boolean result = false;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(lastDownloadId);
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING);
        Cursor cur = downloader.query(query);
        if (cur != null) {
            if (cur.moveToFirst()) {
                result = true;
            }
            cur.close();
        }
        return result;
    }

    public void unregisterContentObserver() {
        if (downloadObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(downloadObserver);
        }
    }

}
