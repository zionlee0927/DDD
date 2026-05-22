#!/bin/bash
# 정상 귀속 + 커미션 PENDING
source "$(dirname "$0")/e2e-helpers.sh"

echo "=== e2e-attribute: 정상 귀속 흐름 ==="

CLICK_ID=$(setup_click)
assert_eq "$([ -n "$CLICK_ID" ] && echo ok)" "ok" "클릭 생성"

EXT_ID="e2e-attr-$(date +%s)"
RESP=$(do_conversion "$EXT_ID" "\"$CLICK_ID\"")
STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
assert_eq "$STATUS" "ATTRIBUTED" "귀속 성공"

sleep 1
DECISION_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
COMMISSION=$(docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
  "SELECT amount FROM commissions WHERE attribution_decision_id='$DECISION_ID';" | tr -d ' \n')
assert_eq "$COMMISSION" "5000.00" "커미션 PENDING (5000원)"

print_result
