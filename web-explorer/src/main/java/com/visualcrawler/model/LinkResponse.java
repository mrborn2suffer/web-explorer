package com.visualcrawler.model;

import java.util.List;

public class LinkResponse {
    private String originalUrl;
    private int count;
    private List<String> links;

    public LinkResponse(String originalUrl, List<String> links) {
        this.originalUrl = originalUrl;
        this.links = links;
        this.count = links.size();
    }

    public String getOriginalUrl() { return originalUrl; }
    public int getCount() { return count; }
    public List<String> getLinks() { return links; }
}
