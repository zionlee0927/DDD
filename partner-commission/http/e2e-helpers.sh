#!/bin/bash
# 공통 헬퍼
BASE_URL="http://localhost:8080"
TENANT_ID="a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
PARTNER_ID="c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
PASS=0; FAIL=0

assert_eq() {
  if [ "$1" = "$2" ]; then echo "  ✅ $3"; PASS=$((PASS+1))
  else echo "  ❌ $3 (expected: $2, got: $1)"; FAIL=$((FAIL+1)); fi
}

# 링크 발급 → 클릭 → clickId 반환
setup_click() {
  local LINK_RESP=$(curl -s -X POST "$BASE_URL/api/tracking-links" \
    -H "Content-Type: application/json" \
    -d "{\"tenantId\":\"$TENANT_ID\",\"partnerId\":\"$PARTNER_ID\",\"targetUrl\":\"https://shop.com/p/1\"}")
  local CODE=$(echo "$LINK_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['trackingCode'])" 2>/dev/null)
  curl -s -o /dev/null "$BASE_URL/api/redirect/$CODE"
  sleep 0.5
  docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
    "SELECT id FROM clicks WHERE tracking_code='$CODE' ORDER BY clicked_at DESC LIMIT 1;" | tr -d ' \n'
}

# 전환 수신 → AttributionDecision JSON 반환
do_conversion() {
  local EXT_ID=$1; local CLICK_ID=$2; local REF_CODE=$3; local AMOUNT=${4:-50000}
  curl -s -X POST "$BASE_URL/api/attributions/conversions" \
    -H "Content-Type: application/json" \
    -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"$EXT_ID\",\"amount\":$AMOUNT,\"eventType\":\"PAYMENT\",\"clickId\":${CLICK_ID:-null},\"referralCode\":${REF_CODE:-null}}"
}

# 철회
do_revoke() {
  local EXT_ID=$1
  curl -s -X POST "$BASE_URL/api/attributions/revocations" \
    -H "Content-Type: application/json" \
    -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"$EXT_ID\"}"
}

print_result() {
  echo ""; echo "=== 결과: $PASS passed, $FAIL failed ==="
  [ $FAIL -eq 0 ] && exit 0 || exit 1
}
