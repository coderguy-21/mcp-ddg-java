package com.xtivia.mcp.websearch.provider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.xtivia.mcp.websearch.config.WebSearchProperties;
import com.xtivia.mcp.websearch.model.SearchResult;
import com.xtivia.mcp.websearch.service.RequestManager;

/**
 * Brave Search provider implementation.
 * Used as fallback when DuckDuckGo is rate limited.
 */
@Component
public class BraveProvider implements SearchProvider {
    
    private static final Logger log = LoggerFactory.getLogger(BraveProvider.class);
    private static final String BASE_URL = "https://search.brave.com/search";
    
    private final WebClient.Builder webClientBuilder;
    private final RequestManager requestManager;
    private final WebSearchProperties properties;
    
    public BraveProvider(WebClient.Builder webClientBuilder,
                        RequestManager requestManager,
                        WebSearchProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.requestManager = requestManager;
        this.properties = properties;
    }
    
    @Override
    public String getName() {
        return "Brave";
    }
    
    @Override
    public List<SearchResult> search(String query, int maxResults, String dateFilter) {
        try {
            // Format query for Brave (handle site: operators)
            String searchQuery = formatQueryForBrave(query);
            String url = buildSearchUrl(searchQuery);
            
            // Wait for rate limiting
            requestManager.waitForRequest();
            requestManager.addRandomDelay();
            
            // Execute search
            String html = webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("User-Agent", requestManager.getUserAgent());
                    headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                    headers.set("Accept-Language", "en-US,en;q=0.5");
                    // DON'T request compressed response - Spring WebClient has issues with gzip decompression on Windows
                    // headers.set("Accept-Encoding", "gzip, deflate, br");
                    headers.set("DNT", "1");
                    headers.set("Connection", "keep-alive");
                    headers.set("Upgrade-Insecure-Requests", "1");
                })
                .retrieve()
                .bodyToMono(String.class)
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("Empty response from Brave"));
            
            List<SearchResult> results = parseResults(html, maxResults);
            
            if (properties.isDebug()) {
                log.debug("Brave search returned {} results for query: {}", results.size(), query);
            }
            
            return results;
            
        } catch (Exception e) {
            if (properties.isDebug()) {
                log.error("Brave search failed for query \"{}\": {}", query, e.getMessage());
            }
            throw new RuntimeException("Brave search failed: " + e.getMessage(), e);
        }
    }
    
    private String formatQueryForBrave(String query) {
        // If the query has site: operators, format them properly for Brave
        if (query.contains("site:") || query.contains("OR site:")) {
            // Keep the enhanced query but clean up the format for Brave
            return query.replace("(site:", "site:").replace(")", "");
        }
        return query;
    }
    
    private String buildSearchUrl(String query) {
        try {
            return BASE_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build search URL", e);
        }
    }
    
    private List<SearchResult> parseResults(String html, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            if (properties.isDebug()) {
                log.debug("Parsing Brave results from {} character response", html.length());
            }
            
            Document doc = Jsoup.parse(html);
            
            // Brave search results can be in different selectors
            String[] selectors = {".snippet", ".web-result", ".result", "#results .fdb", "div[data-type='web']"};
            boolean foundResults = false;
            
            for (String selector : selectors) {
                if (foundResults && !results.isEmpty()) {
                    break;
                }
                
                Elements resultElements = doc.select(selector);
                
                if (properties.isDebug()) {
                    log.debug("Selector '{}' found {} elements", selector, resultElements.size());
                }
                
                for (Element element : resultElements) {
                    if (results.size() >= maxResults) {
                        break;
                    }
                    
                    // Extract title and URL with multiple fallback selectors for Brave
                    Element titleElement = element.selectFirst("h3 a, .title a, .result-title a, a[href]");
                    if (titleElement == null) continue;
                    
                    String title = titleElement.text().trim();
                    String url = titleElement.attr("href");
                    
                    // Clean up relative URLs if needed
                    if (url.startsWith("/")) {
                        url = "https://search.brave.com" + url;
                    }
                    
                    // Basic validation
                    if (title.isEmpty() || url.isEmpty() || url.length() < 10) {
                        continue;
                    }
                    
                    // Extract snippet for summary with Brave-specific selectors
                    String snippet = "";
                    Element snippetElement = element.selectFirst(".snippet-description, .description, .snippet-content");
                    if (snippetElement != null) {
                        snippet = snippetElement.text().trim();
                    } else {
                        Element pElement = element.selectFirst("p");
                        if (pElement != null) {
                            snippet = pElement.text().trim();
                        }
                    }
                    
                    // Generate keywords and summary
                    List<String> keywords = extractKeywords(title, snippet);
                    String summary = createSummary(title, snippet, url);
                    
                    if (properties.isDebug()) {
                        log.debug("Adding Brave result: {}...", title.substring(0, Math.min(50, title.length())));
                    }
                    
                    results.add(new SearchResult(title, url, keywords, summary));
                    
                    foundResults = true;
                }
            }
            
            if (properties.isDebug()) {
                log.debug("Extracted {} results from Brave", results.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to parse Brave results", e);
        }
        
        return results;
    }
    
    private List<String> extractKeywords(String title, String snippet) {
        String text = (title + " " + snippet).toLowerCase();
        String[] words = text.split("\\s+");
        
        Set<String> stopWords = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", 
            "had", "her", "was", "one", "our", "out", "day", "get", "has",
            "him", "his", "how", "man", "new", "now", "old", "see", "two",
            "way", "who", "boy", "did", "its", "let", "put", "say", "she",
            "too", "use"
        );
        
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            word = word.replaceAll("[^a-z]", "");
            if (word.length() >= 3 && !stopWords.contains(word)) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        
        return wordCount.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private String createSummary(String title, String snippet, String url) {
        if (snippet == null || snippet.isEmpty()) {
            return "Content from " + extractDomain(url) + ": " + title;
        }
        
        String summary = snippet.replaceAll("\\s+", " ").trim();
        
        if (summary.length() < 50) {
            summary = title + ": " + summary;
        }
        
        if (summary.length() > 200) {
            String[] sentences = summary.split("[.!?]+");
            StringBuilder truncated = new StringBuilder(sentences[0]);
            
            for (int i = 1; i < sentences.length; i++) {
                if ((truncated.length() + sentences[i].length()) <= 180) {
                    truncated.append(sentences[i]).append(".");
                } else {
                    break;
                }
            }
            
            summary = truncated.length() > 50 ? truncated.toString() : summary.substring(0, 180) + "...";
        }
        
        if (!summary.matches(".*[.!?]$")) {
            summary += summary.length() < 180 ? "." : "...";
        }
        
        return summary;
    }
    
    private String extractDomain(String url) {
        try {
            return java.net.URI.create(url).toURL().getHost().replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return "unknown source";
        }
    }
}
