#!/usr/bin/env sh
# Script to renew Let's Encrypt certificates via certbot container
set -eu

# Get the directory of the script and go to the project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== Starting Let's Encrypt Certificate Renewal: $(date) ==="

# Run certbot renew
docker compose -f compose.yaml -f compose.demo.yaml run --rm certbot renew

# Reload Nginx to apply any changes (using -T for non-interactive shells like cron)
docker compose -f compose.yaml -f compose.demo.yaml exec -T nginx nginx -s reload

echo "=== Renewal Process Finished: $(date) ==="
