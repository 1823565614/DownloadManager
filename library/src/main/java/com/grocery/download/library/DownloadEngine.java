package com.grocery.download.library;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.grocery.download.ui.DownloadActivity;
import com.grocery.library.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 4ndroidev on 16/10/6.
 */
public class DownloadEngine {

    public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "Download";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int KEEP_ALIVE = 10;
    private static final int NOTIFY_ID = 10000;
    private static final int REQUEST_CODE = 100;

    /**
     * observes job lifecycle: onJobCreated, onJobStarted, onJobCompleted
     */
    private List<DownloadJobListener> downloadJobListeners;

    private NotificationManager notificationManager;

    /**
     * record all jobs those are not completed
     */
    private Map<String, DownloadJob> jobs;

    /**
     * record all download info
     */
    private Map<String, DownloadInfo> infos;

    /**
     * record all active jobs in order for notification, some jobs are created, but not running
     */
    private List<DownloadJob> activeJobs;

    private ThreadPoolExecutor singleExecutor;

    /**
     * update notification
     */
    private Runnable updateNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            int activeCount = activeJobs.size();
            if (activeCount == 0) {
                notificationManager.cancel(NOTIFY_ID);
            } else {
                Notification.Builder builder = new Notification.Builder(context);
                Intent intent = new Intent();
                intent.setClass(context, DownloadActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent downloadIntent = PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                Resources resources = context.getResources();
                String title = resources.getString(R.string.download_notification_title);
                String summary = resources.getString(R.string.download_notification_summary, activeCount);
                builder.setSmallIcon(R.drawable.notify_download)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setContentTitle(title)
                        .setContentText(summary)
                        .setWhen(System.currentTimeMillis())
                        .setContentIntent(downloadIntent)
                        .setOngoing(true);
                Notification.InboxStyle style = new Notification.InboxStyle();
                style.setBigContentTitle(title);
                style.setSummaryText(summary);
                for (int i = 0; i < activeCount; i++) {
                    DownloadJob job = activeJobs.get(i);
                    if (job.isRunning()) {
                        style.addLine(job.getInfo().name);
                    }
                }
                builder.setStyle(style);
                notificationManager.notify(NOTIFY_ID, builder.build());
            }
        }
    };

    /**
     * for some server, the url of resource if temporary
     * maybe need setting interceptor to update the url
     */
    List<DownloadManager.Interceptor> interceptors;

    /**
     * download ThreadPoolExecutor
     */
    ThreadPoolExecutor executor;

    /**
     * provider for inserting, deleting, querying or updating the download info with the database
     */
    DownloadProvider provider;
    Handler handler;
    Context context;

    DownloadEngine(Context context, int maxTask) {
        this.context = context.getApplicationContext();
        jobs = new HashMap<>();
        infos = new HashMap<>();
        activeJobs = new ArrayList<>();
        interceptors = new ArrayList<>();
        downloadJobListeners = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
        if (maxTask > CORE_POOL_SIZE) maxTask = CORE_POOL_SIZE;
        executor = new ThreadPoolExecutor(maxTask, maxTask, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue());
        executor.allowCoreThreadTimeOut(true);
        singleExecutor = new ThreadPoolExecutor(1, 1, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue());
        singleExecutor.allowCoreThreadTimeOut(true);
        notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        provider = new DownloadProvider(this.context);
    }

    /**
     * load download info from the database
     */
    void initialize() {
        singleExecutor.submit(new Runnable() {
            @Override
            public void run() {
                List<DownloadInfo> list = provider.query();
                for (DownloadInfo info : list) {
                    infos.put(info.key, info);
                    if (info.isFinished()) continue;
                    DownloadJob job = new DownloadJob(DownloadEngine.this, info);
                    jobs.put(info.key, job);
                }
            }
        });
    }

    /**
     * clear and clear
     */
    void destroy() {
        singleExecutor.shutdown();
        executor.shutdown();
        interceptors.clear();
        downloadJobListeners.clear();
    }

    /**
     * @return all tasks those are not completed
     */
    List<DownloadTask> getAllTasks() {
        List<DownloadTask> tasks = new ArrayList<>();
        Iterator<DownloadJob> iterator = jobs.values().iterator();
        while (iterator.hasNext()) {
            tasks.add(new DownloadTask(this, iterator.next().getInfo(), null));
        }
        Collections.sort(tasks);
        return tasks;
    }

    /**
     * @return all download info in order
     */
    List<DownloadInfo> getAllInfo() {
        List<DownloadInfo> result = new ArrayList<>(infos.values());
        Collections.sort(result);
        return result;
    }

    boolean isActive() {
        return activeJobs.size() > 0;
    }

    /**
     * @param interceptor which implements method updateDownloadInfo(DownloadInfo info)
     */
    void addInterceptor(DownloadManager.Interceptor interceptor) {
        if (interceptor == null || interceptors.contains(interceptor)) return;
        interceptors.add(interceptor);
    }

    /**
     * add downloadJobListener to observe the job lifecycle
     *
     * @param downloadJobListener which implements onJobCreated, onJobStarted, onJobCompleted
     */
    void addDownloadJobListener(DownloadJobListener downloadJobListener) {
        if (downloadJobListener == null || downloadJobListeners.contains(downloadJobListener))
            return;
        downloadJobListeners.add(downloadJobListener);
    }

    /**
     * delete downloadJobListener that observing the job lifecycle
     *
     * @param downloadJobListener which implements onJobCreated, onJobStarted, onJobCompleted
     */
    void removeDownloadJobListener(DownloadJobListener downloadJobListener) {
        if (downloadJobListener == null || !downloadJobListeners.contains(downloadJobListener))
            return;
        downloadJobListeners.remove(downloadJobListener);
    }

    /**
     * prepare for the task, while creating a task, should callback the download info to the listener
     *
     * @param task
     */
    void prepare(DownloadTask task) {
        String key = task.key;
        if (!infos.containsKey(key)) {  // do not contain this info, means that it will create a download job
            if (task.listener == null) return;
            task.listener.onStateChanged(key, DownloadState.STATE_UNKNOWN);
            return;
        }
        DownloadInfo info = infos.get(key);
        task.size = info.contentLength;
        task.createTime = info.createTime;
        if (!jobs.containsKey(key)) {  // uncompleted jobs do not contain this job, means the job had completed
            if (task.listener == null) return;
            task.listener.onStateChanged(key, info.state); // info.state == DownloadState.STATE_FINISHED
        } else {
            jobs.get(key).addListener(task.listener);
        }
    }

    /**
     * if downloadJobs contains the relative job, and the job is not running, enqueue it
     * otherwise create the job and enqueue it
     *
     * @param task
     */
    void enqueue(DownloadTask task) {
        String key = task.key;
        if (jobs.containsKey(key)) {                   // has existed uncompleted job
            DownloadJob job = jobs.get(key);
            if (job.isRunning()) return;
            job.enqueue();
            activeJobs.add(job);
        } else {
            if (infos.containsKey(key)) return;         // means the job had completed
            DownloadInfo info = task.generateInfo();
            DownloadJob job = new DownloadJob(this, info);
            infos.put(key, info);
            jobs.put(key, job);
            onJobCreated(info);
            job.addListener(task.listener);
            job.enqueue();
            activeJobs.add(job);
        }
        updateNotification();
    }

    /**
     * delete the downloadJob and delete the relative info
     *
     * @param task
     */
    void delete(DownloadTask task) {
        String key = task.key;
        if (!jobs.containsKey(key)) return;
        DownloadJob job = jobs.remove(task.key);
        delete(job.getInfo());
        if (!activeJobs.contains(job)) return;
        activeJobs.remove(job);
        updateNotification();
    }

    /**
     * pause the downloadJob
     *
     * @param task
     */
    void pause(DownloadTask task) {
        String key = task.key;
        if (!jobs.containsKey(key)) return;
        jobs.get(key).pause();
    }

    /**
     * resume the downloadJob if it has not been running
     *
     * @param task
     */
    void resume(DownloadTask task) {
        String key = task.key;
        if (!jobs.containsKey(key)) return;
        DownloadJob job = jobs.get(key);
        if (job.isRunning()) return;
        job.resume();
        activeJobs.add(job);
        updateNotification();
    }

    /**
     * delete download info, delete file
     *
     * @param info
     */
    void delete(final DownloadInfo info) {
        if (info == null || !infos.containsValue(info)) return;
        infos.remove(info.key);
        if (isMainThread()) {
            singleExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    provider.delete(info);
                    File file = new File(info.path);
                    if (file.exists()) file.delete();
                }
            });
        } else {
            provider.delete(info);
            File file = new File(info.path);
            if (file.exists()) file.delete();
        }
    }

    /**
     * add download listener
     *
     * @param task
     */
    void addListener(DownloadTask task) {
        String key = task.key;
        if (!infos.containsKey(key)) {
            if (task.listener == null) return;
            task.listener.onStateChanged(key, DownloadState.STATE_UNKNOWN);
        } else {
            if (!jobs.containsKey(key)) {
                if (task.listener == null) return;
                task.listener.onStateChanged(key, DownloadState.STATE_FINISHED);
            } else {
                jobs.get(key).addListener(task.listener);
            }
        }
    }

    /**
     * delete download listener
     *
     * @param task
     */
    void removeListener(DownloadTask task) {
        String key = task.key;
        if (!jobs.containsKey(key)) return;
        jobs.get(key).removeListener(task.listener);
    }

    /**
     * notify the downloadJob has been create
     *
     * @param info
     */
    void onJobCreated(DownloadInfo info) {
        for (DownloadJobListener downloadJobListener : downloadJobListeners) {
            downloadJobListener.onCreated(info);
        }
    }

    /**
     * notify the downloadJob has been started
     *
     * @param info
     */
    void onJobStarted(DownloadInfo info) {
        updateNotification();
        for (DownloadJobListener downloadJobListener : downloadJobListeners) {
            downloadJobListener.onStarted(info);
        }
    }

    /**
     * notify the downloadJob has been completed
     *
     * @param success true if the job finished
     * @param info
     */
    void onJobCompleted(boolean success, DownloadInfo info) {
        String key = info.key;
        DownloadJob job = jobs.get(key);
        activeJobs.remove(job);
        updateNotification();
        if (success) {
            jobs.remove(key);
        }
        for (DownloadJobListener downloadJobListener : downloadJobListeners) {
            downloadJobListener.onCompleted(success, info);
        }
    }

    /**
     * update the notification avoid too fast
     */
    void updateNotification() {
        handler.removeCallbacks(updateNotificationRunnable);
        handler.postDelayed(updateNotificationRunnable, 100);
    }

    /**
     * @return whether is in main thread
     */
    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
