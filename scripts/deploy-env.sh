#!/usr/bin/env bash
# ==============================================================================
# Script de Despliegue Multi-Entorno para PuntoMatch
# Uso: ./deploy-env.sh [prod|staging] [build|up|down|logs|status]
# Ejemplos:
#   ./scripts/deploy-env.sh staging up
#   ./scripts/deploy-env.sh staging build
#   ./scripts/deploy-env.sh prod up
#   ./scripts/deploy-env.sh staging logs
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENV_NAME="${1:-staging}"
ACTION="${2:-up}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${GREEN}[PuntoMatch - ${ENV_NAME^^}]${NC} $1"; }
warn() { echo -e "${YELLOW}[AVISO]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

ENV_FILE="${PROJECT_ROOT}/.env.${ENV_NAME}"

if [ ! -f "$ENV_FILE" ]; then
    error "No se encontró el archivo de entorno: ${ENV_FILE}"
    echo "Entornos disponibles: prod, staging"
    exit 1
fi

COMPOSE_CMD="docker compose --env-file ${ENV_FILE} -f ${PROJECT_ROOT}/compose.yaml"

if [ "$ENV_NAME" = "staging" ] || [ "$ENV_NAME" = "dev" ]; then
    # En staging usamos compose.local.yaml para compilar codigo local
    COMPOSE_CMD="${COMPOSE_CMD} -f ${PROJECT_ROOT}/compose.local.yaml"
fi

log "Ejecutando acción '${ACTION}' para el entorno [${ENV_NAME}]..."

case "$ACTION" in
    up)
        $COMPOSE_CMD up -d --remove-orphans
        log "Servicios levantados en segundo plano."
        ;;
    build)
        $COMPOSE_CMD up -d --build --remove-orphans
        log "Servicios reconstruidos y desplegados."
        ;;
    down)
        $COMPOSE_CMD down
        log "Servicios detenidos."
        ;;
    logs)
        $COMPOSE_CMD logs -f
        ;;
    status)
        $COMPOSE_CMD ps
        ;;
    *)
        error "Acción desconocida: ${ACTION}. Usa: up, build, down, logs, status"
        exit 1
        ;;
esac
