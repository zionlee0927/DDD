# S01: 고객사가 귀속을 철회한다

## 유즈케이스
고객사가 전환 취소 API를 호출하면, 해당 전환의 귀속 판정을 철회(REVOKED)하고 AttributionRevoked 이벤트를 발행한다.

## AC

- Given ATTRIBUTED 상태의 귀속 판정이 존재할 때
- When 고객사가 철회 API를 호출하면
- Then 상태가 REVOKED로 전환된다
- And AttributionRevoked 이벤트가 발행된다

- Given 이미 REVOKED 상태일 때
- When 철회를 시도하면
- Then 예외가 발생한다

## Task
- T01: AttributionDecision.revoke() + REVOKED 상태 + AttributionRevoked 이벤트
- T02: RevokeAttributionUseCase + API
- T03: 단위 테스트 + 통합 테스트
