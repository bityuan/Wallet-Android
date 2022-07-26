package com.fzm.walletmodule.update;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.fzm.walletmodule.utils.AppUtils;
import com.fzm.walletmodule.utils.PreferencesUtils;

import java.io.File;


public class UpdataBroadcastReceiver extends BroadcastReceiver {

    @SuppressLint("NewApi")
    public void onReceive(Context context, Intent intent) {
        long downLoadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        long cacheDownLoadId = PreferencesUtils.getLong(context, UpdateUtils.DOWNLOAD_ID);
        if (cacheDownLoadId == downLoadId) {
            File apkFile = queryDownloadedApk(context);
            if (apkFile != null) {
                AppUtils.install(apkFile, context);
            }
        }
    }

    //通过downLoadId查询下载的apk，解决6.0以后安装的问题
    public static File queryDownloadedApk(Context context) {
        File targetApkFile = null;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = PreferencesUtils.getLong(context, UpdateUtils.DOWNLOAD_ID);
        if (downloadId != -1) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
            Cursor cur = downloader.query(query);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (!TextUtils.isEmpty(uriString)) {
                        targetApkFile = new File(Uri.parse(uriString).getPath());
                    }
                }
                cur.close();
            }
        }
        return targetApkFile;
    }

    public static File queryDownloadedApk(Context context, String downloadIdKey) {
        File targetApkFile = null;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = PreferencesUtils.getLong(context, downloadIdKey);
        if (downloadId != -1) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
            Cursor cur = downloader.query(query);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (!TextUtils.isEmpty(uriString)) {
                        targetApkFile = new File(Uri.parse(uriString).getPath());
                    }


                    //获取状态
                    int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    switch (status) {
                        case DownloadManager.STATUS_PENDING:
                            Log.v("tag", "status_pending");
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            Log.v("tag", "status_paused");
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            Log.v("tag", "status_running");
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            Log.v("tag", "status_successful");
                            //AppUtils.install(targetApkFile, context);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            Log.v("tag", "status_failed");
                            break;
                    }
                }
                cur.close();
            }
        }
        return targetApkFile;
    }
}