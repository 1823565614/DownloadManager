package com.grocery.download.ui;

import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.grocery.library.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 4ndroidev on 16/10/8.
 */
public class DownloadActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private DownloadedFragment downloadedFragment;
    private DownloadingFragment downloadingFragment;

    private List<PageInfo> pages = new ArrayList<>();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SwipeBackLayout.attachTo(this);
        setContentView(R.layout.activity_download);
        bindView();
    }

    public void bindView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.title_download);
        downloadingFragment = new DownloadingFragment();
        downloadedFragment = new DownloadedFragment();
        Resources resources = getResources();
        pages.add(new PageInfo(resources.getString(R.string.download_title_downloading), downloadingFragment));
        pages.add(new PageInfo(resources.getString(R.string.download_title_downloaded), downloadedFragment));
        viewPager = (ViewPager) findViewById(R.id.download_viewpager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return pages.get(position).fragment;
            }

            @Override
            public int getCount() {
                return pages.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return pages.get(position).title;
            }
        });
        tabLayout = (TabLayout) findViewById(R.id.download_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = pages.get(viewPager.getCurrentItem()).fragment;
        if (fragment instanceof BackEventHandler && ((BackEventHandler) fragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    private class PageInfo {
        String title;
        Fragment fragment;

        public PageInfo(String title, Fragment fragment) {
            this.title = title;
            this.fragment = fragment;
        }
    }
}
