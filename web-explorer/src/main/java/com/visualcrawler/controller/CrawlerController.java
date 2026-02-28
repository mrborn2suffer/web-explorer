package com.visualcrawler.controller;

import com.visualcrawler.model.LinkResponse;
import com.visualcrawler.service.CrawlerService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") 
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    
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

   
    @PostMapping("/extract-range")
    public LinkResponse extractRange(
            @RequestParam String urlPattern,
            @RequestParam int start,
            @RequestParam int end) {

        
        List<String> allLinks = crawlerService.crawlPagination(urlPattern, start, end);

        return new LinkResponse("Bulk Crawl: Pages " + start + "-" + end, allLinks);
    }
}
