#!/bin/bash
# 전체 E2E 실행
DIR="$(dirname "$0")"
TOTAL_PASS=0; TOTAL_FAIL=0

for script in "$DIR"/e2e-attribute.sh "$DIR"/e2e-revoke.sh "$DIR"/e2e-reject-duplicate.sh; do
  echo ""
  bash "$script"
  [ $? -ne 0 ] && TOTAL_FAIL=$((TOTAL_FAIL+1)) || TOTAL_PASS=$((TOTAL_PASS+1))
done

echo ""
echo "=============================="
echo "전체: $TOTAL_PASS scripts passed, $TOTAL_FAIL failed"
[ $TOTAL_FAIL -eq 0 ] && exit 0 || exit 1
