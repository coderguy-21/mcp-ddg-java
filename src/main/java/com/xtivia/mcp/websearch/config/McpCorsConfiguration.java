package com.xtivia.mcp.websearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for MCP HTTP transport.
 * 
 * <p>Configures Cross-Origin Resource Sharing (CORS) to support MCP protocol
 * communication from web clients, including proper handling of preflight OPTIONS requests.
 * 
 * <p>MCP Protocol Requirements:
 * <ul>
 *   <li>Support for POST requests to /mcp endpoint</li>
 *   <li>Preflight OPTIONS request handling</li>
 *   <li>Accept custom MCP protocol headers</li>
 *   <li>Support streamable HTTP transport for chunked responses</li>
 *   <li>Handle JSON-RPC 2.0 content type</li>
 * </ul>
 * 
 * <p>This configuration enables the MCP server to be accessed from:
 * <ul>
 *   <li>Web applications running on different ports</li>
 *   <li>Browser-based MCP clients</li>
 *   <li>Development tools (e.g., VS Code, Claude Desktop)</li>
 * </ul>
 */
@Configuration
public class McpCorsConfiguration {
    
    /**
     * Configure CORS filter for reactive web applications.
     * 
     * <p>This configuration:
     * <ul>
     *   <li>Allows all origins (*) for development/testing</li>
     *   <li>Supports GET, POST, OPTIONS, HEAD methods</li>
     *   <li>Accepts all headers including custom MCP headers</li>
     *   <li>Exposes necessary response headers for streaming</li>
     *   <li>Handles preflight caching for 1 hour</li>
     * </ul>
     * 
     * <p>Ordered with HIGHEST_PRECEDENCE to ensure it runs first in the filter chain,
     * before any other filters (including logging filters).
     * 
     * @return CorsWebFilter configured for MCP protocol
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow all origins for development
        // For production, replace with specific origins:
        // config.addAllowedOrigin("https://yourdomain.com");
        config.addAllowedOriginPattern("*");
        
        // Allow essential HTTP methods for MCP protocol
        config.addAllowedMethod("GET");      // Health checks, initial requests
        config.addAllowedMethod("POST");     // MCP JSON-RPC requests
        config.addAllowedMethod("OPTIONS");  // Preflight requests
        config.addAllowedMethod("HEAD");     // Status checks
        
        // Allow all headers including custom MCP protocol headers
        // MCP may use custom headers for:
        // - Content negotiation
        // - Authentication tokens
        // - Protocol versioning
        // - Client identification
        config.addAllowedHeader("*");
        
        // Common headers that should be explicitly allowed
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("X-MCP-Version");
        config.addAllowedHeader("X-MCP-Client");
        
        // Expose headers needed for streamable HTTP and MCP responses
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Content-Length");
        config.addExposedHeader("Transfer-Encoding");
        config.addExposedHeader("Cache-Control");
        config.addExposedHeader("X-MCP-Server-Version");
        config.addExposedHeader("X-Content-Type-Options");
        
        // Allow credentials (cookies, authorization headers)
        // Set to false if using wildcard origins, true for specific origins
        config.setAllowCredentials(false);
        
        // Cache preflight response for 1 hour to reduce OPTIONS requests
        config.setMaxAge(3600L);
        
        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Apply to MCP endpoint
        source.registerCorsConfiguration("/mcp/**", config);
        
        // Apply to all other endpoints (actuator, health checks, etc.)
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
