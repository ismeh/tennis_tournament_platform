#!/usr/bin/env sh
set -eu

if [ "${1:-}" = "" ] || [ "${2:-}" = "" ]; then
  printf 'Usage: %s <domain> <email> [--staging]\n' "$0" >&2
  exit 1
fi

DOMAIN="$1"
EMAIL="$2"
STAGING_ARG=""

if [ "${3:-}" = "--staging" ]; then
  STAGING_ARG="--staging"
fi

COMPOSE="docker compose -f compose.yaml -f compose.demo.yaml"
RSA_KEY_SIZE=4096

mkdir -p infra/nginx/certbot/conf/live/"$DOMAIN"
mkdir -p infra/nginx/certbot/www

printf 'Creating temporary certificate for %s\n' "$DOMAIN"
PUBLIC_DOMAIN="$DOMAIN" $COMPOSE run --rm --entrypoint sh certbot -c "\
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem \
    -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem \
    -subj /CN=$DOMAIN"

printf 'Starting nginx with temporary certificate\n'
PUBLIC_DOMAIN="$DOMAIN" $COMPOSE up -d nginx

printf 'Removing temporary certificate\n'
PUBLIC_DOMAIN="$DOMAIN" $COMPOSE run --rm --entrypoint sh certbot -c "\
  rm -Rf /etc/letsencrypt/live/$DOMAIN \
         /etc/letsencrypt/archive/$DOMAIN \
         /etc/letsencrypt/renewal/$DOMAIN.conf"

printf 'Requesting Let'\''s Encrypt certificate for %s\n' "$DOMAIN"
PUBLIC_DOMAIN="$DOMAIN" $COMPOSE run --rm certbot certonly \
  --webroot \
  --webroot-path /var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --force-renewal \
  --rsa-key-size "$RSA_KEY_SIZE" \
  -d "$DOMAIN" \
  $STAGING_ARG

printf 'Reloading nginx\n'
PUBLIC_DOMAIN="$DOMAIN" $COMPOSE exec nginx nginx -s reload

printf 'HTTPS is ready for https://%s\n' "$DOMAIN"
