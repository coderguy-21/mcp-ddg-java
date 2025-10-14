package com.xtivia.mcp.websearch.provider;

import java.util.List;

import com.xtivia.mcp.websearch.model.SearchResult;

/**
 * Interface for search providers (DuckDuckGo, Brave, etc.)
 */
public interface SearchProvider {
    
    /**
     * Get the name of this search provider
     */
    String getName();
    
    /**
     * Perform a search with the given query
     * 
     * @param query The search query
     * @param maxResults Maximum number of results to return
     * @param dateFilter Optional date filter (d, w, m, y)
     * @return List of search results
     */
    List<SearchResult> search(String query, int maxResults, String dateFilter);
}
