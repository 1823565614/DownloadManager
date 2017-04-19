package com.grocery.download.library;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 4ndroidev on 16/10/6.
 */

// one-to-one association with DownloadInfo
public class DownloadJob implements Runnable {

    private boolean isPaused;
    private DownloadInfo info;
    private DownloadEngine engine;
    private List<DownloadListener> listeners;

    private Runnable changeState = new Runnable() {
        @Override
        public void run() {
            for (DownloadListener listener : listeners) {
                listener.onStateChanged(info, DownloadJob.this.info.state);
            }
            switch (info.state) {
                case DownloadState.STATE_RUNNING:
                    engine.onJobStarted(info);
                    break;
                case DownloadState.STATE_FINISHED:
                    engine.onJobCompleted(true, info);
                    clear();
                    break;
                case DownloadState.STATE_FAILED:
                case DownloadState.STATE_PAUSED:
                    engine.onJobCompleted(false, info);
                    break;
            }
        }
    };

    private Runnable changeProgress = new Runnable() {
        @Override
        public void run() {
            for (DownloadListener listener : listeners) {
                listener.onProgressChanged(info, DownloadJob.this.info.finishedLength, DownloadJob.this.info.contentLength);
            }
        }
    };

    public DownloadJob(DownloadEngine engine, DownloadInfo info) {
        this.engine = engine;
        this.info = info;
        this.listeners = new ArrayList<>();
    }

    DownloadInfo getInfo() {
        return info;
    }

    void addListener(DownloadListener listener) {
        if (listener == null || listeners.contains(listener)) return;
        listener.onStateChanged(info, info.state);
        listeners.add(listener);
    }

    void removeListener(DownloadListener listener) {
        if (listener == null || !listeners.contains(listener)) return;
        listeners.remove(listener);
    }

    boolean isRunning() {
        return DownloadState.STATE_RUNNING == info.state;
    }

    void enqueue() {
        resume();
    }

    void pause() {
        isPaused = true;
    }

    void resume() {
        if (isRunning()) return;
        onStateChanged(DownloadState.STATE_WAITING, false);
        isPaused = false;
        engine.executor.submit(this);
    }

    void remove(){
        onStateChanged(DownloadState.STATE_UNKNOWN, false);
    }

    private void clear() {
        listeners.clear();
        engine = null;
        info = null;
    }

    private void onStateChanged(int state, boolean updateDb) {
        info.state = state;
        if (updateDb) engine.provider.update(info);
        engine.handler.removeCallbacks(changeState);
        engine.handler.post(changeState);
    }

    private void onProgressChanged(long finishedLength, long contentLength) {
        info.finishedLength = finishedLength;
        info.contentLength = contentLength;
        engine.handler.removeCallbacks(changeProgress);
        engine.handler.post(changeProgress);
    }

    private boolean prepare() {
        if (isPaused) {
            onStateChanged(DownloadState.STATE_PAUSED, false);
            if (!engine.provider.exists(info)) {
                engine.provider.insert(info);
            } else {
                engine.provider.update(info);
            }
            return false;
        } else {
            onStateChanged(DownloadState.STATE_RUNNING, false);
            onProgressChanged(info.finishedLength, info.contentLength);
            if (engine.interceptors != null) {
                for (DownloadManager.Interceptor interceptor : engine.interceptors) {
                    interceptor.updateDownloadInfo(info);
                }
            }
            if (!engine.provider.exists(info)) {
                engine.provider.insert(info);
            }
            return true;
        }
    }

    @Override
    public void run() {
        if (!prepare()) return;
        long finishedLength = info.finishedLength;
        long contentLength = info.contentLength;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        try {
            connection = (HttpURLConnection) new URL(info.url).openConnection();
            connection.setAllowUserInteraction(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            if (finishedLength != 0 && contentLength > 0) {
                connection.setRequestProperty("Range", "bytes=" + finishedLength + "-" + contentLength);
            } else {
                contentLength = connection.getContentLength();
            }
            int responseCode = connection.getResponseCode();
            if (contentLength > 0 && (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL)) {
                inputStream = connection.getInputStream();
                File file = new File(info.path);
                randomAccessFile = new RandomAccessFile(file, "rw");
                randomAccessFile.seek(finishedLength);
                byte[] buffer = new byte[20480];
                int len;
                long bytesRead = finishedLength;
                while (!this.isPaused && (len = inputStream.read(buffer)) != -1) {
                    randomAccessFile.write(buffer, 0, len);
                    bytesRead += len;
                    finishedLength = bytesRead;
                    onProgressChanged(finishedLength, contentLength);
                }
                connection.disconnect();
                if (this.isPaused) {
                    onStateChanged(DownloadState.STATE_PAUSED, true);
                } else {
                    info.finishTime = System.currentTimeMillis();
                    onStateChanged(DownloadState.STATE_FINISHED, true);
                    return;
                }
            } else {
                onStateChanged(DownloadState.STATE_FAILED, true);
            }
        } catch (final Exception e) {
            onStateChanged(DownloadState.STATE_FAILED, true);
        } finally {
            try {
                if (randomAccessFile != null)
                    randomAccessFile.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
            }
            if (connection != null)
                connection.disconnect();
        }
    }
}
