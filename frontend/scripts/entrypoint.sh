#!/bin/sh
set -eu

TEMPLATE_PATH="/app/dist/tfm_front/browser/config.template.json"
CONFIG_PATH="/app/dist/tfm_front/browser/config.json"

if [ -f "$TEMPLATE_PATH" ]; then
  envsubst < "$TEMPLATE_PATH" > "$CONFIG_PATH"
fi

exec node dist/tfm_front/server/server.mjs