package com.example.novaflix;

public class Channel {
    private String name;
    private String logoUrl;
    private String streamUrl;

    public Channel(String name, String logoUrl, String streamUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
}
