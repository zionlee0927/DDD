#!/bin/bash
# Attribution 시나리오 E2E 테스트
# 사전 조건: 앱 + Docker(PostgreSQL) 실행 중

# 전체 시나리오: 링크 발급 → 클릭 → 전환 → 귀속 판정 + 커미션 산정

BASE_URL="http://localhost:8080"
TENANT_ID="a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
PARTNER_ID="c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
PASS=0
FAIL=0

assert_eq() {
  if [ "$1" = "$2" ]; then
    echo "  ✅ $3"
    PASS=$((PASS + 1))
  else
    echo "  ❌ $3 (expected: $2, got: $1)"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== 1. 추적 링크 발급 ==="
LINK_RESP=$(curl -s -X POST "$BASE_URL/api/tracking-links" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"partnerId\":\"$PARTNER_ID\",\"targetUrl\":\"https://shop.com/p/1\"}")
TRACKING_CODE=$(echo "$LINK_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['trackingCode'])" 2>/dev/null)
assert_eq "$([ -n "$TRACKING_CODE" ] && echo 'ok')" "ok" "trackingCode 발급"

echo "=== 2. 클릭 리다이렉트 ==="
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/redirect/$TRACKING_CODE")
assert_eq "$HTTP_STATUS" "302" "리다이렉트 302"

# clickId 조회
sleep 0.5
CLICK_ID=$(docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
  "SELECT id FROM clicks WHERE tracking_code='$TRACKING_CODE' ORDER BY clicked_at DESC LIMIT 1;" | tr -d ' \n')
assert_eq "$([ -n "$CLICK_ID" ] && echo 'ok')" "ok" "Click 저장됨"

echo "=== 3. 전환 수신 (클릭 기반) ==="
ATTR_RESP=$(curl -s -X POST "$BASE_URL/api/attributions/conversions" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"e2e-$(date +%s)\",\"amount\":50000,\"eventType\":\"PAYMENT\",\"clickId\":\"$CLICK_ID\",\"referralCode\":null}")
STATUS=$(echo "$ATTR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
DECISION_ID=$(echo "$ATTR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
assert_eq "$STATUS" "ATTRIBUTED" "귀속 성공"

echo "=== 4. 커미션 자동 산정 ==="
sleep 1
COMMISSION=$(docker exec -i $(docker ps -q --filter "expose=5432") psql -U partner_commission -d partner_commission -t -c \
  "SELECT amount FROM commissions WHERE attribution_decision_id='$DECISION_ID';" | tr -d ' \n')
assert_eq "$COMMISSION" "5000.00" "커미션 5000원 (10%)"

echo "=== 5. 중복 전환 거부 ==="
DUP_RESP=$(curl -s -X POST "$BASE_URL/api/attributions/conversions" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"e2e-dup-test\",\"amount\":50000,\"eventType\":\"PAYMENT\",\"clickId\":\"$CLICK_ID\",\"referralCode\":null}")
# 첫 번째 호출
curl -s -X POST "$BASE_URL/api/attributions/conversions" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"e2e-dup-test\",\"amount\":50000,\"eventType\":\"PAYMENT\",\"clickId\":\"$CLICK_ID\",\"referralCode\":null}" > /dev/null
# 두 번째 호출 (중복)
DUP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/attributions/conversions" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"e2e-dup-test\",\"amount\":50000,\"eventType\":\"PAYMENT\",\"clickId\":\"$CLICK_ID\",\"referralCode\":null}")
assert_eq "$DUP_STATUS" "500" "중복 거부"

echo "=== 6. 추천코드 기반 전환 ==="
REF_RESP=$(curl -s -X POST "$BASE_URL/api/attributions/conversions" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"e2e-ref-$(date +%s)\",\"amount\":30000,\"eventType\":\"PAYMENT\",\"clickId\":null,\"referralCode\":\"DEMO10\"}")
REF_STATUS=$(echo "$REF_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
assert_eq "$REF_STATUS" "ATTRIBUTED" "추천코드 귀속 성공"

echo ""
echo "=== 결과: $PASS passed, $FAIL failed ==="
[ $FAIL -eq 0 ] && exit 0 || exit 1
