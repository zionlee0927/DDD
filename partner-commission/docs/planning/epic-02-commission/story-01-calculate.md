# S01: 귀속 판정 시 커미션을 산정한다 ✅

## 유즈케이스
AttributionDecided 이벤트를 수신하여 테넌트 커미션 규칙에 따라 커미션을 산정(PENDING)한다.

## AC

- Given 귀속 판정이 완료되었을 때
- When AttributionDecided 이벤트가 발행되면
- Then 커미션이 산정된다 (PENDING, 금액 = 전환금액 × 규칙)

## 완료
- Commission aggregate + CommissionCalculator
- AttributionEventListener → CalculateCommissionUseCase
- @Async + AFTER_COMMIT + REQUIRES_NEW
- E2E 검증 완료
