package com.xtivia.mcp.websearch.model;

import java.util.List;

/**
 * Response object for search operations.
 */
public record SearchResponse(
    /**
     * The original query (or enhanced version)
     */
    String query,
    
    /**
     * Total number of results returned
     */
    int totalResults,
    
    /**
     * Search provider used (DuckDuckGo, Brave, or Brave (DDG suspended))
     */
    String searchProvider,
    
    /**
     * List of search results
     */
    List<SearchResult> results
) {
}
