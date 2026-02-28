package com.visualcrawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CrawlerService {

    private static final Pattern HTTP_PATTERN = Pattern.compile("^https?://.*");

    /**
     * Crawls a single URL and returns UNIQUE normalized links.
     */
    public List<String> extractLinks(String url) throws IOException {
        // Using a Set here prevents duplicates on the single page immediately
        Set<String> uniqueLinks = new HashSet<>();

        try { enableSSLBypass(); } catch (Exception e) { /* Log error */ }

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .sslSocketFactory(socketFactory())
                .get();

        Elements links = doc.select("a[href]");
        links.forEach(link -> {
            String absoluteUrl = link.attr("abs:href");
            if (!absoluteUrl.isEmpty() && HTTP_PATTERN.matcher(absoluteUrl).matches()) {
                // CLEAN THE URL BEFORE ADDING
                String cleanLink = normalizeUrl(absoluteUrl);
                uniqueLinks.add(cleanLink);
            }
        });

        return new ArrayList<>(uniqueLinks);
    }

    /**
     * Smart Bulk Pagination with Global Deduplication.
     */
    public List<String> crawlPagination(String baseUrl, int start, int end) {
        if (end - start > 200) {
            throw new IllegalArgumentException("Batch size limit exceeded. Max 200 pages per batch.");
        }

        // We use a Concurrent Set to handle multiple threads adding links at the same time
        // This ensures GLOBAL uniqueness across all 100 pages
        Set<String> globalUniqueLinks = ConcurrentHashMap.newKeySet();

        IntStream.rangeClosed(start, end)
                .parallel() // Run pages in parallel
                .forEach(i -> {
                    String targetUrl = generatePagedUrl(baseUrl, i);
                    try {
                        List<String> linksOnPage = extractLinks(targetUrl);
                        // Add all found links to the global set
                        // The Set will automatically reject any link it has seen before
                        globalUniqueLinks.addAll(linksOnPage);
                    } catch (Exception e) {
                        // Ignore failed pages
                    }
                });

        // Convert back to a sorted list for the user
        return globalUniqueLinks.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Helper: Generates the correct URL for page X
     */
    private String generatePagedUrl(String baseUrl, int pageNum) {
        if (baseUrl.contains("{}")) {
            return baseUrl.replace("{}", String.valueOf(pageNum));
        } else if (baseUrl.contains("?")) {
            return baseUrl + "&page=" + pageNum;
        } else {
            return baseUrl + "?page=" + pageNum;
        }
    }

    /**
     * Helper: Removes junk from URLs to ensure "google.com/" and "google.com" are the same.
     */
    private String normalizeUrl(String url) {
        // 1. Remove Anchor tags (#section1)
        int hashIndex = url.indexOf("#");
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }

        // 2. Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    // --- SSL Helpers (Standard) ---
    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void enableSSLBypass() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }};
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
}