package com.xtivia.mcp.websearch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

/**
 * Request manager to help prevent rate limiting.
 * Implements intelligent request spacing and header rotation.
 */
@Service
public class RequestManager {
    
    private static final long MIN_DELAY_MS = 2000; // 2 seconds minimum between requests
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    private static final List<String> USER_AGENTS = List.of(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    );
    
    private final Random random = new Random();
    
    private long lastRequestTime = 0;
    private int requestCount = 0;
    private final List<Long> requestTimes = new ArrayList<>();
    
    /**
     * Wait for appropriate delay before making request
     */
    public synchronized void waitForRequest() {
        long now = System.currentTimeMillis();
        
        // Clean old request times (older than 1 minute)
        long oneMinuteAgo = now - 60000;
        requestTimes.removeIf(time -> time < oneMinuteAgo);
        
        // Check if we're exceeding rate limits
        if (requestTimes.size() >= MAX_REQUESTS_PER_MINUTE) {
            long oldestRequest = requestTimes.get(0);
            long waitTime = oneMinuteAgo - oldestRequest + 1000;
            sleep(waitTime);
        }
        
        // Ensure minimum delay between requests
        long timeSinceLastRequest = now - lastRequestTime;
        if (timeSinceLastRequest < MIN_DELAY_MS) {
            long delayNeeded = MIN_DELAY_MS - timeSinceLastRequest;
            sleep(delayNeeded);
        }
        
        // Record this request
        lastRequestTime = System.currentTimeMillis();
        requestTimes.add(lastRequestTime);
        requestCount++;
    }
    
    /**
     * Add random jitter to make requests appear more human-like
     */
    public void addRandomDelay() {
        // Add 0-3 second random delay
        long randomDelay = random.nextInt(3000);
        sleep(randomDelay);
    }
    
    /**
     * Get a rotated user agent to vary requests
     */
    public String getUserAgent() {
        int index = requestCount % USER_AGENTS.size();
        return USER_AGENTS.get(index);
    }
    
    /**
     * Get additional headers to make requests more human-like
     */
    public java.util.Map<String, String> getRandomHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        
        // Randomly include some optional headers
        if (random.nextDouble() > 0.5) {
            headers.put("DNT", "1");
        }
        
        if (random.nextDouble() > 0.7) {
            headers.put("Cache-Control", "max-age=0");
        }
        
        return headers;
    }
    
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
