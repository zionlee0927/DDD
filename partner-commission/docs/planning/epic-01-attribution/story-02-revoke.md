# S02: 고객사가 귀속을 철회한다

## 유즈케이스
고객사가 전환 취소 API를 호출하면, 해당 전환의 귀속 판정을 철회(REVOKED)하고 AttributionRevoked 이벤트를 발행한다.

## 정책
- 철회 기한: 테넌트 설정 `revocationWindowDays` (기본 30일)
- 귀속 판정일(`decidedAt`)로부터 기한 내에만 철회 가능
- 기한 초과 시 409 REVOCATION_WINDOW_EXPIRED

## AC

- Given ATTRIBUTED 상태의 귀속 판정이 존재하고 기한 내일 때
- When 고객사가 철회 API를 호출하면
- Then 상태가 REVOKED로 전환된다
- And AttributionRevoked 이벤트가 발행된다

- Given 철회 기한(revocationWindowDays)이 초과되었을 때
- When 철회를 시도하면
- Then 409 REVOCATION_WINDOW_EXPIRED 에러가 반환된다

- Given 이미 REVOKED 상태일 때
- When 철회를 시도하면
- Then 400 예외가 발생한다

- Given UNATTRIBUTED 상태일 때
- When 철회를 시도하면
- Then 400 예외가 발생한다

## Task
- T01: AttributionDecision.revoke(now, windowDays) 시그니처 변경 + 기한 검증
- T02: RevokeAttributionUseCase + RevokeAttributionService
- T03: API (POST /api/attributions/revocations)
- T04: Tenant ProgramConfig에 revocationWindowDays 추가
- T05: 단위 테스트 + 통합 테스트
