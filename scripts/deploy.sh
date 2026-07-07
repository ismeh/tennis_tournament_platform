#!/bin/bash
# Zero-downtime deployment script for Tennis Tournament Platform
# Usage: ./deploy.sh [backend|frontend|all]
#
# Environment variables:
#   EC2_OVERRIDE=true   Appends compose.override.ec2.yaml (memory limits for t3.small)
#   BUILD_LOCAL=true    Builds images locally instead of pulling from registry

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="${PROJECT_ROOT}/compose.yaml"
EC2_OVERRIDE_FILE="${PROJECT_ROOT}/compose.override.ec2.yaml"
MAINTENANCE_FLAG="/etc/nginx/maintenance.flag"

COMPOSE_ARGS="-f ${COMPOSE_FILE}"
if [ "${EC2_OVERRIDE:-false}" = "true" ] && [ -f "$EC2_OVERRIDE_FILE" ]; then
  COMPOSE_ARGS="${COMPOSE_ARGS} -f ${EC2_OVERRIDE_FILE}"
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[DEPLOY]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

enable_maintenance() {
    log "Enabling maintenance mode..."
    touch "$MAINTENANCE_FLAG"
    sleep 2
}

disable_maintenance() {
    log "Disabling maintenance mode..."
    rm -f "$MAINTENANCE_FLAG"
}

wait_for_healthy() {
    local service=$1
    local max_attempts=30
    local attempt=1

    log "Waiting for $service to become healthy..."
    while [ $attempt -le $max_attempts ]; do
        if docker compose $COMPOSE_ARGS exec -T "$service" curl -sf http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
            log "$service is healthy!"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    error "$service failed to become healthy after $max_attempts attempts"
    return 1
}

deploy_backend() {
    log "Deploying backend..."

    enable_maintenance

    # Pull latest image or rebuild
    if [ "${BUILD_LOCAL:-false}" = "true" ]; then
        log "Building backend image locally..."
        docker compose $COMPOSE_ARGS build backend
    fi

    # Start new container before stopping old one
    log "Starting new backend container..."
    docker compose $COMPOSE_ARGS up -d --no-deps --wait backend

    # Wait for new container to be healthy
    if wait_for_healthy "backend"; then
        log "Backend deployed successfully!"
        disable_maintenance
    else
        error "Backend deployment failed! Rolling back..."
        docker compose $COMPOSE_ARGS rollback backend 2>/dev/null || true
        disable_maintenance
        exit 1
    fi
}

deploy_frontend() {
    log "Deploying frontend..."

    enable_maintenance

    if [ "${BUILD_LOCAL:-false}" = "true" ]; then
        log "Building frontend image locally..."
        docker compose $COMPOSE_ARGS build frontend
    fi

    log "Starting new frontend container..."
    docker compose $COMPOSE_ARGS up -d --no-deps --wait frontend

    # Frontend doesn't have health endpoint, just check if it's running
    sleep 3
    if docker compose $COMPOSE_ARGS ps frontend | grep -q "Up"; then
        log "Frontend deployed successfully!"
        disable_maintenance
    else
        error "Frontend deployment failed!"
        disable_maintenance
        exit 1
    fi
}

deploy_nginx() {
    log "Reloading nginx configuration..."
    docker compose $COMPOSE_ARGS exec -T nginx nginx -s reload 2>/dev/null || true
    log "Nginx reloaded!"
}

# Main
TARGET="${1:-all}"

case "$TARGET" in
    backend)
        deploy_backend
        deploy_nginx
        ;;
    frontend)
        deploy_frontend
        deploy_nginx
        ;;
    all)
        deploy_backend
        deploy_frontend
        deploy_nginx
        ;;
    *)
        error "Usage: $0 [backend|frontend|all]"
        exit 1
        ;;
esac

log "Deployment complete!"
