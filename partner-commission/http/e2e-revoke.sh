#!/bin/bash
# 기한 내 철회 → REVOKED
source "$(dirname "$0")/e2e-helpers.sh"

echo "=== e2e-revoke: 귀속 철회 ==="

CLICK_ID=$(setup_click)
EXT_ID="e2e-revoke-$(date +%s)"
do_conversion "$EXT_ID" "\"$CLICK_ID\"" > /dev/null

RESP=$(do_revoke "$EXT_ID")
STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
assert_eq "$STATUS" "REVOKED" "철회 성공"

print_result
