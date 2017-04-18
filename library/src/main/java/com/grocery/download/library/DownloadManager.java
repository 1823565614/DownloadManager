package com.grocery.download.library;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.grocery.download.ui.DownloadActivity;

import java.util.List;

/**
 * Created by 4ndroidev on 16/10/6.
 */
public class DownloadManager {

    private final static int MAX_TASK_COUNT = 3;

    private Context context;
    private DownloadEngine engine;
    private static DownloadManager instance;
    private DownloadJobListener listener = new DownloadJobListener() {
        @Override
        public void onCreated(DownloadInfo info) {
        }

        @Override
        public void onStarted(DownloadInfo info) {
        }

        @Override
        public void onCompleted(boolean success, DownloadInfo info) {
            if (!success || info == null) return;
            FileManager fileManager = FileManager.getInstance(context);
            String extension = fileManager.getExtension(info.key);
            if (fileManager.isApk(extension)) {
//            FileManager.getInstance(this).install(info.name, info.path, true);
            } else if (fileManager.isMusic(extension)) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + info.path)));
            }
        }
    };

    private DownloadManager(Context context) {
        this.context = context;
        engine = new DownloadEngine(context, MAX_TASK_COUNT);
        engine.addDownloadJobListener(listener);
    }

    public static DownloadManager get(Context context) {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                Context applicationContext = context.getApplicationContext();
                instance = new DownloadManager(applicationContext);
            }
        }
        return instance;
    }

    private void destroy() {
        engine.removeDownloadJobListener(listener);
        engine.destroy();
        engine = null;
        instance = null;
    }

    public static void gotoDownload(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, DownloadActivity.class);
        context.startActivity(intent);
    }


    public DownloadTask.Builder download(long id, String url, String name) {
        return new DownloadTask.Builder(engine).id(id).url(url).name(name);
    }

    public void addInterceptor(Interceptor interceptor) {
        engine.addInterceptor(interceptor);
    }

    public void addDownloadJobListener(DownloadJobListener downloadJobListener) {
        engine.addDownloadJobListener(downloadJobListener);
    }

    public void removeDownloadJobListener(DownloadJobListener downloadJobListener) {
        engine.removeDownloadJobListener(downloadJobListener);
    }

    public List<DownloadTask> getAllTasks() {
        return engine.getAllTasks();
    }

    public List<DownloadInfo> getAllInfo() {
        return engine.getAllInfo();
    }

    public void delete(DownloadInfo info) {
        engine.delete(info);
    }

    public boolean isActive() {
        return engine.isActive();
    }

    public interface Interceptor {

        void updateDownloadInfo(DownloadInfo info);

    }

}
