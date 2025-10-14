package com.xtivia.mcp.websearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.xtivia.mcp.websearch.config.WebSearchProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 
 * <p>Usage:
 * <pre>
 *   # HTTP mode with streamable transport (default)
 *   java -jar mcp-websearch.jar
 *   
 *   # STDIO mode for process communication
 *   java -jar mcp-websearch.jar --stdio
 *   
 *   # Custom port
 *   java -jar mcp-websearch.jar --port=8080
 *   
 *   # Debug mode
 *   java -jar mcp-websearch.jar --debug
 * </pre>
 */
@SpringBootApplication
@EnableConfigurationProperties(WebSearchProperties.class)
public class McpWebSearchApplication {

    public static void main(String[] args) {
        boolean stdioMode = false;
        boolean debugMode = false;
        String customPort = null;
        
        // Check for SSL certificates
        boolean httpsEnabled = checkAndConfigureSSL();
        
        for (String arg : args) {
            if ("--stdio".equals(arg)) {
                stdioMode = true;
                // Set Spring AI MCP server transport to stdio
                System.setProperty("spring.ai.mcp.server.transport", "stdio");
            } else if ("--debug".equals(arg)) {
                debugMode = true;
                System.setProperty("websearch.debug", "true");
                System.setProperty("logging.level.com.xtivia.mcp", "DEBUG");
            } else if (arg.startsWith("--port=")) {
                customPort = arg.substring(7);
                System.setProperty("server.port", customPort);
            }
        }
        
        // Display startup information
        if (stdioMode) {
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("🔌 MCP WebSearch Server - STDIO Mode");
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("Transport: Standard Input/Output");
            System.err.println("Communication: Direct process IPC");
        } else {
            String port = customPort != null ? customPort : 
                          System.getProperty("server.port", "3000");
            String protocol = httpsEnabled ? "https" : "http";
            String securityIcon = httpsEnabled ? "🔒" : "🌐";
            String securityMode = httpsEnabled ? "HTTPS Secure Mode" : "HTTP Mode";
            
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println(securityIcon + " MCP WebSearch Server - " + securityMode);
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("Transport: HTTP with chunked transfer encoding");
            System.err.println("Endpoint: " + protocol + "://localhost:" + port + "/mcp");
            
            if (httpsEnabled) {
                System.err.println("SSL: Enabled (certificates found in ./certs/)");
            } else {
                System.err.println("SSL: Disabled (no certificates found)");
            }
            
            System.err.println("");
            System.err.println("Available Tools:");
            System.err.println("  • web-search-tool - DuckDuckGo/Brave web search");
            System.err.println("  • content-fetch-tool - URL content extraction");
            System.err.println("");
            System.err.println("Options:");
            System.err.println("  --stdio          Run in STDIO mode");
            System.err.println("  --port=<number>  Custom port (default: 3000)");
            System.err.println("  --debug          Enable debug logging");
        }
        
        if (debugMode) {
            System.err.println("");
            System.err.println("🐛 Debug mode enabled");
        }
        
        System.err.println("═══════════════════════════════════════════════════════════");
        
        
        SpringApplication.run(McpWebSearchApplication.class, args);
    }
    
    /**
     * Checks for SSL certificates in the ./certs/ directory and configures
     * Spring Boot to use HTTPS if both cert.pem and key.pem are found.
     * 
     * @return true if HTTPS is enabled, false otherwise
     */
    private static boolean checkAndConfigureSSL() {
        try {
            // Get the application's working directory
            Path certsDir = Paths.get("certs");
            Path certFile = certsDir.resolve("cert.pem");
            Path keyFile = certsDir.resolve("key.pem");
            
            // Check if certs directory exists
            if (!Files.exists(certsDir) || !Files.isDirectory(certsDir)) {
                return false;
            }
            
            // Check if both certificate files exist
            if (!Files.exists(certFile) || !Files.exists(keyFile)) {
                return false;
            }
            
            // Check if files are readable and not empty
            if (!Files.isReadable(certFile) || !Files.isReadable(keyFile)) {
                System.err.println("⚠️  Warning: Certificate files found but not readable");
                return false;
            }
            
            if (Files.size(certFile) == 0 || Files.size(keyFile) == 0) {
                System.err.println("⚠️  Warning: Certificate files found but are empty");
                return false;
            }
            
            // Configure Spring Boot for HTTPS using the simpler format
            System.setProperty("server.ssl.enabled", "true");
            System.setProperty("server.ssl.certificate", "file:" + certFile.toAbsolutePath().toString());
            System.setProperty("server.ssl.certificate-private-key", "file:" + keyFile.toAbsolutePath().toString());
            
            return true;
            
        } catch (Exception e) {
            System.err.println("⚠️  Warning: Error checking for SSL certificates: " + e.getMessage());
            return false;
        }
    }
}
