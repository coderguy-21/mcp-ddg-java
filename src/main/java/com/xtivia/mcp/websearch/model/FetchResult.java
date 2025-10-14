package com.xtivia.mcp.websearch.model;

import java.util.List;

/**
 * Result from fetching URL content.
 */
public record FetchResult(
    /**
     * URL that was fetched
     */
    String url,
    
    /**
     * Page title
     */
    String title,
    
    /**
     * Extracted content
     */
    String content,
    
    /**
     * Generated summary
     */
    String summary,
    
    /**
     * Extracted keywords
     */
    List<String> keywords,
    
    /**
     * Metadata about the fetch operation
     */
    Metadata metadata
) {
    
    public record Metadata(
        String domain,
        String contentType,
        Integer contentLength,
        String lastModified
    ) {}
}
