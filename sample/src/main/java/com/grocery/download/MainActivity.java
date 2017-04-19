package com.grocery.download;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.grocery.download.library.DownloadInfo;
import com.grocery.download.library.DownloadListener;
import com.grocery.download.library.DownloadManager;
import com.grocery.download.library.DownloadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.grocery.download.library.DownloadState.STATE_FAILED;
import static com.grocery.download.library.DownloadState.STATE_FINISHED;
import static com.grocery.download.library.DownloadState.STATE_PAUSED;
import static com.grocery.download.library.DownloadState.STATE_RUNNING;
import static com.grocery.download.library.DownloadState.STATE_UNKNOWN;
import static com.grocery.download.library.DownloadState.STATE_WAITING;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<DownloadTask> tasks;
    private DownloadManager controller;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void initialize() {
        controller = DownloadManager.get(this);
        tasks = new ArrayList<>();
        String data = AssetsHelper.readAsString(this, "data.json");
        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0, length = array.length(); i < length; i++) {
                JSONObject item = array.getJSONObject(i);
                String name = item.optString("name");
                String icon = item.optString("icon");
                String url = item.optString("url");
                tasks.add(controller.download(i, url, name).extras(icon).create());
            }
        } catch (JSONException e) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_download) {
            DownloadManager.gotoDownload(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {

        private LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

        @Override
        public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = inflater.inflate(R.layout.list_item, parent, false);
            return new DownloadViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(DownloadViewHolder holder, int position) {
            DownloadTask task = tasks.get(position);
            holder.setKey(task.getKey());
            task.setListener(holder);
            holder.title.setText(task.getName());
            if (task.getSize() == 0) {
                holder.size.setText(R.string.download_unknown);
            } else {
                holder.size.setText(String.format("%.1fMB", task.getSize() / 1048576.0f));
            }
            Glide.with(MainActivity.this).load(task.getExtras()).into(holder.icon);
        }

        @Override
        public int getItemCount() {
            return tasks == null ? 0 : tasks.size();
        }
    }

    private class DownloadViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, DownloadListener {

        int state;
        String key;
        ImageView icon;
        TextView title;
        TextView size;
        Button download;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            size = (TextView) itemView.findViewById(R.id.size);
            download = (Button) itemView.findViewById(R.id.download);
            download.setOnClickListener(this);
        }

        void setKey(String key) {
            this.key = key;
        }

        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            final DownloadTask task = tasks.get(position);
            switch (state) {
                case STATE_FAILED:
                case STATE_UNKNOWN:
                    task.start();
                    break;
                case STATE_PAUSED:
                    task.resume();
                    break;
                case STATE_WAITING:
                case STATE_RUNNING:
                    task.pause();
                    break;
            }
        }

        @Override
        public void onStateChanged(DownloadInfo info, int state) {
            if (!info.key.equals(this.key)) return;
            this.state = state;
            switch (state) {
                case STATE_FAILED:
                    download.setText(R.string.download_retry);
                    break;
                case STATE_UNKNOWN:
                    download.setText(R.string.label_download);
                    break;
                case STATE_PAUSED:
                    download.setText(R.string.download_resume);
                    break;
                case STATE_WAITING:
                    download.setText(R.string.download_wait);
                    break;
                case STATE_FINISHED:
                    if (tasks == null) return;
                    int position = getAdapterPosition();
                    DownloadTask task = tasks.get(position);
                    task.clear();
                    break;
            }
        }

        @Override
        public void onProgressChanged(DownloadInfo info, long finishedLength, long contentLength) {
            if (!info.key.equals(this.key)) return;
            download.setText(String.format("%.1f%%", finishedLength * 100.f / Math.max(contentLength, 1)));
            if (contentLength == 0) {
                size.setText(R.string.download_unknown);
            } else {
                size.setText(String.format("%.1fMB", contentLength / 1048576.0f));
            }
        }
    }
}
