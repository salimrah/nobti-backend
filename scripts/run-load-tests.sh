#!/usr/bin/env bash
set -euo pipefail

# ── noba-backend Load Test Runner ──
# Usage: ./scripts/run-load-tests.sh [base-url] [stages]
#   base-url  default: http://localhost:8080
#   stages    default: smoke (smoke|stress|soak)

BASE_URL="${1:-http://localhost:8080}"
STAGE="${2:-smoke}"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v k6 &> /dev/null; then
    echo "ERROR: k6 is not installed."
    echo "Install: brew install k6  (macOS) or visit https://k6.io/docs/get-started/installation/"
    exit 1
fi

echo "======================================"
echo " noba-backend Load Tests"
echo " Base URL: $BASE_URL"
echo " Stage:    $STAGE"
echo "======================================"

run_test() {
    local name="$1"
    local script="$2"
    local extra_opts="${3:-}"

    echo ""
    echo "── Running: $name ──"
    K6_STAGES="$STAGE" k6 run "$SCRIPT_DIR/load-tests/$script" \
        -e BASE_URL="$BASE_URL" \
        $extra_opts \
        --summary-trend-stats="min,med,avg,p(90),p(95),p(99),max" \
        --quiet=false
}

case "$STAGE" in
    smoke)
        echo "Mode: smoke test (1 iteration each)"
        run_test "OTP Rate Limiting" "otp-stress.js" "--iterations 5 --vus 1"
        run_test "Provider Search" "provider-search.js" "--iterations 10 --vus 1"
        run_test "Queue Operations" "queue-operations.js" "--iterations 5 --vus 1"
        ;;
    stress)
        echo "Mode: stress test (ramp-up stages)"
        run_test "OTP Rate Limiting" "otp-stress.js"
        run_test "Provider Search" "provider-search.js"
        run_test "Queue Operations" "queue-operations.js"
        ;;
    soak)
        echo "Mode: soak test (extended duration)"
        run_test "OTP Rate Limiting" "otp-stress.js" "--stage 5m:20 --stage 10m:20 --stage 5m:0"
        run_test "Provider Search" "provider-search.js" "--stage 5m:50 --stage 10m:50 --stage 5m:0"
        run_test "Queue Operations" "queue-operations.js" "--stage 5m:10 --stage 10m:10 --stage 5m:0"
        ;;
    *)
        echo "Unknown stage: $STAGE (use: smoke, stress, soak)"
        exit 1
        ;;
esac

echo ""
echo "======================================"
echo " All load tests completed!"
echo "======================================"
