package com.xtivia.mcp.websearch.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.xtivia.mcp.websearch.config.WebSearchProperties;
import com.xtivia.mcp.websearch.model.FetchResult;

/**
 * Service for fetching and parsing web page content.
 */
@Service
public class ContentFetchService {
    
    private static final Logger log = LoggerFactory.getLogger(ContentFetchService.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    
    private final WebClient.Builder webClientBuilder;
    private final WebSearchProperties properties;
    
    public ContentFetchService(WebClient.Builder webClientBuilder,
                              WebSearchProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
    }
    
    /**
     * Fetch content from a URL
     */
    public FetchResult fetchContent(String url) {
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException("Valid URL parameter is required");
        }
        
        try {
            if (properties.isDebug()) {
                log.debug("Fetching content from: {}", url);
            }
            
            String html = webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("User-Agent", USER_AGENT);
                    headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    headers.set("Accept-Language", "en-US,en;q=0.5");
                    headers.set("Accept-Encoding", "gzip, deflate");
                })
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .block();
            
            if (html == null || html.isEmpty()) {
                throw new RuntimeException("Empty response from URL");
            }
            
            return parseHtmlContent(html, url);
            
        } catch (Exception e) {
            if (properties.isDebug()) {
                log.error("Fetch failed for URL \"{}\": {}", url, e.getMessage());
            }
            throw new RuntimeException("Fetch failed: " + e.getMessage(), e);
        }
    }
    
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String scheme = uri.getScheme();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }
    
    private FetchResult parseHtmlContent(String html, String url) {
        Document doc = Jsoup.parse(html);
        
        // Extract title
        String title = extractTitle(doc);
        
        // Extract main content
        String content = extractMainContent(doc);
        
        // Generate keywords
        List<String> keywords = extractKeywords(title, content);
        
        // Create intelligent summary
        String summary = createIntelligentSummary(title, content, url);
        
        // Build metadata
        FetchResult.Metadata metadata = new FetchResult.Metadata(
            extractDomain(url),
            "text/html",
            html.length(),
            null
        );
        
        return new FetchResult(
            url,
            title,
            content,
            summary,
            keywords,
            metadata
        );
    }
    
    private String extractTitle(Document doc) {
        // Try multiple sources for title
        String[] selectors = {
            "title",
            "meta[property=og:title]",
            "meta[name=twitter:title]",
            "h1"
        };
        
        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String text = selector.startsWith("meta") ? 
                    element.attr("content") : element.text();
                if (!text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        
        return "Untitled Document";
    }
    
    private String extractMainContent(Document doc) {
        // Remove unwanted elements
        doc.select("script, style, nav, header, footer, aside, .advertisement, .ads, .sidebar").remove();
        
        // Try to find main content areas
        String[] contentSelectors = {
            "main", "article", ".content", ".main-content", ".post-content",
            ".entry-content", "#content", "#main", ".container"
        };
        
        for (String selector : contentSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String text = element.text().trim();
                if (text.length() > 100) {
                    return cleanText(text);
                }
            }
        }
        
        // Fallback to body content
        String bodyText = doc.body().text().trim();
        return cleanText(bodyText);
    }
    
    private String cleanText(String text) {
        return text
            .replaceAll("\\s+", " ")
            .replaceAll("\\n\\s*\\n", "\n\n")
            .trim()
            .substring(0, Math.min(text.length(), properties.getFetchResultMaxLength()));
    }
    
    private List<String> extractKeywords(String title, String content) {
        String text = (title + " " + content).toLowerCase();
        String[] words = text.split("\\s+");
        
        Set<String> stopWords = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", 
            "had", "her", "was", "one", "our", "out", "day", "get", "has",
            "him", "his", "how", "man", "new", "now", "old", "see", "two",
            "way", "who", "boy", "did", "its", "let", "put", "say", "she",
            "too", "use", "with", "have", "this", "will", "your", "from",
            "they", "know", "want", "been", "good", "much", "some", "time",
            "very", "when", "come", "here", "just", "like", "long", "make",
            "many", "over", "such", "take", "than", "them", "well", "were"
        );
        
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            word = word.replaceAll("[^a-z]", "");
            if (word.length() >= 4 && !stopWords.contains(word)) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        
        // Return top keywords (minimum frequency of 2 for longer content)
        int minFreq = content.length() > 1000 ? 2 : 1;
        return wordCount.entrySet().stream()
            .filter(entry -> entry.getValue() >= minFreq)
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(8)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private String createIntelligentSummary(String title, String content, String url) {
        if (content == null || content.length() < 50) {
            return "Content from " + extractDomain(url) + ": " + title;
        }
        
        // Split content into paragraphs
        String[] paragraphs = content.split("\\n\\s*\\n");
        List<String> validParagraphs = new ArrayList<>();
        for (String p : paragraphs) {
            if (p.trim().length() > 20) {
                validParagraphs.add(p.trim());
            }
        }
        
        if (validParagraphs.isEmpty()) {
            return createFallbackSummary(content, title);
        }
        
        // Find the most substantial paragraph
        String bestParagraph = validParagraphs.get(0);
        double bestScore = 0;
        
        for (int i = 0; i < Math.min(validParagraphs.size(), 5); i++) {
            String paragraph = validParagraphs.get(i);
            String[] sentences = paragraph.split("[.!?]+");
            List<String> validSentences = new ArrayList<>();
            for (String s : sentences) {
                if (s.trim().length() > 10) {
                    validSentences.add(s);
                }
            }
            
            // Score based on length, sentence count, and position
            double lengthScore = Math.min(paragraph.length() / 200.0, 1.0);
            double sentenceScore = Math.min(validSentences.size() / 3.0, 1.0);
            double positionScore = i == 0 ? 0.8 : 1.0;
            
            double score = (lengthScore + sentenceScore) * positionScore;
            
            if (score > bestScore && paragraph.length() > 80) {
                bestScore = score;
                bestParagraph = paragraph;
            }
        }
        
        // Extract 2-3 key sentences from the best paragraph
        String[] sentences = bestParagraph.split("[.!?]+");
        StringBuilder summary = new StringBuilder();
        
        for (int i = 0; i < Math.min(sentences.length, 3); i++) {
            String sentence = sentences[i].trim();
            if ((summary.length() + sentence.length()) <= 300) {
                summary.append(sentence).append(". ");
            } else {
                break;
            }
        }
        
        // Ensure we have a reasonable summary
        if (summary.length() < 80) {
            return createFallbackSummary(content, title);
        }
        
        return summary.toString().trim();
    }
    
    private String createFallbackSummary(String content, String title) {
        String[] words = content.split("\\s+");
        int wordCount = Math.min(words.length, 50);
        StringBuilder summary = new StringBuilder();
        
        for (int i = 0; i < wordCount; i++) {
            summary.append(words[i]).append(" ");
        }
        
        String result = summary.toString().trim();
        
        if (result.length() < 100) {
            result = title + ": " + result;
        }
        
        // Clean up and ensure proper ending
        result = result.replaceAll("[.!?]*$", "");
        if (result.length() > 200) {
            result = result.substring(0, 197) + "...";
        } else {
            result += ".";
        }
        
        return result;
    }
    
    private String extractDomain(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getHost().replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return "unknown source";
        }
    }
}
