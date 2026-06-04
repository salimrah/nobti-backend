#!/usr/bin/env bash
set -euo pipefail

# ── noba-backend Restore Script ──
# Usage: ./scripts/restore.sh <backup-file>
#
# Examples:
#   ./scripts/restore.sh backups/noba-db-20260511_030000.sql.gz   # restore PostgreSQL
#   ./scripts/restore.sh backups/noba-redis-20260511_030000.rdb   # restore Redis
#   ./scripts/restore.sh --latest                                  # restore latest of each

MODE="${1:---help}"
BACKUP_DIR="${2:-./backups}"

DB_CONTAINER="${COMPOSE_PROJECT_NAME:-noba}-postgres-1"
REDIS_CONTAINER="${COMPOSE_PROJECT_NAME:-noba}-redis-1"
DB_USER="${PGUSER:-postgres}"
DB_NAME="${PGDATABASE:-noba}"

log() { echo "[$(date +%H:%M:%S)] $*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

restore_postgres() {
    local file="$1"
    log "Restoring PostgreSQL from: $file"
    [ -f "$file" ] || die "Backup file not found: $file"

    if ! docker ps --format '{{.Names}}' | grep -q "$DB_CONTAINER"; then
        die "PostgreSQL container $DB_CONTAINER not running"
    fi

    # Drop and recreate the database
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS ${DB_NAME}_restore;" 2>/dev/null || true
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -c "CREATE DATABASE ${DB_NAME}_restore;" 2>/dev/null || true

    gunzip -c "$file" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" "${DB_NAME}_restore" && {
        docker exec "$DB_CONTAINER" psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
        docker exec "$DB_CONTAINER" psql -U "$DB_USER" -c "ALTER DATABASE ${DB_NAME}_restore RENAME TO $DB_NAME;"
        log "PostgreSQL restore complete"
    } || die "PostgreSQL restore failed"
}

restore_redis() {
    local file="$1"
    log "Restoring Redis from: $file"
    [ -f "$file" ] || die "Backup file not found: $file"

    if ! docker ps --format '{{.Names}}' | grep -q "$REDIS_CONTAINER"; then
        die "Redis container $REDIS_CONTAINER not running"
    fi

    docker cp "$file" "$REDIS_CONTAINER:/data/dump.rdb"
    docker exec "$REDIS_CONTAINER" redis-cli SLAVEOF NO ONE 2>/dev/null || true
    docker exec "$REDIS_CONTAINER" redis-cli CONFIG SET dir /data
    docker exec "$REDIS_CONTAINER" redis-cli CONFIG SET dbfilename dump.rdb
    docker exec "$REDIS_CONTAINER" redis-cli SAVE
    docker restart "$REDIS_CONTAINER"
    log "Redis restore complete — container restarted"
}

case "$MODE" in
    *.sql.gz)  restore_postgres "$MODE" ;;
    *.rdb)     restore_redis "$MODE" ;;
    --latest)
        restore_postgres "$(ls -t "$BACKUP_DIR"/noba-db-*.sql.gz 2>/dev/null | head -1)"
        restore_redis "$(ls -t "$BACKUP_DIR"/noba-redis-*.rdb 2>/dev/null | head -1)"
        ;;
    --help|*)
        echo "Usage: $0 <backup-file> | --latest [backup-dir]"
        echo ""
        echo "  Restore PostgreSQL:  $0 backups/noba-db-20260511_030000.sql.gz"
        echo "  Restore Redis:       $0 backups/noba-redis-20260511_030000.rdb"
        echo "  Restore latest:      $0 --latest"
        echo ""
        echo "Requirements:"
        echo "  - Docker containers must be running"
        echo "  - PostgreSQL container accessible as: $DB_CONTAINER"
        echo "  - Redis container accessible as: $REDIS_CONTAINER"
        exit 0
        ;;
esac
