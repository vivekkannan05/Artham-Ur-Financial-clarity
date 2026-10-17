#!/bin/bash
# ─────────────────────────────────────────────────────────────
# Artha — Database Backup Script (per NFR-7.2)
#
# Usage: ./scripts/backup.sh
#
# Creates a timestamped SQL dump of the PostgreSQL database.
# Dumps are saved to ./backups/ (which is gitignored).
# ─────────────────────────────────────────────────────────────

set -euo pipefail

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="$(dirname "$0")/../backups"

mkdir -p "$BACKUP_DIR"

docker exec artha-postgres-1 pg_dump -U hfos hfos > "$BACKUP_DIR/artha-${TIMESTAMP}.sql"
echo "✅ Backup saved: $BACKUP_DIR/artha-${TIMESTAMP}.sql"
