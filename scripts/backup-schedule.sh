#!/usr/bin/env bash
# ── noba-backend Scheduled Backup Cron Wrapper ──
# Add to crontab:
#   0 3 * * * /opt/noba/scripts/backup-schedule.sh
#
# This runs daily at 3 AM and:
#   1. Backs up PostgreSQL + Redis
#   2. Keeps 30 daily backups
#   3. Uploads to S3/GCS if configured (optional)

cd "$(dirname "$0")/.."

BACKUP_DIR="/opt/noba/backups"
LOG_DIR="/opt/noba/logs"
mkdir -p "$BACKUP_DIR" "$LOG_DIR"

LOG_FILE="$LOG_DIR/backup-$(date +%Y%m%d).log"

./scripts/backup.sh --all "$BACKUP_DIR" >> "$LOG_FILE" 2>&1

# ── Optional: Upload to S3-compatible storage ──
if [ -n "${S3_BACKUP_BUCKET:-}" ]; then
    echo "" >> "$LOG_FILE"
    echo "Uploading to S3: $S3_BACKUP_BUCKET" >> "$LOG_FILE"
    
    LATEST_DB=$(ls -t "$BACKUP_DIR"/noba-db-*.sql.gz 2>/dev/null | head -1)
    LATEST_REDIS=$(ls -t "$BACKUP_DIR"/noba-redis-*.rdb 2>/dev/null | head -1)
    
    if command -v aws &> /dev/null; then
        [ -n "$LATEST_DB" ] && aws s3 cp "$LATEST_DB" "$S3_BACKUP_BUCKET/db/" >> "$LOG_FILE" 2>&1
        [ -n "$LATEST_REDIS" ] && aws s3 cp "$LATEST_REDIS" "$S3_BACKUP_BUCKET/redis/" >> "$LOG_FILE" 2>&1
    elif command -v rclone &> /dev/null; then
        [ -n "$LATEST_DB" ] && rclone copy "$LATEST_DB" "$S3_BACKUP_BUCKET/db/" >> "$LOG_FILE" 2>&1
        [ -n "$LATEST_REDIS" ] && rclone copy "$LATEST_REDIS" "$S3_BACKUP_BUCKET/redis/" >> "$LOG_FILE" 2>&1
    else
        echo "WARNING: aws/rclone not found, skipping S3 upload" >> "$LOG_FILE"
    fi
fi

echo "Backup completed at $(date)" >> "$LOG_FILE"
