#!/usr/bin/env bash
set -euo pipefail

# ── noba-backend Backup Script ──
# Usage: ./scripts/backup.sh [--db-only | --redis-only | --all] [output-dir]
#
# Creates timestamped backups of PostgreSQL and Redis.
# Designed for cron scheduling. Backups are rotated (keep last 30).
#
# Examples:
#   ./scripts/backup.sh                        # backup everything to ./backups/
#   ./scripts/backup.sh --db-only /mnt/backups # DB only to custom dir
#   ./scripts/backup.sh --redis-only           # Redis only

MODE="${1:---all}"
OUTPUT_DIR="${2:-./backups}"

DB_CONTAINER="${COMPOSE_PROJECT_NAME}-postgres-1"
REDIS_CONTAINER="${COMPOSE_PROJECT_NAME}-redis-1"

# Fallback container names
[ -z "${COMPOSE_PROJECT_NAME:-}" ] && DB_CONTAINER="noba-postgres-1"
[ -z "${COMPOSE_PROJECT_NAME:-}" ] && REDIS_CONTAINER="noba-redis-1"

DB_USER="${PGUSER:-postgres}"
DB_NAME="${PGDATABASE:-noba}"

mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

log() { echo "[$(date +%H:%M:%S)] $*"; }

# ── PostgreSQL Backup ──
backup_postgres() {
    local file="$OUTPUT_DIR/noba-db-$TIMESTAMP.sql.gz"
    log "Backing up PostgreSQL → $file"

    if docker ps --format '{{.Names}}' | grep -q "$DB_CONTAINER"; then
        docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$file"
        log "PostgreSQL backup complete ($(du -h "$file" | cut -f1))"
    else
        # Try direct pg_dump if container name doesn't match
        pg_dump -h "${PGHOST:-localhost}" -U "$DB_USER" "$DB_NAME" 2>/dev/null | gzip > "$file" && \
            log "PostgreSQL backup complete (direct)" || \
            log "WARNING: PostgreSQL backup skipped — container $DB_CONTAINER not found and direct connection failed"
    fi
}

# ── Redis Backup (RDB) ──
backup_redis() {
    local file="$OUTPUT_DIR/noba-redis-$TIMESTAMP.rdb"
    log "Backing up Redis → $file"

    if docker ps --format '{{.Names}}' | grep -q "$REDIS_CONTAINER"; then
        docker exec "$REDIS_CONTAINER" redis-cli SAVE > /dev/null
        docker cp "$REDIS_CONTAINER:/data/dump.rdb" "$file"
        log "Redis backup complete ($(du -h "$file" | cut -f1))"
    elif command -v redis-cli &> /dev/null; then
        redis-cli SAVE > /dev/null 2>&1
        cp /var/lib/redis/dump.rdb "$file" 2>/dev/null && \
            log "Redis backup complete (direct)" || \
            log "WARNING: Redis backup skipped — container not found and local dump.rdb not accessible"
    else
        log "WARNING: Redis backup skipped — no container or redis-cli found"
    fi
}

# ── Environment Backup (Docker Compose) ──
backup_env() {
    local file="$OUTPUT_DIR/noba-env-$TIMESTAMP.env"
    log "Backing up environment → $file"
    # Only export non-sensitive env vars (secrets should be backed up separately)
    {
        echo "# noba-backend environment backup — $TIMESTAMP"
        echo "# WARNING: Do NOT commit this file. Secrets must be added manually."
        echo ""
        env | grep -E '^(SPRING_|JWT_|APP_|RABBITMQ_|EVENT_|QUARTZ_|TRACING_|SENTRY_)' \
            | grep -vi 'PASSWORD\|SECRET\|KEY\|DSN' || true
    } > "$file"
    log "Environment backup complete ($(wc -l < "$file") vars, secrets excluded)"
}

# ── Rotation: Keep last 30 ──
rotate() {
    local pattern="$1"
    local keep="${2:-30}"
    ls -t "$OUTPUT_DIR"/$pattern 2>/dev/null | tail -n +$((keep + 1)) | while read -r old; do
        rm -f "$old"
        log "Removed old backup: $old"
    done
}

# ── Main ──
case "$MODE" in
    --db-only)    backup_postgres ;;
    --redis-only) backup_redis ;;
    --env-only)   backup_env ;;
    --all|*)
        backup_postgres
        backup_redis
        backup_env
        ;;
esac

rotate "noba-db-*.sql.gz" 30
rotate "noba-redis-*.rdb" 30
rotate "noba-env-*.env" 10

log "Backup complete — directory: $OUTPUT_DIR"
echo ""
echo "Quick restore commands:"
echo "  gunzip -c noba-db-$TIMESTAMP.sql.gz | docker exec -i $DB_CONTAINER psql -U $DB_USER $DB_NAME"
echo "  cp noba-redis-$TIMESTAMP.rdb /var/lib/redis/dump.rdb && redis-cli SAVE"
