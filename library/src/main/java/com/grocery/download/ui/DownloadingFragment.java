package com.grocery.download.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.grocery.download.library.DownloadManager;
import com.grocery.download.library.DownloadInfo;
import com.grocery.download.library.DownloadTask;
import com.grocery.library.R;
import com.grocery.download.library.DownloadJobListener;
import com.grocery.download.library.DownloadListener;
import com.grocery.download.library.FileManager;

import java.util.List;

import static com.grocery.download.library.DownloadState.STATE_FAILED;
import static com.grocery.download.library.DownloadState.STATE_FINISHED;
import static com.grocery.download.library.DownloadState.STATE_PAUSED;
import static com.grocery.download.library.DownloadState.STATE_RUNNING;
import static com.grocery.download.library.DownloadState.STATE_UNKNOWN;
import static com.grocery.download.library.DownloadState.STATE_WAITING;

/**
 * Created by 4ndroidev on 16/10/17.
 */
public class DownloadingFragment extends Fragment implements View.OnClickListener {

    private View hint;
    private View header;
    private View contentView;
    private TextView operationText;
    private ImageView operationIcon;
    private DownloadAdapter adapter;
    private RecyclerView recyclerView;
    private List<DownloadTask> tasks;
    private DownloadManager controller;
    private FileManager fileManager;

