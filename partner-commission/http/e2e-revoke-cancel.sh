#!/bin/bash
# 귀속 철회 → 커미션 취소 (PENDING → CANCELLED)
source "$(dirname "$0")/e2e-helpers.sh"

echo "=== e2e-revoke-cancel: 철회 시 커미션 취소 ==="

CLICK_ID=$(setup_click)
EXT_ID="e2e-revoke-cancel-$(date +%s)"
ATTR_RESP=$(do_conversion "$EXT_ID" "\"$CLICK_ID\"")
DECISION_ID=$(echo "$ATTR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

sleep 1
# 커미션 PENDING 확인
COMM_STATUS=$(docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
  "SELECT status FROM commissions WHERE attribution_decision_id='$DECISION_ID';" | tr -d ' \n')
assert_eq "$COMM_STATUS" "PENDING" "커미션 PENDING"

# 철회
do_revoke "$EXT_ID" > /dev/null

sleep 1
# 커미션 CANCELLED 확인
COMM_STATUS2=$(docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
  "SELECT status FROM commissions WHERE attribution_decision_id='$DECISION_ID';" | tr -d ' \n')
assert_eq "$COMM_STATUS2" "CANCELLED" "커미션 CANCELLED"

print_result
