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
 * DuckDuckGo search provider implementation.
 * Uses HTML search endpoint with intelligent rate limiting.
 */
@Component
public class DuckDuckGoProvider implements SearchProvider {
    
    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoProvider.class);
    private static final String BASE_URL = "https://html.duckduckgo.com/html/";
    
    private final WebClient.Builder webClientBuilder;
    private final RequestManager requestManager;
    private final WebSearchProperties properties;
    
    public DuckDuckGoProvider(WebClient.Builder webClientBuilder, 
                              RequestManager requestManager,
                              WebSearchProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.requestManager = requestManager;
        this.properties = properties;
    }
    
    @Override
    public String getName() {
        return "DuckDuckGo";
    }
    
    @Override
    public List<SearchResult> search(String query, int maxResults, String dateFilter) {
        try {
            String url = buildSearchUrl(query, dateFilter);
            
            // Wait for rate limiting
            requestManager.waitForRequest();
            requestManager.addRandomDelay();
            
            // Execute search - request uncompressed response
            String html = webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("User-Agent", requestManager.getUserAgent());
                    headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    headers.set("Accept-Language", "en-US,en;q=0.9");
                    headers.set("DNT", "1");
                    headers.set("Connection", "keep-alive");
                    headers.set("Upgrade-Insecure-Requests", "1");
                    headers.set("Referer", "https://duckduckgo.com/");
                    requestManager.getRandomHeaders().forEach(headers::set);
                    // CRITICAL: Set AFTER random headers to override the Accept-Encoding they add
                    // DuckDuckGo uses Brotli compression which Java can't decompress
                    headers.set("Accept-Encoding", "identity");
                })
                .retrieve()
                .bodyToMono(String.class)
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("Empty response from DuckDuckGo"));
            
            List<SearchResult> results = parseResults(html, maxResults);
            
            return results;
            
        } catch (Exception e) {
            if (properties.isDebug()) {
                log.error("DuckDuckGo search failed for query \"{}\": {}", query, e.getMessage());
            }
            throw new RuntimeException("DuckDuckGo search failed: " + e.getMessage(), e);
        }
    }
    
    private String buildSearchUrl(String query, String dateFilter) {
        try {
            StringBuilder url = new StringBuilder(BASE_URL);
            url.append("?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            url.append("&b="); // Start from beginning
            
            if (dateFilter != null && !dateFilter.isEmpty()) {
                url.append("&df=").append(dateFilter);
            }
            
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build search URL", e);
        }
    }
    
    private List<SearchResult> parseResults(String html, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(html);
            Elements resultElements = doc.select(".result");
            
            for (Element element : resultElements) {
                if (results.size() >= maxResults) {
                    break;
                }
                
                // Extract title and URL
                Element titleElement = element.selectFirst(".result__title a");
                if (titleElement == null) continue;
                
                String title = titleElement.text().trim();
                String url = titleElement.attr("href");
                
                if (title.isEmpty() || url.isEmpty()) {
                    continue;
                }
                
                // Extract snippet
                Element snippetElement = element.selectFirst(".result__snippet");
                String snippet = snippetElement != null ? snippetElement.text().trim() : "";
                
                // Generate keywords and summary
                List<String> keywords = extractKeywords(title, snippet);
                String summary = createSummary(title, snippet, url);
                
                results.add(new SearchResult(title, url, keywords, summary));
            }
            
        } catch (Exception e) {
            log.error("Failed to parse DuckDuckGo results", e);
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
