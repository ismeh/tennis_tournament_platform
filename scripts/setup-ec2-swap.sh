#!/bin/bash
# Setup 1 GB swap on EC2 EBS — run once as root or with sudo.
# Swap is a safety net for OOM spikes, not a substitute for adequate RAM.
#
# Usage: sudo bash scripts/setup-ec2-swap.sh

set -euo pipefail

SWAP_FILE="/swapfile"
SWAP_SIZE="1G"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[SWAP]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

if swapon --show | grep -q "$SWAP_FILE"; then
  warn "Swap already active at ${SWAP_FILE}. Nothing to do."
  free -h
  exit 0
fi

log "Creating ${SWAP_SIZE} swap file at ${SWAP_FILE}..."
fallocate -l "$SWAP_SIZE" "$SWAP_FILE"
chmod 600 "$SWAP_FILE"
mkswap "$SWAP_FILE"
swapon "$SWAP_FILE"

if ! grep -q "$SWAP_FILE" /etc/fstab; then
  echo "${SWAP_FILE} none swap sw 0 0" >> /etc/fstab
  log "Persisted swap in /etc/fstab (survives reboots)."
fi

# Reduce swappiness: kernel only uses swap when RAM is >90% full.
if ! grep -q "vm.swappiness" /etc/sysctl.conf; then
  echo "vm.swappiness=10" >> /etc/sysctl.conf
  sysctl -p > /dev/null
  log "Set vm.swappiness=10 (kernel prefers RAM until 90% full)."
fi

log "Done. Current memory status:"
free -h
