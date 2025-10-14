package com.xtivia.mcp.websearch.tool;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtivia.mcp.websearch.config.WebSearchProperties;
import com.xtivia.mcp.websearch.model.FetchResult;
import com.xtivia.mcp.websearch.service.ContentFetchService;

/**
 * MCP tool for fetching and extracting web page content.
 */
@Component
public class ContentFetchTool implements Function<ContentFetchTool.FetchRequest, String> {
    
    private static final Logger log = LoggerFactory.getLogger(ContentFetchTool.class);
    
    private final ContentFetchService contentFetchService;
    private final WebSearchProperties properties;
    private final ObjectMapper objectMapper;
    
    public ContentFetchTool(ContentFetchService contentFetchService,
                           WebSearchProperties properties,
                           ObjectMapper objectMapper) {
        this.contentFetchService = contentFetchService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String apply(FetchRequest request) {
        try {
            String url = request.url();
            
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("Valid URL parameter is required");
            }
            
            if (properties.isDebug()) {
                log.debug("Fetching content from: {}", url);
            }
            
            FetchResult result = contentFetchService.fetchContent(url);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("Fetch failed: {}", e.getMessage(), e);
            return "{\"error\": \"Fetch failed: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Fetch request record
     */
    public record FetchRequest(String url) {}
}
