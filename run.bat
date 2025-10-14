@echo off
REM MCP WebSearch Server - Windows Startup Script

echo ========================================
echo   MCP WebSearch Server - Java/Spring
echo ========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/
    exit /b 1
)

REM Check Java version
java -version 2>&1 | findstr /C:"version" >nul
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    exit /b 1
)

echo [OK] Java and Maven found
echo.

REM Build the project if jar doesn't exist
set JAR_FILE=target\mcp-websearch-1.0.0.jar
if not exist "%JAR_FILE%" (
    echo Building project...
    call mvn clean package -DskipTests
    
    if %errorlevel% neq 0 (
        echo Build failed!
        exit /b 1
    )
    echo [OK] Build successful
    echo.
)

REM Parse command line arguments
set MODE=http
set PORT=3000
set DEBUG=

:parse_args
if "%~1"=="" goto end_parse
if "%~1"=="--stdio" set MODE=stdio
if "%~1"=="--debug" set DEBUG=--debug
if "%~1:~0,7%"=="--port=" set PORT=%~1:~7%
shift
goto parse_args
:end_parse

REM Run the application
echo Starting MCP WebSearch Server...
echo.

if "%MODE%"=="stdio" (
    echo Mode: stdio
    java -jar "%JAR_FILE%" --stdio %DEBUG%
) else (
    echo Mode: HTTP
    echo Port: %PORT%
    java -jar "%JAR_FILE%" --port=%PORT% %DEBUG%
)
