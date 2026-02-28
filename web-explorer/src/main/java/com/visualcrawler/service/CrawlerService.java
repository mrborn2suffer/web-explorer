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

    public List<String> extractLinks(String url) throws IOException {
        Set<String> uniqueLinks = new HashSet<>();

        try { enableSSLBypass(); } catch (Exception e) {  }

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
                String cleanLink = normalizeUrl(absoluteUrl);
                uniqueLinks.add(cleanLink);
            }
        });

        return new ArrayList<>(uniqueLinks);
    }

    public List<String> crawlPagination(String baseUrl, int start, int end) {
        if (end - start > 200) {
            throw new IllegalArgumentException("Batch size limit exceeded. Max 200 pages per batch.");
        }

        Set<String> globalUniqueLinks = ConcurrentHashMap.newKeySet();

        IntStream.rangeClosed(start, end)
                .parallel() 
                .forEach(i -> {
                    String targetUrl = generatePagedUrl(baseUrl, i);
                    try {
                        List<String> linksOnPage = extractLinks(targetUrl);
                       
                        globalUniqueLinks.addAll(linksOnPage);
                    } catch (Exception e) {
                        
                    }
                });

        
        return globalUniqueLinks.stream().sorted().collect(Collectors.toList());
    }

    private String generatePagedUrl(String baseUrl, int pageNum) {
        if (baseUrl.contains("{}")) {
            return baseUrl.replace("{}", String.valueOf(pageNum));
        } else if (baseUrl.contains("?")) {
            return baseUrl + "&page=" + pageNum;
        } else {
            return baseUrl + "?page=" + pageNum;
        }
    }

    
    private String normalizeUrl(String url) {
        
        int hashIndex = url.indexOf("#");
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }

        
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

  
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
