#!/bin/bash

# Simple test script for MCP WebSearch

QUERY="${1:-test query}"
MAX_RESULTS="${2:-3}"

echo "Searching for: $QUERY"
echo "Max results: $MAX_RESULTS"
echo ""

RESPONSE=$(curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"web_search_tool\",\"arguments\":{\"query\":\"$QUERY\",\"maxResults\":$MAX_RESULTS}}}")

# Parse and display summary
PROVIDER=$(echo "$RESPONSE" | grep -o '"searchProvider" : "[^"]*"' | cut -d'"' -f4)
TOTAL=$(echo "$RESPONSE" | grep -o '"totalResults" : [0-9]*' | grep -o '[0-9]*')

echo "Provider: $PROVIDER"
echo "Results: $TOTAL"
echo ""

# Show result titles
echo "$RESPONSE" | grep -o '"title" : "[^"]*"' | cut -d'"' -f4 | nl

echo ""
echo "✓ Search completed"
