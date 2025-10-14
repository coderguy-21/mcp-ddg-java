package com.xtivia.mcp.websearch.model;

import java.util.List;

/**
 * Represents a single search result.
 */
public record SearchResult(
    /**
     * Title of the search result
     */
    String title,
    
    /**
     * URL of the search result
     */
    String url,
    
    /**
     * Extracted keywords from the result
     */
    List<String> keywords,
    
    /**
     * Intelligent summary of the result
     */
    String summary
) {
}
