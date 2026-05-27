#!/bin/bash
# 확정 후 철회 → 차감(Deduction) 생성
source "$(dirname "$0")/e2e-helpers.sh"

echo "=== e2e-revoke-confirmed: 확정 후 철회 → 차감 ==="

CLICK_ID=$(setup_click)
EXT_ID="e2e-revoke-conf-$(date +%s)"
ATTR_RESP=$(do_conversion "$EXT_ID" "\"$CLICK_ID\"")
DECISION_ID=$(echo "$ATTR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

sleep 1
# 확정
curl -s -X POST "$BASE_URL/api/commissions/confirmations" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"attributionDecisionId\":\"$DECISION_ID\"}" > /dev/null
assert_eq "$?" "0" "커미션 확정"

# 철회
do_revoke "$EXT_ID" > /dev/null

sleep 1
# Deduction 생성 확인
DEDUCTION=$(docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
  "SELECT amount FROM deductions WHERE original_commission_id=(SELECT id FROM commissions WHERE attribution_decision_id='$DECISION_ID');" | tr -d ' \n')
assert_eq "$([ -n "$DEDUCTION" ] && echo ok)" "ok" "Deduction 생성됨 ($DEDUCTION)"

print_result
