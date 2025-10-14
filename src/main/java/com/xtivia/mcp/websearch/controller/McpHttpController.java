package com.xtivia.mcp.websearch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.xtivia.mcp.websearch.tool.WebSearchTool;
import com.xtivia.mcp.websearch.tool.ContentFetchTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * HTTP endpoint for MCP protocol communication.
 * 
 * Handles JSON-RPC 2.0 requests for the MCP protocol over HTTP.
 * Uses chunked transfer encoding for streamable responses.
 */
@RestController
@RequestMapping("/mcp")
public class McpHttpController {
    
    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);
    
    private final WebSearchTool webSearchTool;
    private final ContentFetchTool contentFetchTool;
    private final ObjectMapper objectMapper;
    
    public McpHttpController(WebSearchTool webSearchTool,
                            ContentFetchTool contentFetchTool,
                            ObjectMapper objectMapper) {
        this.webSearchTool = webSearchTool;
        this.contentFetchTool = contentFetchTool;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle MCP JSON-RPC 2.0 requests
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<String> handleMcpRequest(@RequestBody String requestBody) {
        // Use Mono.defer to defer execution and run on elastic scheduler
        return Mono.defer(() -> {
            log.debug("Received MCP request: {}", requestBody);
            
            try {
                JsonNode request = objectMapper.readTree(requestBody);
                String method = request.get("method").asText();
                Object id = request.has("id") ? request.get("id") : null;
                
                log.debug("MCP method: {}, id: {}", method, id);
                
                // Handle different MCP methods
                String result = switch (method) {
                    case "tools/list" -> handleToolsList(id);
                    case "tools/call" -> handleToolsCall(request, id);
                    case "initialize" -> handleInitialize(request, id);
                    default -> createErrorResponse(id, -32601, "Method not found: " + method);
                };
                
                log.debug("Sending MCP response: {}", result);
                return Mono.just(result);
                
            } catch (Exception e) {
                log.error("Error processing MCP request", e);
                return Mono.just(createErrorResponse(null, -32603, "Internal error: " + e.getMessage()));
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    private String handleToolsList(Object id) throws Exception {
        var tools = new Object[] {
            Map.of(
                "name", "web_search_tool",
                "description", "Intelligent web search using DuckDuckGo with Brave Search fallback. " +
                              "Supports query enhancement, rate limiting protection, and automatic fallback.",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Search query"),
                        "maxResults", Map.of("type", "integer", "description", "Maximum results (1-50)", "default", 10),
                        "dateFilter", Map.of("type", "string", "description", "Date filter (d=day, w=week, m=month, y=year)")
                    ),
                    "required", new String[] {"query"}
                )
            ),
            Map.of(
                "name", "content_fetch_tool",
                "description", "Fetch and extract content from a URL. Returns clean text content with metadata.",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "url", Map.of("type", "string", "description", "URL to fetch content from")
                    ),
                    "required", new String[] {"url"}
                )
            )
        };
        
        return objectMapper.writeValueAsString(Map.of(
            "jsonrpc", "2.0",
            "result", Map.of("tools", tools),
            "id", id
        ));
    }
    
    private String handleToolsCall(JsonNode request, Object id) throws Exception {
        JsonNode params = request.get("params");
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        
        log.debug("Calling tool: {} with arguments: {}", toolName, arguments);
        
        String result = switch (toolName) {
            case "web_search_tool" -> {
                var searchRequest = objectMapper.treeToValue(arguments, WebSearchTool.SearchRequest.class);
                yield webSearchTool.apply(searchRequest);
            }
            case "content_fetch_tool" -> {
                var fetchRequest = objectMapper.treeToValue(arguments, ContentFetchTool.FetchRequest.class);
                yield contentFetchTool.apply(fetchRequest);
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
        
        return objectMapper.writeValueAsString(Map.of(
            "jsonrpc", "2.0",
            "result", Map.of(
                "content", new Object[] {
                    Map.of("type", "text", "text", result)
                }
            ),
            "id", id
        ));
    }
    
    private String handleInitialize(JsonNode request, Object id) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "jsonrpc", "2.0",
            "result", Map.of(
                "protocolVersion", "2024-11-05",
                "serverInfo", Map.of(
                    "name", "mcp-websearch-server",
                    "version", "1.0.0"
                ),
                "capabilities", Map.of(
                    "tools", Map.of()
                )
            ),
            "id", id
        ));
    }
    
    private String createErrorResponse(Object id, int code, String message) {
        try {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("error", Map.of(
                "code", code,
                "message", message
            ));
            response.put("id", id); // Allow null id
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating error response", e);
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"},\"id\":null}";
        }
    }
}
