#!/usr/bin/env bash
set -euo pipefail

echo "=== 1. Starting Docker services (PostgreSQL + Redis) ==="
if ! docker ps >/dev/null 2>&1; then
  echo "   Starting Docker daemon..."
  sudo systemctl start docker
  echo "   Waiting for Docker to be ready..."
  until docker ps >/dev/null 2>&1; do
    sleep 2
  done
fi
docker compose -f docker-compose.dev.yml up -d
echo "   PostgreSQL + Redis started."

echo "=== 2. Waiting for Postgres health check ==="
until docker exec noba-postgres-dev pg_isready -U postgres >/dev/null 2>&1; do
  sleep 2
done
echo "   Postgres is healthy."

echo "=== 3. Switching to Java 21 ==="
if [[ -d "$HOME/.sdkman/candidates/java/21-tem" ]]; then
  export JAVA_HOME="$HOME/.sdkman/candidates/java/21-tem"
  export PATH="$JAVA_HOME/bin:$PATH"
elif [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  set +u
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  if sdk list java 2>/dev/null | grep -q "21-tem"; then
    sdk use java 21-tem
  else
    sdk install java 21-tem
    sdk use java 21-tem
  fi
  set -u
elif command -v update-java-alternatives &>/dev/null; then
  JAVA_HOME=$(update-java-alternatives -l | grep "21" | awk '{print $3}')
  if [[ -n "$JAVA_HOME" ]]; then
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi
echo "   Java: $(java -version 2>&1 | head -1)"
echo "   JAVA_HOME: $JAVA_HOME"

echo "=== 4. Starting Spring Boot ==="
echo ""
LAUNCH_MODE="${1:-}"
if [[ -z "$LAUNCH_MODE" ]]; then
  echo -n "  Do you want to run the app from your IDE or from the terminal? [y/N]: "
  read -r response
  case "$response" in
    [yY]|[yY][eE][sS]) LAUNCH_MODE="terminal" ;;
    *) LAUNCH_MODE="ide" ;;
  esac
fi
case "$LAUNCH_MODE" in
  terminal|mvn|spring)
    echo ""
    echo "  Starting Spring Boot from terminal..."
    ./mvnw clean spring-boot:run
    ;;
  *)
    echo ""
    echo "  Skipping terminal start. Run the app from your IDE (IntelliJ, VS Code, etc.)."
    echo "  Entry point: src/main/java/ma/noba/NobaBackendApplication.java"
    ;;
esac
