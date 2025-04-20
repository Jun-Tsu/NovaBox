package com.example.novaflix;

import android.net.Uri;

public class DownloadItem {
    private String fileName;
    private int progress;
    private boolean isPaused;
    private long downloadId;  // This will track the download's unique ID
    private String fileUrl;
    private Uri fileUri;

    // Updated constructor to include downloadId
    public DownloadItem(String fileName, int progress, String fileUrl, long downloadId) {
        this.fileName = fileName;
        this.progress = progress;
        this.fileUrl = fileUrl;
        this.isPaused = false; // Initially not paused
        this.downloadId = downloadId;
        this.fileUri = fileUri;
        // Assign downloadId
    }

    // Getters and setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public String getFileUrl() {
        return fileUrl;

    }


}
