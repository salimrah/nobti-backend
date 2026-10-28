#!/usr/bin/env zsh
set -euo pipefail

echo "=== 1. Starting Docker services (PostgreSQL + Redis) ==="
if ! docker ps >/dev/null 2>&1; then
  open -a Docker
  echo "   Waiting for Docker to be ready..."
  until docker ps >/dev/null 2>&1; do
    sleep 2
  done
fi
# The local stack is broker-less: modules use the in-process event bus.
# Pass --broker (or set EVENT_BUS_RABBITMQ_ENABLED=true) when testing the
# RabbitMQ transport or running more than one app instance.
LAUNCH_MODE="${1:-}"
if [[ "$LAUNCH_MODE" == "--broker" ]]; then
  docker compose -f docker-compose.dev.yml -f docker-compose.broker.yml --profile broker up -d
  export EVENT_BUS_RABBITMQ_ENABLED="${EVENT_BUS_RABBITMQ_ENABLED:-true}"
  echo "   PostgreSQL + Redis + RabbitMQ started (EVENT_BUS_RABBITMQ_ENABLED=$EVENT_BUS_RABBITMQ_ENABLED)."
  shift
else
  docker compose -f docker-compose.dev.yml up -d
  export EVENT_BUS_RABBITMQ_ENABLED="${EVENT_BUS_RABBITMQ_ENABLED:-false}"
  echo "   PostgreSQL + Redis started (in-process events, no broker)."
fi

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
elif [[ -d "/opt/homebrew/Cellar/openjdk@21" ]]; then
  export JAVA_HOME=$(ls -d /opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home | tail -1)
  export PATH="$JAVA_HOME/bin:$PATH"
fi
echo "   Java: $(java -version 2>&1 | head -1)"
echo "   JAVA_HOME: $JAVA_HOME"

echo "=== 4. Starting Spring Boot ==="
echo ""
if [[ -z "${LAUNCH_MODE:-}" ]]; then
  echo -n "  Do you want to run the app from your IDE or from the terminal? [y/N]: "
  read -r response
  case "$response" in
    [yY]|[yY][eE][sS]) LAUNCH_MODE="terminal" ;;
    *) LAUNCH_MODE="ide" ;;
  esac
fi
case "${LAUNCH_MODE:-}" in
  terminal|mvn|spring)
    echo ""
    echo "  Starting Spring Boot from terminal..."
    ./mvnw -q -DskipTests -Dspotbugs.skip=true -Djacoco.skip=true -pl modules/noba-app -am install && \
      ./mvnw -pl modules/noba-app spring-boot:run
    ;;
  *)
    echo ""
    echo "  Skipping terminal start. Run the app from your IDE (IntelliJ, VS Code, etc.)."
    echo "  Entry point: src/main/java/ma/noba/NobaBackendApplication.java"
    ;;
esac
