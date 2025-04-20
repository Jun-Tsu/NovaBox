package com.example.novaflix;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {

    private ListView downloadListView;  // Or RecyclerView if you prefer
    private DownloadListAdapter adapter;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_download, container, false);

        // Initialize ListView
        downloadListView = view.findViewById(R.id.downloadListView);

        // Fetch the completed downloads
        List<DownloadItem> completedDownloads = getCompletedDownloads();

        // Set up the adapter with the list of downloaded items
        adapter = new DownloadListAdapter(getActivity(), completedDownloads);
        downloadListView.setAdapter(adapter);

        return view;
    }

    // Function to fetch completed downloads
    private List<DownloadItem> getCompletedDownloads() {
        List<DownloadItem> completedDownloads = new ArrayList<>();
        DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);

        if (manager != null) {
            // Query for completed downloads
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL); // Only completed downloads
            Cursor cursor = manager.query(query);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    int downloadIdIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);

                    String title = cursor.getString(titleIndex);
                    String uri = cursor.getString(uriIndex);
                    long downloadId = cursor.getLong(downloadIdIndex);

                    if (title != null && uri != null) {
                        // Assuming progress is 100% for completed downloads
                        int progress = 100;  // Default value since download is complete
                        String fileUrl = uri;  // Using the URI as the file URL
                        Uri fileUri = Uri.parse(uri);  // Convert string URI to Uri object

                        // Create a new DownloadItem with all required parameters
                        DownloadItem item = new DownloadItem(title, progress, fileUrl, downloadId);
                        completedDownloads.add(item);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        return completedDownloads;
    }
}
