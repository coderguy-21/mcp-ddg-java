package com.xtivia.mcp.websearch.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Preferred site configuration for query enhancement.
 */
public record PreferredSite(
    /**
     * Site URL (e.g., "stackoverflow.com")
     */
    @JsonProperty("url")
    String url,
    
    /**
     * Keywords that trigger this site preference
     */
    @JsonProperty("keywords")
    List<String> keywords
) {
}
