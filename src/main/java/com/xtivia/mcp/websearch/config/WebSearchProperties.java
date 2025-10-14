package com.xtivia.mcp.websearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for web search functionality.
 */
@ConfigurationProperties(prefix = "websearch")
public class WebSearchProperties {
    
    /**
     * Primary search provider: duckduckgo or brave
     */
    private String provider = "duckduckgo";
    
    /**
     * Maximum number of concurrent search requests
     */
    private int maxConcurrentRequests = 4;
    
    /**
     * Rate limit (requests per window)
     */
    private int rateLimit = 10;
    
    /**
     * Rate limit window in seconds
     */
    private int rateLimitWindowSeconds = 60;
    
    /**
     * Number of search results to return
     */
    private int searchResultsCount = 10;
    
    /**
     * Maximum length of each search result snippet
     */
    private int searchResultMaxLength = 400;
    
    /**
     * Maximum length of fetched content
     */
    private int fetchResultMaxLength = 5000;
    
    /**
     * Enable debug mode for detailed logging
     */
    private boolean debug = false;
    
    /**
     * Path to preferred sites configuration file
     */
    private String preferredSitesFile = "classpath:preferred_sites.json";
    
    /**
     * DuckDuckGo-specific settings
     */
    private DuckDuckGoProperties duckduckgo = new DuckDuckGoProperties();

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit;
    }

    public int getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) {
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    public int getSearchResultsCount() {
        return searchResultsCount;
    }

    public void setSearchResultsCount(int searchResultsCount) {
        this.searchResultsCount = searchResultsCount;
    }

    public int getSearchResultMaxLength() {
        return searchResultMaxLength;
    }

    public void setSearchResultMaxLength(int searchResultMaxLength) {
        this.searchResultMaxLength = searchResultMaxLength;
    }

    public int getFetchResultMaxLength() {
        return fetchResultMaxLength;
    }

    public void setFetchResultMaxLength(int fetchResultMaxLength) {
        this.fetchResultMaxLength = fetchResultMaxLength;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getPreferredSitesFile() {
        return preferredSitesFile;
    }

    public void setPreferredSitesFile(String preferredSitesFile) {
        this.preferredSitesFile = preferredSitesFile;
    }

    public DuckDuckGoProperties getDuckduckgo() {
        return duckduckgo;
    }

    public void setDuckduckgo(DuckDuckGoProperties duckduckgo) {
        this.duckduckgo = duckduckgo;
    }
    
    public static class DuckDuckGoProperties {
        /**
         * Duration in minutes to suspend DuckDuckGo when rate limited
         */
        private int suspensionDurationMinutes = 20;
        
        /**
         * Maximum multiplier for exponential backoff
         */
        private int maxSuspensionMultiplier = 6;

        public int getSuspensionDurationMinutes() {
            return suspensionDurationMinutes;
        }

        public void setSuspensionDurationMinutes(int suspensionDurationMinutes) {
            this.suspensionDurationMinutes = suspensionDurationMinutes;
        }

        public int getMaxSuspensionMultiplier() {
            return maxSuspensionMultiplier;
        }

        public void setMaxSuspensionMultiplier(int maxSuspensionMultiplier) {
            this.maxSuspensionMultiplier = maxSuspensionMultiplier;
        }
    }
}
