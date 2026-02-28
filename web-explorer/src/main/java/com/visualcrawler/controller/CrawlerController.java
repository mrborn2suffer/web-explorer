package com.visualcrawler.controller;

import com.visualcrawler.model.LinkResponse;
import com.visualcrawler.service.CrawlerService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend access
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    // ORIGINAL: Single Page Crawl
    @PostMapping("/extract")
    public LinkResponse extract(@RequestParam String url) {
        try {
            List<String> links = crawlerService.extractLinks(url);
            return new LinkResponse(url, links);
        } catch (IllegalArgumentException e) {
            return new LinkResponse(url, Collections.singletonList("Error: Invalid URL format"));
        } catch (IOException e) {
            return new LinkResponse(url, Collections.singletonList("Error: Could not connect to site (" + e.getMessage() + ")"));
        }
    }

    // NEW: Bulk Pagination Crawl
    @PostMapping("/extract-range")
    public LinkResponse extractRange(
            @RequestParam String urlPattern,
            @RequestParam int start,
            @RequestParam int end) {

        // Example: urlPattern="site.com/page/{}", start=1, end=5
        List<String> allLinks = crawlerService.crawlPagination(urlPattern, start, end);

        return new LinkResponse("Bulk Crawl: Pages " + start + "-" + end, allLinks);
    }
}