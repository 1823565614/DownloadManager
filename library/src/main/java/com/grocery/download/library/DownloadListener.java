package com.grocery.download.library;

/**
 * Created by 4ndroidev on 16/10/6.
 */

// one-to-one association with DownloadTask
public interface DownloadListener {

    void onStateChanged(DownloadInfo info, int state);

    void onProgressChanged(DownloadInfo info, long finishedLength, long contentLength);

}
