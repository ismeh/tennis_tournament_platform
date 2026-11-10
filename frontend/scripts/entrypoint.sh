#!/bin/sh
set -eu

TEMPLATE_PATH="/app/dist/tfm_front/browser/config.template.json"
CONFIG_PATH="/app/dist/tfm_front/browser/config.json"

if [ -f "$TEMPLATE_PATH" ]; then
  # The browser talks to the public origin, which proxies /api to the backend.
  # The SSR server keeps using the absolute API_URL provided to the container.
  API_URL="${PUBLIC_API_URL:-/api}" envsubst '${API_URL} ${PRODUCTION}' < "$TEMPLATE_PATH" > "$CONFIG_PATH"
fi

exec node dist/tfm_front/server/server.mjs
