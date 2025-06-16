package com.cse441.tluprojectexpo.model;

public class LinkItem {
    private String url;
    private String platform;

    public LinkItem() { // Default constructor for Firestore if you plan to deserialize
    }

    public LinkItem(String url, String platform) {
        this.url = url;
        this.platform = platform;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}