#!/usr/bin/env sh
set -eu

CERT_DIR="${CERT_DIR:-infra/nginx/certs/local}"
COMMON_NAME="${LOCAL_SERVER_NAME:-localhost}"

mkdir -p "$CERT_DIR"

openssl req \
  -x509 \
  -nodes \
  -newkey rsa:2048 \
  -sha256 \
  -days 365 \
  -keyout "$CERT_DIR/privkey.pem" \
  -out "$CERT_DIR/fullchain.pem" \
  -subj "/CN=$COMMON_NAME" \
  -addext "subjectAltName=DNS:localhost,DNS:$COMMON_NAME,IP:127.0.0.1"

printf 'Generated local HTTPS certificate in %s\n' "$CERT_DIR"
