package com.xtivia.mcp.websearch.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtivia.mcp.websearch.config.WebSearchProperties;
import com.xtivia.mcp.websearch.model.PreferredSite;

import jakarta.annotation.PostConstruct;

/**
 * Manages preferred sites for query enhancement.
 * Automatically adds site: operators to relevant queries.
 */
@Service
public class PreferredSitesManager {
    
    private static final Logger log = LoggerFactory.getLogger(PreferredSitesManager.class);
    
    private final WebSearchProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    private List<PreferredSite> sites = new ArrayList<>();
    private boolean loaded = false;
    
    public PreferredSitesManager(WebSearchProperties properties,
                                 ResourceLoader resourceLoader,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() {
        loadSites();
    }
    
    /**
     * Enhance query with preferred sites if keywords match
     */
    public String enhanceQuery(String query) {
        ensureLoaded();
        
        if (sites.isEmpty()) {
            return query;
        }
        
        String queryLower = query.toLowerCase();
        
        // Find matching sites (up to 4)
        List<String> matchingSites = sites.stream()
            .filter(site -> site.keywords().stream()
                .anyMatch(keyword -> queryLower.contains(keyword.toLowerCase())))
            .limit(4)
            .map(PreferredSite::url)
            .toList();
        
        if (matchingSites.isEmpty()) {
            return query;
        }
        
        // Build site operators
        String siteOperators = String.join(" OR ", 
            matchingSites.stream()
                .map(site -> "site:" + site)
                .toList());
        
        String enhanced = query + " (" + siteOperators + ")";
        
        if (properties.isDebug()) {
            log.debug("Using preferred sites for \"{}\": {}", query, String.join(", ", matchingSites));
            log.debug("Enhanced query: \"{}\" -> \"{}\"", query, enhanced);
        }
        
        return enhanced;
    }
    
    private void ensureLoaded() {
        if (!loaded) {
            loadSites();
        }
    }
    
    private void loadSites() {
        try {
            String location = properties.getPreferredSitesFile();
            Resource resource = resourceLoader.getResource(location);
            
            if (!resource.exists()) {
                if (properties.isDebug()) {
                    log.debug("Preferred sites file not found: {}", location);
                }
                loaded = true;
                return;
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                sites = objectMapper.readValue(inputStream, new TypeReference<List<PreferredSite>>() {});
                
                if (properties.isDebug()) {
                    log.debug("Loaded {} preferred sites", sites.size());
                }
            }
            
        } catch (IOException e) {
            if (properties.isDebug()) {
                log.warn("Failed to load preferred sites: {}", e.getMessage());
            }
            sites = new ArrayList<>();
        } finally {
            loaded = true;
        }
    }
}
