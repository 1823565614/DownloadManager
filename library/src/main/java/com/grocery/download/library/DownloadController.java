package com.grocery.download.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.grocery.download.ui.DownloadActivity;

import java.util.Collections;
import java.util.List;

/**
 * Created by 4ndroidev on 16/10/6.
 */
public class DownloadController {

    private Context context;
    private boolean isConnected;
    private static DownloadController instance;
    private DownloadService.DownloadBinder binder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof DownloadService.DownloadBinder) {
                DownloadController.this.binder = (DownloadService.DownloadBinder) service;
                isConnected = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            DownloadController.this.binder = null;
            isConnected = false;
        }
    };

    private DownloadController(Context context) {
        this.context = context;
    }

    public static DownloadController get(Context context) {
        if (instance == null) {
            synchronized (DownloadController.class) {
                Context applicationContext = context.getApplicationContext();
                instance = new DownloadController(applicationContext);
            }
        }
        return instance;
    }

    public static void gotoDownload(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, DownloadActivity.class);
        context.startActivity(intent);
    }

    public void connect() {
        Intent intent = new Intent();
        intent.setClass(this.context, DownloadService.class);
        this.context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void disConnect() {
        this.context.unbindService(connection);
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public DownloadTask.Builder download(long id, String url, String name) {
        if (binder == null) return null;
        return binder.download(id, url, name);
    }

    public void addInterceptor(Interceptor interceptor) {
        if (binder == null) return;
        binder.addInterceptor(interceptor);
    }

    public void addDownloadJobListener(DownloadJobListener downloadJobListener) {
        if (binder == null) return;
        binder.addDownloadJobListener(downloadJobListener);
    }

    public void removeDownloadJobListener(DownloadJobListener downloadJobListener) {
        if (binder == null) return;
        binder.removeDownloadJobListener(downloadJobListener);
    }

    public List<DownloadTask> getAllTasks() {
        if (binder == null) return Collections.EMPTY_LIST;
        return binder.getAllTasks();
    }

    public List<DownloadInfo> getAllInfo() {
        if (binder == null) return Collections.EMPTY_LIST;
        return binder.getAllInfo();
    }

    public void delete(DownloadInfo info) {
        if (binder == null) return;
        binder.delete(info);
    }

    public boolean isActive() {
        if (binder == null) return false;
        return binder.isActive();
    }

    public interface Interceptor {

        void updateDownloadInfo(DownloadInfo info);

    }

}
