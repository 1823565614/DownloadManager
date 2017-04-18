package com.grocery.download.library;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import java.util.List;

/**
 * Created by 4ndroidev on 16/10/7.
 */
public class DownloadService extends Service {

    private DownloadEngine engine;

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
            FileManager fileManager = FileManager.getInstance(DownloadService.this);
            String extension = fileManager.getExtension(info.key);
            if (fileManager.isApk(extension)) {
//            FileManager.getInstance(this).install(info.path);
            } else if (fileManager.isMusic(extension)) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + info.path)));
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        engine = new DownloadEngine(this, 3);
        engine.addDownloadJobListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        engine.removeDownloadJobListener(listener);
        engine.destroy();
    }

    class DownloadBinder extends Binder {
        DownloadTask.Builder download(long id, String url, String name) {
            return new DownloadTask.Builder(engine).id(id).url(url).name(name);
        }

        void addInterceptor(DownloadController.Interceptor interceptor) {
            engine.addInterceptor(interceptor);
        }

        void addDownloadJobListener(DownloadJobListener downloadJobListener) {
            engine.addDownloadJobListener(downloadJobListener);
        }

        void removeDownloadJobListener(DownloadJobListener downloadJobListener) {
            engine.removeDownloadJobListener(downloadJobListener);
        }

        List<DownloadTask> getAllTasks() {
            return engine.getAllTasks();
        }

        List<DownloadInfo> getAllInfo() {
            return engine.getAllInfo();
        }

        void delete(DownloadInfo info) {
            engine.delete(info);
        }

        boolean isActive() {
            return engine.isActive();
        }

    }

}
