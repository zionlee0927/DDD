#!/bin/bash
# 중복 전환 → 409
source "$(dirname "$0")/e2e-helpers.sh"

echo "=== e2e-reject-duplicate: 중복 전환 거부 ==="

CLICK_ID=$(setup_click)
EXT_ID="e2e-dup-$(date +%s)"
do_conversion "$EXT_ID" "\"$CLICK_ID\"" > /dev/null

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/attributions/conversions" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\":\"$TENANT_ID\",\"externalId\":\"$EXT_ID\",\"amount\":50000,\"eventType\":\"PAYMENT\",\"clickId\":\"$CLICK_ID\",\"referralCode\":null}")
assert_eq "$HTTP_STATUS" "409" "중복 거부 (409)"

print_result
