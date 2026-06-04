#!/usr/bin/env bash
# =============================================================================
# Noba Backend — Database Migration Script
# =============================================================================
# Run Flyway migrations against a target environment BEFORE deploying new code.
#
# Usage:
#   scripts/migrate.sh                         # uses env vars from current shell
#   scripts/migrate.sh dev                      # run against local dev DB
#   scripts/migrate.sh staging                  # run against staging DB
#   scripts/migrate.sh prod                     # run against production DB
#
# Environment variables required:
#   SPRING_DATASOURCE_URL
#   SPRING_DATASOURCE_USERNAME
#   SPRING_DATASOURCE_PASSWORD
#
# Docker Compose (alternative):
#   docker compose -f docker-compose.prod.yml run --rm migration
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

ENV="${1:-}"
if [ -n "$ENV" ]; then
    echo "==> Loading .env.$ENV if present..."
    if [ -f ".env.$ENV" ]; then
        set -a
        source ".env.$ENV"
        set +a
    fi
    # Also try plain .env
    if [ -f ".env" ]; then
        set -a
        source ".env"
        set +a
    fi
fi

# Validate required vars
: "${SPRING_DATASOURCE_URL:?Must set SPRING_DATASOURCE_URL}"
: "${SPRING_DATASOURCE_USERNAME:?Must set SPRING_DATASOURCE_USERNAME}"
: "${SPRING_DATASOURCE_PASSWORD:?Must set SPRING_DATASOURCE_PASSWORD}"

echo "==> Target: $SPRING_DATASOURCE_URL"
echo "==> Running Flyway migrations..."

./mvnw flyway:migrate \
    -Dflyway.url="$SPRING_DATASOURCE_URL" \
    -Dflyway.user="$SPRING_DATASOURCE_USERNAME" \
    -Dflyway.password="$SPRING_DATASOURCE_PASSWORD" \
    -Dflyway.locations="filesystem:src/main/resources/db/migration" \
    -Dflyway.baselineOnMigrate=true \
    -q

echo "==> Migrations complete!"
