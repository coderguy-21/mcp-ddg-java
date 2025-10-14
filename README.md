# MCP WebSearch Server - Java/Spring AI Implementation

This is a Java/Spring Boot implementation of the MCP WebSearch server with the same features as the Node.js version.

## Features

- ✅ **MCP Server Implementation** using Spring AI MCP SDK
- ✅ **Web Search Tools**:
  - DuckDuckGo as primary search provider
  - Brave Search as automatic fallback
  - Intelligent query enhancement with preferred sites
  - Rate limiting and suspension management
- ✅ **Content Fetching Tool**: Extract and parse web page content
- ✅ **Dual Transport Support**: stdio and HTTP/HTTPS
- ✅ **Automatic HTTPS Detection**: Enables SSL when certificates are found in `./certs/`
- ✅ **Debug Mode**: Comprehensive logging for troubleshooting
- ✅ **Preferred Sites**: Auto-enhance queries with relevant site: operators

## Prerequisites

- Java 21 or higher
- Maven 3.8+

## Project Structure

```
java/
├── pom.xml                           # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/xtivia/mcp/websearch/
│   │   │   ├── McpWebSearchApplication.java       # Main application
│   │   │   ├── config/
│   │   │   │   └── WebSearchProperties.java       # Configuration properties
│   │   │   ├── model/
│   │   │   │   ├── SearchResult.java              # Search result model
│   │   │   │   ├── SearchResponse.java            # Search response model
│   │   │   │   ├── FetchResult.java               # Fetch result model
│   │   │   │   └── PreferredSite.java             # Preferred site model
│   │   │   ├── provider/
│   │   │   │   ├── SearchProvider.java            # Provider interface
│   │   │   │   ├── DuckDuckGoProvider.java        # DuckDuckGo implementation
│   │   │   │   └── BraveProvider.java             # Brave Search implementation
│   │   │   ├── service/
│   │   │   │   ├── PreferredSitesManager.java     # Site preferences
│   │   │   │   ├── RequestManager.java            # Rate limiting
│   │   │   │   └── ContentFetchService.java       # Content fetching
│   │   │   ├── tool/
│   │   │   │   ├── WebSearchTool.java             # Search MCP tool
│   │   │   │   └── ContentFetchTool.java          # Fetch MCP tool
│   │   │   └── util/
│   │   │       ├── KeywordExtractor.java          # Keyword extraction
│   │   │       └── SummaryGenerator.java          # Summary generation
│   │   └── resources/
│   │       ├── application.yml                    # Spring configuration
│   │       └── preferred_sites.json               # Site preferences
│   └── test/
│       └── java/com/xtivia/mcp/websearch/
│           └── McpWebSearchApplicationTests.java
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
websearch:
  provider: duckduckgo                 # duckduckgo or brave
  max-concurrent-requests: 4
  rate-limit: 10
  rate-limit-window-seconds: 60
  search-results-count: 10
  search-result-max-length: 400
  fetch-result-max-length: 5000
  debug: false
```

## Build and Run

### Build the project

```bash
cd java
mvn clean package
```

### Run in stdio mode

```bash
java -jar target/mcp-websearch-1.0.0.jar --stdio
```

### Run as HTTP server

```bash
java -jar target/mcp-websearch-1.0.0.jar
# Or specify custom port
java -jar target/mcp-websearch-1.0.0.jar --port=8080
```

### Run with HTTPS (Automatic Detection)

The application automatically detects SSL certificates and enables HTTPS if:
1. A `certs/` folder exists in the working directory
2. Both `cert.pem` and `key.pem` files are present in the folder
3. The certificate files are readable and not empty

If certificates are found, the server will start in HTTPS mode automatically. Otherwise, it falls back to HTTP mode.

**Generate self-signed certificates (for development):**

On Linux/Mac:
```bash
./generate-certs.sh
```

On Windows:
```bash
openssl req -x509 -newkey rsa:4096 -keyout certs/key.pem -out certs/cert.pem -days 365 -nodes
```

The server will display which mode it's running in at startup:
- 🔒 HTTPS Secure Mode (when certificates are found)
- 🌐 HTTP Mode (when no certificates are detected)

### Enable debug mode

