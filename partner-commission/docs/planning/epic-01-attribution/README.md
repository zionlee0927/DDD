# Epic 01: Attribution BC — 귀속 판정/철회

## 목표
전환 이벤트를 수신하여 귀속 판정하고, 취소 시 철회하는 전체 흐름 구현.

## 스토리

| ID | 스토리 | 상태 |
|----|--------|------|
| S01 | [전환 수신 → 귀속 판정](./story-01-attribute.md) | ✅ |
| S02 | [고객사가 귀속을 철회한다](./story-02-revoke.md) | ✅ |

## 완료 조건 (DoD)
- [x] AttributionDecision 단위 테스트
- [x] 통합 테스트 (클릭/추천코드 축 기반)
- [x] ArchUnit 통과
- [x] E2E 시나리오
- [x] 철회 API + 테스트
