#!/bin/sh
set -eu

TEMPLATE_PATH="/app/dist/puntomatch/browser/config.template.json"
CONFIG_PATH="/app/dist/puntomatch/browser/config.json"

if [ -f "$TEMPLATE_PATH" ]; then
  envsubst < "$TEMPLATE_PATH" > "$CONFIG_PATH"
fi

exec node dist/puntomatch/server/server.mjs