    private DownloadJobListener jobListener = new DownloadJobListener() {
        @Override
        public void onCreated(DownloadInfo info) {
            tasks.add(0, controller.download(info.id, info.url, info.name).source(info.source).extras(info.extras).create());
            adapter.notifyItemInserted(0);
        }

        @Override
        public void onStarted(DownloadInfo info) {
            updateUI();
        }

        @Override
        public void onCompleted(boolean success, DownloadInfo info) {
            updateUI();
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        fileManager = FileManager.getInstance(context);
        controller = DownloadManager.get(context);
        controller.addDownloadJobListener(jobListener);
        tasks = controller.getAllTasks();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (contentView != null) return contentView;
        Context context = getContext();
        contentView = inflater.inflate(R.layout.fragment_downloading, container, false);
        hint = contentView.findViewById(R.id.download_hint);
        header = contentView.findViewById(R.id.download_header);
        operationText = (TextView) contentView.findViewById(R.id.download_operation_text);
        operationIcon = (ImageView) contentView.findViewById(R.id.download_operation_icon);
        recyclerView = (RecyclerView) contentView.findViewById(R.id.download_recycler_view);
        contentView.findViewById(R.id.download_operation).setOnClickListener(this);
        contentView.findViewById(R.id.download_clear).setOnClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(context, layoutManager.getOrientation());
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadAdapter();
        recyclerView.setAdapter(adapter);
        updateUI();
        return contentView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        controller.removeDownloadJobListener(jobListener);
        for (DownloadTask task : tasks) {
            task.setListener(null);
        }
        tasks.clear();
        tasks = null;
    }

    private void updateUI() {
        boolean hasTask = tasks.size() > 0;
        if (hasTask) {
            if (hint.getVisibility() == View.VISIBLE) hint.setVisibility(View.GONE);
            if (header.getVisibility() != View.VISIBLE) header.setVisibility(View.VISIBLE);
            if (recyclerView.getVisibility() != View.VISIBLE)
                recyclerView.setVisibility(View.VISIBLE);
            boolean isActive = controller.isActive();
            if (isActive) {
                operationIcon.setImageResource(R.drawable.list_icon_pause);
                operationText.setText(R.string.download_pause_all);
            } else {
                operationIcon.setImageResource(R.drawable.list_icon_download);
                operationText.setText(R.string.download_start_all);
            }
        } else {
            if (hint.getVisibility() != View.VISIBLE) hint.setVisibility(View.VISIBLE);
            if (header.getVisibility() == View.VISIBLE) header.setVisibility(View.GONE);
            if (recyclerView.getVisibility() == View.VISIBLE)
                recyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.download_operation == id) {
            if (controller.isActive()) {
                for (DownloadTask task : tasks) {
                    task.pause();
                }
            } else {
                for (DownloadTask task : tasks) {
                    task.resume();
                }
            }
        } else if (R.id.download_clear == id) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.download_clear_task_waring)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (DownloadTask task : tasks) {
                                task.remove();
                            }
                            tasks.clear();
                            adapter.notifyDataSetChanged();
                            updateUI();
                            dialog.dismiss();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create().show();
        }
    }

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {

        private LayoutInflater inflater = LayoutInflater.from(getContext());

        @Override
        public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = inflater.inflate(R.layout.layout_downloading_list_item, parent, false);
            return new DownloadViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(DownloadViewHolder holder, int position) {
            DownloadTask task = tasks.get(position);
            holder.setKey(task.getKey());
            task.setListener(holder);
            holder.name.setText(task.getName());
            if (task.getSize() == 0) {
                holder.size.setText(R.string.download_unknown);
            } else {
                holder.size.setText(String.format("%.1fMB", task.getSize() / 1048576.0f));
            }
            String extension = fileManager.getExtension(task.getName());
            if (fileManager.isApk(extension)) {
                holder.icon.setImageResource(R.drawable.format_apk);
            } else if (fileManager.isMusic(extension)) {
                holder.icon.setImageResource(R.drawable.format_music);
            } else if (fileManager.isVideo(extension)) {
                holder.icon.setImageResource(R.drawable.format_vedio);
            } else if (fileManager.isZip(extension) || fileManager.isRar(extension)) {
                holder.icon.setImageResource(R.drawable.format_zip);
            } else {
                holder.icon.setImageResource(R.drawable.format_unknown);
            }
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
        TextView name;
        TextView size;
        TextView status;
        long refreshProgressTime;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.download_icon);
            name = (TextView) itemView.findViewById(R.id.download_name);
            size = (TextView) itemView.findViewById(R.id.download_size);
            status = (TextView) itemView.findViewById(R.id.download_status);
            itemView.findViewById(R.id.download_close).setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        void setKey(String key) {
            this.key = key;
        }

        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            final DownloadTask task = tasks.get(position);
            if (v == itemView) {
                switch (state) {
                    case STATE_FAILED:
                        task.start();
                        break;
                    case STATE_PAUSED:
                    case STATE_UNKNOWN:
                        task.resume();
                        break;
                    case STATE_WAITING:
                    case STATE_RUNNING:
                        task.pause();
                        break;
                }
            } else if (R.id.download_close == v.getId()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.download_remove_task_waring)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (tasks.contains(task)) {
                                    task.remove();
                                    tasks.remove(task);
                                    adapter.notifyItemRemoved(position);
                                    updateUI();
                                }
                                dialog.dismiss();
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
            }
        }

        @Override
        public void onStateChanged(DownloadInfo info, int state) {
            if (!info.key.equals(this.key)) return;
            this.state = state;
            switch (state) {
                case STATE_FAILED:
                    status.setText(R.string.download_retry);
                    break;
                case STATE_PAUSED:
                case STATE_UNKNOWN:
                    status.setText(R.string.download_resume);
                    break;
                case STATE_WAITING:
                    status.setText(R.string.download_wait);
                    break;
                case STATE_FINISHED:
                    if (tasks == null) return;
                    int position = getAdapterPosition();
                    DownloadTask task = tasks.get(position);
                    task.clear();
                    tasks.remove(task);
                    adapter.notifyItemRemoved(position);
                    updateUI();
                    break;
            }
        }

        @Override
        public void onProgressChanged(DownloadInfo info, long finishedLength, long contentLength) {
            if (!info.key.equals(this.key)) return;
            long now = System.currentTimeMillis();
            if (now - refreshProgressTime < 500) return;
            refreshProgressTime = now;
            status.setText(String.format("%.1f%%", finishedLength * 100.f / Math.max(contentLength, 1)));
            if (contentLength == 0) {
                size.setText(R.string.download_unknown);
            } else {
                size.setText(String.format("%.1fMB", contentLength / 1048576.0f));
            }
        }
    }
}
