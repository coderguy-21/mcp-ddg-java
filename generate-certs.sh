#!/bin/bash
# Generate a self-signed certificate for local HTTPS development
# Usage: bash generate-certs.sh

set -e

CERT_DIR="certs"
CERT_FILE="$CERT_DIR/cert.pem"
KEY_FILE="$CERT_DIR/key.pem"

mkdir -p "$CERT_DIR"


# Detect Git Bash and adjust -subj argument
SUBJ_ARG="/CN=localhost"
if [[ "$OSTYPE" == "msys"* || "$OSTYPE" == "win32"* || "$OSTYPE" == "cygwin"* ]]; then
  SUBJ_ARG="//CN=localhost"
fi

# Try with -addext (OpenSSL 1.1.1+), fallback if not supported
set +e
openssl req -x509 -newkey rsa:4096 -sha256 -days 365 \
  -nodes -keyout "$KEY_FILE" -out "$CERT_FILE" \
  -subj "$SUBJ_ARG" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
RESULT=$?
set -e
if [ $RESULT -ne 0 ]; then
  echo "OpenSSL -addext not supported, retrying without it..."
  openssl req -x509 -newkey rsa:4096 -sha256 -days 365 \
    -nodes -keyout "$KEY_FILE" -out "$CERT_FILE" \
    -subj "$SUBJ_ARG"
fi

echo "Self-signed certificate and key generated:"
echo "  Certificate: $CERT_FILE"
echo "  Key:         $KEY_FILE"
