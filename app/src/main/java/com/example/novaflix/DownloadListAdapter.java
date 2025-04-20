package com.example.novaflix;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

public class DownloadListAdapter extends ArrayAdapter<DownloadItem> {
    private Context context;
    private List<DownloadItem> downloadItems;
    private DownloadManager downloadManager;

    public DownloadListAdapter(Context context, List<DownloadItem> downloadItems) {
        super(context, R.layout.download_item, downloadItems);
        this.context = context;
        this.downloadItems = downloadItems;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.download_item, parent, false);
        }

        final DownloadItem item = downloadItems.get(position);

        // Setup UI elements
        TextView title = convertView.findViewById(R.id.downloadTitle);
        TextView percentage = convertView.findViewById(R.id.downloadPercentage);
        ImageView pauseResumeButton = convertView.findViewById(R.id.pauseResumeButton);
        ImageView deleteCancelButton = convertView.findViewById(R.id.deleteCancelButton);

        // Set the movie title
        title.setText(item.getFileName());

        // Check if the download is complete
        if (item.getProgress() == 100) {
            // Hide percentage and set a "completed" icon
            percentage.setVisibility(View.GONE);
            pauseResumeButton.setImageResource(R.drawable.star); // Replace with your "completed" icon
            pauseResumeButton.setOnClickListener(null); // Disable pause/resume functionality
        } else {
            // Show percentage and set the appropriate pause/resume icon
            percentage.setVisibility(View.VISIBLE);
            percentage.setText(item.getProgress() + "%");

            if (item.isPaused()) {
                pauseResumeButton.setImageResource(R.drawable.ic_play);
            } else {
                pauseResumeButton.setImageResource(R.drawable.ic_pause);
            }

            pauseResumeButton.setOnClickListener(v -> {
                if (item.isPaused()) {
                    resumeDownload(item);
                    item.setPaused(false);
                    pauseResumeButton.setImageResource(R.drawable.ic_pause);
                } else {
                    pauseDownload(item);
                    item.setPaused(true);
                    pauseResumeButton.setImageResource(R.drawable.ic_play);
                }
            });
        }

        // Handle movie title click
        title.setOnClickListener(v -> {
            if (item.getProgress() == 100) {
                playDownloadedMovieOrInstallApk(item.getDownloadId());
            } else {
                Toast.makeText(context, "Download is not complete!", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel/delete button functionality
        deleteCancelButton.setOnClickListener(v -> {
            // Cancel or delete the download
            downloadManager.remove(item.getDownloadId()); // Cancel the download
            downloadItems.remove(position); // Remove from list
            notifyDataSetChanged(); // Update the adapter
        });



    // Pause/Resume functionality
        if (item.isPaused()) {
            pauseResumeButton.setImageResource(R.drawable.ic_play);
        } else {
            pauseResumeButton.setImageResource(R.drawable.ic_pause);
        }

        pauseResumeButton.setOnClickListener(v -> {
            if (item.isPaused()) {
                resumeDownload(item);
                item.setPaused(false);
                pauseResumeButton.setImageResource(R.drawable.ic_pause);
            } else {
                pauseDownload(item);
                item.setPaused(true);
                pauseResumeButton.setImageResource(R.drawable.ic_play);
            }
        });


        return convertView;
    }

    private void pauseDownload(DownloadItem item) {
        // Logic to pause the download
        // DownloadManager does not support direct pausing, but you can cancel and restart the download
        downloadManager.remove(item.getDownloadId());
    }

    private void resumeDownload(DownloadItem item) {
        if (TextUtils.isEmpty(item.getFileUrl()) || !item.getFileUrl().startsWith("http")) {
            Toast.makeText(context, "Invalid file URL. Cannot resume download.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new download request with the same fileUrl
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.getFileUrl()));
        request.setTitle(item.getFileName());
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, item.getFileName() + ".mp4");
        long newDownloadId = downloadManager.enqueue(request);

        // Update item with new download ID
        item.setDownloadId(newDownloadId);
    }
    private void playDownloadedMovieOrInstallApk(long downloadId) {
        // Get the file's local URI from DownloadManager
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            cursor.close();

            if (uriString != null) {
                Uri fileUri = Uri.parse(uriString);

                // For Android N and above, use FileProvider to share the file URI securely
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", new File(fileUri.getPath()));
                }

                String fileName = fileUri.getPath();
                if (fileName != null) {
                    if (fileName.endsWith(".apk")) {
                        // Handle APK file
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permission
                        context.startActivity(intent);
                    } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".avi")) {
                        // Handle video files, using your custom player
                        Intent intent = new Intent(context, VideoPlayerActivity.class);
                        intent.setData(fileUri); // Pass the video URI to your custom player
                        intent.putExtra("video_uri", fileUri.toString()); // Optionally pass the URI as a String
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Unsupported file type.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Error: File not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Error: Unable to retrieve file.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Error: Unable to retrieve file.", Toast.LENGTH_SHORT).show();
        }
    }



}
