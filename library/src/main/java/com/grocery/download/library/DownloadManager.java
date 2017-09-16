package com.grocery.download.library;

import android.content.Context;
import android.content.Intent;

import com.grocery.download.ui.DownloadActivity;

import junit.framework.Assert;

import java.util.List;

/**
 * Created by 4ndroidev on 16/10/6.
 */
public class DownloadManager {

    private final static int MAX_TASK_COUNT = 3;

    private DownloadEngine engine;
    private static DownloadManager instance;

    private DownloadManager(Context context) {
        engine = new DownloadEngine(context, MAX_TASK_COUNT);
    }

    public void initialize() {
        engine.initialize();
    }

    public synchronized static DownloadManager get(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context.getApplicationContext());
        }
        return instance;
    }

    public void destroy() {
        Assert.assertNotNull(engine);
        engine.destroy();
        engine = null;
        instance = null;
    }

    public static void gotoDownload(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, DownloadActivity.class);
        context.startActivity(intent);
    }


    public DownloadTask.Builder newTask(long id, String url, String name) {
        Assert.assertNotNull(engine);
        return new DownloadTask.Builder(engine).id(id).url(url).name(name);
    }

    public DownloadTask createTask(DownloadInfo info, DownloadListener listener) {
        return new DownloadTask(engine, info, listener);
    }

    public void addInterceptor(Interceptor interceptor) {
        Assert.assertNotNull(engine);
        engine.addInterceptor(interceptor);
    }

    public void addDownloadJobListener(DownloadJobListener downloadJobListener) {
        Assert.assertNotNull(engine);
        engine.addDownloadJobListener(downloadJobListener);
    }

    public void removeDownloadJobListener(DownloadJobListener downloadJobListener) {
        Assert.assertNotNull(engine);
        engine.removeDownloadJobListener(downloadJobListener);
    }

    public List<DownloadTask> getAllTasks() {
        Assert.assertNotNull(engine);
        return engine.getAllTasks();
    }

    public List<DownloadInfo> getAllInfo() {
        return engine.getAllInfo();
    }

    public void delete(DownloadInfo info) {
        Assert.assertNotNull(engine);
        engine.delete(info);
    }

    public boolean isActive() {
        Assert.assertNotNull(engine);
        return engine.isActive();
    }

    interface Interceptor {
        void updateDownloadInfo(DownloadInfo info);
    }

}
