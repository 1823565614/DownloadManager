package com.grocery.download;

import android.app.Application;

import com.grocery.download.library.DownloadManager;

/**
 * Created by 4ndroidev on 17/4/20.
 */

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DownloadManager.get(this).initialize();
    }
}
