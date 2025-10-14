#!/bin/bash

# MCP WebSearch Server - Startup Script

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  MCP WebSearch Server - Java/Spring${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    echo "Please install Maven from https://maven.apache.org/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}Error: Java 21 or higher is required${NC}"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

echo -e "${GREEN}✓ Java version OK${NC}"
echo -e "${GREEN}✓ Maven found${NC}"
echo ""

# Build the project if jar doesn't exist
JAR_FILE="target/mcp-websearch-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}Building project...${NC}"
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed!${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Build successful${NC}"
    echo ""
fi

# Parse command line arguments
MODE="http"
PORT="3000"
DEBUG=""

for arg in "$@"; do
    case $arg in
        --stdio)
            MODE="stdio"
            ;;
        --debug)
            DEBUG="--debug"
            ;;
        --port=*)
            PORT="${arg#*=}"
            ;;
    esac
done

# Run the application
echo -e "${GREEN}Starting MCP WebSearch Server...${NC}"
echo ""

if [ "$MODE" = "stdio" ]; then
    echo -e "${YELLOW}Mode: stdio${NC}"
    java -jar "$JAR_FILE" --stdio $DEBUG
else
    echo -e "${YELLOW}Mode: HTTP${NC}"
    echo -e "${YELLOW}Port: $PORT${NC}"
    java -jar "$JAR_FILE" --port=$PORT $DEBUG
fi