```bash
java -jar target/mcp-websearch-1.0.0.jar --debug
```

## MCP Tools

### 1. search
Intelligent web search with DuckDuckGo (primary) and Brave (fallback).

**Parameters:**
- `query` (string, required): Search query
- `maxResults` (integer, optional): Max results (default: 10, max: 50)
- `dateFilter` (string, optional): Date filter (d=day, w=week, m=month, y=year)

**Returns:**
```json
{
  "query": "search term",
  "totalResults": 10,
  "searchProvider": "DuckDuckGo",
  "results": [
    {
      "title": "Result Title",
      "url": "https://example.com",
      "keywords": ["keyword1", "keyword2"],
      "summary": "Intelligent summary of the result..."
    }
  ]
}
```

### 2. fetch
Fetch and extract content from a URL.

**Parameters:**
- `url` (string, required): URL to fetch

**Returns:**
```json
{
  "url": "https://example.com",
  "title": "Page Title",
  "content": "Extracted content...",
  "summary": "Intelligent summary...",
  "keywords": ["keyword1", "keyword2"],
  "metadata": {
    "domain": "example.com",
    "contentType": "text/html",
    "contentLength": 1234,
    "lastModified": "2025-01-01T00:00:00Z"
  }
}
```

## Key Features

### Intelligent Fallback System
- Primary: DuckDuckGo HTML search
- Automatic fallback to Brave Search when DuckDuckGo is rate limited
- Exponential backoff suspension system (20min → 40min → 80min → max 120min)
- Automatic recovery when DuckDuckGo becomes available

### Query Enhancement
Automatically enhances queries with `site:` operators based on keywords:
- Programming queries → Stack Overflow, GitHub, MDN
- Package/module queries → npmjs.com
- Tutorial queries → Medium, YouTube
- Definition queries → Wikipedia

### Rate Limiting Protection
- Minimum 2-second delay between requests
- Maximum 10 requests per minute
- Rotating user agents
- Random jitter to appear human-like

### Content Extraction
- Smart HTML parsing with Jsoup
- Removes navigation, ads, and clutter
- Extracts main content intelligently
- Generates summaries from key paragraphs
- Extracts relevant keywords

## Dependencies

- **Spring Boot 3.4.1**: Application framework
- **Spring AI MCP**: Model Context Protocol SDK
- **Spring WebFlux**: Reactive HTTP client
- **Jsoup 1.18.1**: HTML parsing
- **Jackson**: JSON processing
- **Lombok**: Boilerplate reduction

## Testing

```bash
cd java
mvn test
```

## Development

### Hot Reload
The project includes spring-boot-devtools for automatic restart during development.

### Debug Logging
Enable debug mode to see detailed logs:
```bash
java -jar target/mcp-websearch-1.0.0.jar --debug
```

## Integration with Claude Desktop

Add to your Claude Desktop config:

**stdio mode:**
```json
{
  "mcpServers": {
    "websearch": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-websearch-1.0.0.jar",
        "--stdio"
      ]
    }
  }
}
```

**HTTP mode:**
```json
{
  "mcpServers": {
    "websearch": {
      "url": "http://localhost:3000/mcp"
    }
  }
}
```

**HTTPS mode** (when certificates are detected):
```json
{
  "mcpServers": {
    "websearch": {
      "url": "https://localhost:3000/mcp"
    }
  }
}
```

## Comparison with Node.js Version

| Feature | Node.js | Java/Spring |
|---------|---------|-------------|
| MCP SDK | ✅ @modelcontextprotocol/sdk | ✅ Spring AI MCP |
| Search Providers | ✅ DDG + Brave | ✅ DDG + Brave |
| Query Enhancement | ✅ | ✅ |
| Rate Limiting | ✅ | ✅ |
| Content Fetching | ✅ Cheerio | ✅ Jsoup |
| Stdio Transport | ✅ | ✅ |
| HTTP Transport | ✅ Express | ✅ Spring WebFlux |
| HTTPS Support | ✅ | ✅ |
| Debug Mode | ✅ | ✅ |
| Preferred Sites | ✅ | ✅ |

## License

MIT

## Contributing

Contributions welcome! Please feel free to submit a Pull Request.
