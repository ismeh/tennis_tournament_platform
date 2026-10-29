#!/bin/bash
set -e

# Frontend Lighthouse Performance Test Script
# This script builds the frontend, starts the SSR server, runs Lighthouse audits,
# and generates performance reports.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR"
DIST_DIR="$FRONTEND_DIR/dist/tfm_front"
SERVER_PID=""

cleanup() {
    echo "Cleaning up..."
    if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "Stopping SSR server (PID: $SERVER_PID)..."
        kill "$SERVER_PID"
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}

trap cleanup EXIT

echo "=== Frontend Lighthouse Performance Test ==="

# Step 1: Build the frontend
echo "Step 1: Building frontend..."
cd "$FRONTEND_DIR"
npm run build

# Step 2: Check if SSR server files exist
if [ ! -f "$DIST_DIR/server/server.mjs" ]; then
    echo "Error: SSR server file not found at $DIST_DIR/server/server.mjs"
    echo "Make sure the build includes SSR configuration."
    exit 1
fi

# Step 3: Start SSR server
echo "Step 2: Starting SSR server..."
node "$DIST_DIR/server/server.mjs" &
SERVER_PID=$!
echo "SSR server started with PID: $SERVER_PID"

# Wait for server to be ready
echo "Waiting for server to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:4000/ > /dev/null 2>&1; then
        echo "Server is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "Error: Server failed to start within 30 seconds"
        exit 1
    fi
    sleep 1
done

# Step 4: Run Lighthouse
echo "Step 3: Running Lighthouse audits..."
cd "$FRONTEND_DIR"
npx lhci autorun

echo "=== Lighthouse tests completed ==="
echo "Reports saved to: $FRONTEND_DIR/lighthouse-reports/"