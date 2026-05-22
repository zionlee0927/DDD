#!/bin/bash
# 커미션 확정: 귀속 → PENDING → 확정 API → CONFIRMED
source "$(dirname "$0")/e2e-helpers.sh"

echo "=== e2e-confirm: 커미션 확정 ==="

CLICK_ID=$(setup_click)
EXT_ID="e2e-confirm-$(date +%s)"
ATTR_RESP=$(do_conversion "$EXT_ID" "\"$CLICK_ID\"")
DECISION_ID=$(echo "$ATTR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
assert_eq "$([ -n "$DECISION_ID" ] && echo ok)" "ok" "귀속 성공"

sleep 1
# 확정 API
CONFIRM_RESP=$(curl -s -X POST "$BASE_URL/api/commissions/confirmations" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"attributionDecisionId\":\"$DECISION_ID\"}")
CONFIRM_STATUS=$(echo "$CONFIRM_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
assert_eq "$CONFIRM_STATUS" "CONFIRMED" "커미션 확정"

# 이미 확정된 건 다시 확정 시도 → 400
DUP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/commissions/confirmations" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"attributionDecisionId\":\"$DECISION_ID\"}")
assert_eq "$DUP_STATUS" "400" "이미 확정된 건 거부 (400)"

# 존재하지 않는 커미션 확정 → 404
NOT_FOUND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/commissions/confirmations" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"attributionDecisionId\":\"non-existent\"}")
assert_eq "$NOT_FOUND_STATUS" "404" "미존재 커미션 거부 (404)"

print_result
