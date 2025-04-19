package com.example.novaflix;

public class Movies {
    private String title;
    private String logoUrl; // URL for the movie thumbnail/logo
    private int playCount; // Number of times the movie has been played
    private String downloadUrl; // URL for downloading the movie

    // Constructor to initialize movie details
    public Movies(String title, int playCount, String downloadUrl, String logoUrl) {
        this.title = title;
        this.playCount = playCount;
        this.downloadUrl = downloadUrl;
        this.logoUrl = logoUrl;
    }

    // Getter for the movie title
    public String getTitle() {
        return title;
    }

    // Getter for the logo URL
    public String getLogoUrl() {
        return logoUrl;
    }

    // Getter for the play count
    public int getPlayCount() {
        return playCount;
    }

    // Getter for the download URL
    public String getDownloadUrl() {
        return downloadUrl;
    }

    // Optional setters if you need to modify any of these attributes later
    public void setTitle(String title) {
        this.title = title;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
