package com.xtivia.mcp.websearch.tool;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtivia.mcp.websearch.config.WebSearchProperties;
import com.xtivia.mcp.websearch.model.SearchResponse;
import com.xtivia.mcp.websearch.model.SearchResult;
import com.xtivia.mcp.websearch.provider.BraveProvider;
import com.xtivia.mcp.websearch.provider.DuckDuckGoProvider;
import com.xtivia.mcp.websearch.service.PreferredSitesManager;

/**
 * MCP tool for web search with DuckDuckGo and Brave fallback.
 */
@Component
public class WebSearchTool implements Function<WebSearchTool.SearchRequest, String> {
    
    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    
    private final DuckDuckGoProvider duckDuckGoProvider;
    private final BraveProvider braveProvider;
    private final PreferredSitesManager preferredSitesManager;
    private final WebSearchProperties properties;
    private final ObjectMapper objectMapper;
    
    // Suspension tracking for DuckDuckGo
    private static long duckDuckGoSuspendedUntil = 0;
    private static int suspensionCount = 0;
    
    public WebSearchTool(DuckDuckGoProvider duckDuckGoProvider,
                        BraveProvider braveProvider,
                        PreferredSitesManager preferredSitesManager,
                        WebSearchProperties properties,
                        ObjectMapper objectMapper) {
        this.duckDuckGoProvider = duckDuckGoProvider;
        this.braveProvider = braveProvider;
        this.preferredSitesManager = preferredSitesManager;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String apply(SearchRequest request) {
        try {
            String query = request.query();
            int maxResults = request.maxResults() != null ? request.maxResults() : properties.getSearchResultsCount();
            String dateFilter = request.dateFilter();
            
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Query parameter is required and cannot be empty");
            }
            
            if (maxResults > 50) {
                throw new IllegalArgumentException("Maximum results cannot exceed 50");
            }
            
            // Enhance query with preferred sites
            String enhancedQuery = preferredSitesManager.enhanceQuery(query);
            
            // Check if DuckDuckGo is currently suspended
            long now = System.currentTimeMillis();
            boolean isDuckDuckGoSuspended = now < duckDuckGoSuspendedUntil;
            
            SearchResponse searchResponse;
            
            // Try DuckDuckGo first (unless suspended)
            if (!isDuckDuckGoSuspended) {
                try {
                    List<SearchResult> results = duckDuckGoProvider.search(enhancedQuery, maxResults, dateFilter);
                    
                    // Check if DuckDuckGo results seem valid (not rate limited)
                    if (results.isEmpty() && isLikelyRateLimited(enhancedQuery)) {
                        suspendDuckDuckGo();
                        throw new RuntimeException("DuckDuckGo rate limited");
                    }
                    
                    String displayQuery = enhancedQuery.equals(query) ? 
                        query : query + " (enhanced: " + enhancedQuery + ")";
                    
                    searchResponse = new SearchResponse(
                        displayQuery,
                        results.size(),
                        "DuckDuckGo",
                        results
                    );
                    
                    // Reset suspension count on successful DuckDuckGo search
                    if (suspensionCount > 0) {
                        suspensionCount = 0;
                    }
                    
                } catch (Exception ddgError) {
                    // Suspend DuckDuckGo and fall back to Brave
                    suspendDuckDuckGo();
                    List<SearchResult> results = braveProvider.search(enhancedQuery, maxResults, null);
                    
                    String displayQuery = enhancedQuery.equals(query) ? 
                        query : query + " (enhanced: " + enhancedQuery + ")";
                    
                    searchResponse = new SearchResponse(
                        displayQuery,
                        results.size(),
                        "Brave",
                        results
                    );
                }
            } else {
                // DuckDuckGo is suspended, go straight to Brave
                List<SearchResult> results = braveProvider.search(enhancedQuery, maxResults, null);
                
                String displayQuery = enhancedQuery.equals(query) ? 
                    query : query + " (enhanced: " + enhancedQuery + ")";
                
                searchResponse = new SearchResponse(
                    displayQuery,
                    results.size(),
                    "Brave (DDG suspended)",
                    results
                );
            }
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(searchResponse);
            
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return "{\"error\": \"Search failed on all providers: " + e.getMessage() + "\"}";
        }
    }
    
    private boolean isLikelyRateLimited(String query) {
        // Don't suspend DuckDuckGo just because it returned 0 results
        // Only suspend if we get an actual exception indicating rate limiting
        // Let the catch block handle real failures
        return false;
    }
    
    private void suspendDuckDuckGo() {
        long now = System.currentTimeMillis();
        suspensionCount++;
        
        // Exponential backoff: 20 minutes, 40 minutes, 80 minutes, max 120 minutes
        int backoffMultiplier = Math.min((int) Math.pow(2, suspensionCount - 1), 
            properties.getDuckduckgo().getMaxSuspensionMultiplier());
        long suspensionDuration = properties.getDuckduckgo().getSuspensionDurationMinutes() * 
            60000L * backoffMultiplier;
        
        duckDuckGoSuspendedUntil = now + suspensionDuration;
    }
    
    /**
     * Search request record
     */
    public record SearchRequest(
        String query,
        Integer maxResults,
        String dateFilter
    ) {}
}
