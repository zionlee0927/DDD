# Epic 02: Commission BC — 산정/확정/취소/차감

## 목표
귀속 판정 후 커미션 산정, 확정, 철회 시 취소/차감까지 전체 커미션 생명주기 구현.

## 스토리

| ID | 스토리 | 상태 |
|----|--------|------|
| S01 | [귀속 판정 시 커미션을 산정한다](./story-01-calculate.md) | ✅ |
| S02 | [커미션을 확정한다](./story-02-confirm.md) | ⬜ |
| S03 | [귀속 철회 시 커미션을 취소한다](./story-03-cancel.md) | ⬜ |
| S04 | [귀속 철회 시 차감을 생성한다](./story-04-deduction.md) | ⬜ |

## 완료 조건 (DoD)
- [x] Commission 산정 동작 (AttributionDecided → PENDING)
- [ ] Commission 상태 전이 단위 테스트
- [ ] Deduction aggregate 단위 테스트
- [ ] 통합 테스트 (철회 → 취소/차감 분기)
- [ ] E2E 시나리오 추가
