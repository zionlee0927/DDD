# Epic 01: Statement BC

## 목표
커미션 확정 후 파트너별 정산 명세서를 생성하고, 주기 마감 시 확정하는 흐름 구현.

## 배경

**Aggregate**: Statement (Root) + StatementLineItem (내부 Entity) + DeductionLineItem (내부 Entity) + SettlementPeriod (VO)

**상태 머신**: DRAFT → FINALIZED

**불변식**:
- S1: netAmount = totalAmount - deductionAmount
- S2: totalAmount = StatementLineItem 합계
- S3: deductionAmount = DeductionLineItem 합계
- S4: FINALIZED 후 변경 불가
- S5: 동일 파트너 + 동일 기간 Statement 최대 1개

**이벤트 흐름**:
```
CommissionConfirmed → [정책] 정산 항목 추가
스케줄러 → 정산 주기 마감 → [정책] 명세서 확정
StatementFinalized → [정책] 웹훅 발송 (Notification BC)
```

**BC 의존**:
- events: commission (CommissionConfirmed 구독)
- cs: tenant (정산 주기 설정 조회)

## 스토리

| ID | 스토리 | 상태 |
|----|--------|------|
| S01 | [커미션 확정 시 정산 항목이 추가된다](./story-01-add-item.md) | ⬜ |
| S02 | [차감 발생 시 차감 항목이 추가된다](./story-02-add-deduction.md) | ⬜ |
| S03 | [정산 주기를 마감하여 명세서를 확정한다](./story-03-finalize.md) | ⬜ |
| S04 | [파트너가 자신의 명세서를 조회한다](./story-04-query.md) | ⬜ |

## 완료 조건 (DoD)
- [ ] Statement aggregate 단위 테스트 (불변식 S1~S5 검증)
- [ ] 통합 테스트 (Commission 확정 → Statement 항목 추가 흐름)
- [ ] ArchUnit 통과
- [ ] E2E 시나리오 추가
