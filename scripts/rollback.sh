#!/usr/bin/env bash
# Rollback — reverts to the previous Docker image
# Usage: ./scripts/rollback.sh [previous-sha-or-tag]
#
# If no argument given, rolls back to the second-latest image tag.
# Requires: SSH access, Docker on server, compose file in /opt/noba

set -e

SERVER="${SERVER_USER}@${SERVER_HOST}"
COMPOSE_DIR="/opt/noba"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.app-only.yml"

# Detect which compose file exists
if ssh "$SERVER" "test -f ${COMPOSE_DIR}/docker-compose.app-only.yml"; then
    COMPOSE_FILE="docker-compose.app-only.yml"
elif ssh "$SERVER" "test -f ${COMPOSE_DIR}/docker-compose.prod.yml"; then
    COMPOSE_FILE="docker-compose.prod.yml"
else
    echo "ERROR: No compose file found on server at ${COMPOSE_DIR}"
    exit 1
fi

IMAGE="${1:-}"

if [ -z "$IMAGE" ]; then
    # Find the second-latest image
    echo "No image specified. Finding previous version..."
    IMAGE=$(ssh "$SERVER" "docker images --format '{{.Repository}}:{{.Tag}}' | grep noba-backend | head -2 | tail -1")
    if [ -z "$IMAGE" ]; then
        echo "ERROR: No previous image found to rollback to"
        exit 1
    fi
    echo "Rolling back to: $IMAGE"
else
    echo "Rolling back to: $IMAGE"
fi

echo ""
echo "=== Pulling previous image ==="
ssh "$SERVER" "docker pull $IMAGE"

echo ""
echo "=== Switching to previous version ==="
ssh "$SERVER" << EOF
  cd ${COMPOSE_DIR}
  docker stop noba-backend 2>/dev/null || true
  docker rm noba-backend 2>/dev/null || true
  IMAGE_NAME="${IMAGE%%:*}" DOCKER_TAG="${IMAGE##*:}" docker compose -f ${COMPOSE_FILE} up -d --no-deps noba-backend
EOF

echo ""
echo "=== Waiting for health check ==="
for i in $(seq 1 30); do
    if ssh "$SERVER" "curl -sf http://localhost:8080/actuator/health" > /dev/null 2>&1; then
        echo "Health check passed. Rollback complete."
        exit 0
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo "ERROR: Rollback health check failed"
echo "Run: docker logs noba-backend"
exit 1
