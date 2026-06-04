#!/usr/bin/env bash
# Smoke tests — run AFTER deploy to verify the app actually works
# Usage: ./scripts/smoke-test.sh <base-url> [timeout-seconds]
#
# Returns 0 if all pass, 1 if any fail.
# Called by CI after deploy, or manually to check a running instance.

set -e

BASE_URL="${1:-http://localhost:8080}"
TIMEOUT="${2:-60}"

PASS=0
FAIL=0
TOTAL=0

# Colors for terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { echo -e "  ${GREEN}✓${NC} $1"; PASS=$((PASS + 1)); }
log_fail() { echo -e "  ${RED}✗${NC} $1 — $2"; FAIL=$((FAIL + 1)); }
log_skip() { echo -e "  ${YELLOW}○${NC} $1 (skipped)"; }

run_test() {
    local description="$1"
    local method="${2:-GET}"
    local path="$3"
    local expected_code="${4:-200}"
    local body="${5:-}"

    TOTAL=$((TOTAL + 1))
    local url="${BASE_URL}${path}"

    if [ -n "$body" ]; then
        response=$(curl -sf -o /dev/null -w "%{http_code}" \
            -X "$method" \
            -H "Content-Type: application/json" \
            -d "$body" \
            --max-time "$TIMEOUT" \
            "$url" 2>/dev/null) || response="000"
    else
        response=$(curl -sf -o /dev/null -w "%{http_code}" \
            -X "$method" \
            --max-time "$TIMEOUT" \
            "$url" 2>/dev/null) || response="000"
    fi

    if [ "$response" = "$expected_code" ]; then
        log_pass "$description (HTTP $response)"
    else
        log_fail "$description (expected $expected_code, got $response)"
    fi
}

echo "=========================================="
echo "  Smoke Tests — ${BASE_URL}"
echo "=========================================="
echo ""

# ── Layer 1: Infrastructure Health ──
echo "━━ Infrastructure ━━"

run_test "App health endpoint" "GET" "/actuator/health" "200"
run_test "App info endpoint" "GET" "/actuator/info" "200"
run_test "Metrics endpoint" "GET" "/actuator/metrics" "200"
run_test "Prometheus metrics" "GET" "/actuator/prometheus" "200"

echo ""
echo "━━ Core API (Public Endpoints) ━━"

# Public endpoints that should always work
run_test "Auth hello endpoint" "GET" "/api/v1/auth/hello" "200"
run_test "List providers" "GET" "/api/v1/providers" "200"
run_test "Provider categories" "GET" "/api/v1/providers/categories" "200"

echo ""
echo "━━ Database Connectivity ━━"

# If we can list providers, DB is connected (already tested above)
# Additional DB-dependent endpoints
run_test "Search providers by category" "GET" "/api/v1/providers/search?category=test" "200"

echo ""
echo "━━ Redis Connectivity ━━"

# Rate limiter uses Redis — if it's up, rate limiting works
# A 429 means Redis IS working (rate limited), 200 also means it's working
rate_response=$(curl -sf -o /dev/null -w "%{http_code}" \
    --max-time "$TIMEOUT" \
    "${BASE_URL}/api/v1/providers" 2>/dev/null) || rate_response="000"
TOTAL=$((TOTAL + 1))
if [ "$rate_response" = "200" ] || [ "$rate_response" = "429" ]; then
    log_pass "Redis rate limiter responding (HTTP $rate_response)"
else
    log_fail "Redis rate limiter not responding (got $rate_response)"
fi

echo ""
echo "=========================================="
echo "  Results: ${PASS} passed, ${FAIL} failed, ${TOTAL} total"
echo "=========================================="

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo -e "${RED}SMOKE TESTS FAILED — ${FAIL} endpoint(s) not responding correctly${NC}"
    echo "Deploy may be broken. Check logs: docker logs noba-backend"
    exit 1
else
    echo ""
    echo -e "${GREEN}ALL SMOKE TESTS PASSED${NC}"
    exit 0
fi
