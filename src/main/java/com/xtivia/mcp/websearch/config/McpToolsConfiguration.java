package com.xtivia.mcp.websearch.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MCP server tools.
 * 
 * In Spring AI 1.1, MCP server auto-discovers Function beans as tools.
 * The WebSearchTool and ContentFetchTool are already registered as @Component beans
 * that implement Function, so they will be automatically discovered and exposed
 * as MCP tools without needing explicit McpServerTool configuration.
 * 
 * Tool names are derived from the bean names (converted to kebab-case).
 * Tool descriptions and schemas are inferred from the Function implementations.
 */
@Configuration
public class McpToolsConfiguration {
    // Spring AI 1.1 MCP server automatically discovers Function<I, O> beans
    // and registers them as MCP tools. No explicit tool registration needed.
}
