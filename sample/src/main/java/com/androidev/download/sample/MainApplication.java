package com.androidev.download.sample;

import android.app.Application;

import com.androidev.download.DefaultNotifier;
import com.androidev.download.DownloadManager;

/**
 * Created by 4ndroidev on 17/4/20.
 */

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DownloadManager.getInstance().initialize(this, 3);
        DownloadManager.getInstance().setDownloadNotifier(new DefaultNotifier(this));
    }
}
