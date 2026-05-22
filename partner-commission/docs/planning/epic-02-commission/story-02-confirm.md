# S01: 커미션을 확정한다

## 유즈케이스
고객사 API 호출 또는 14일 경과 시 PENDING 커미션을 CONFIRMED로 전환하고 CommissionConfirmed 이벤트를 발행한다.

## AC

- Given PENDING 상태의 커미션이 존재할 때
- When 고객사가 확정 API를 호출하면
- Then 상태가 CONFIRMED로 전환된다
- And CommissionConfirmed 이벤트가 발행된다

- Given PENDING 상태에서 14일이 경과했을 때
- When 스케줄러가 실행되면
- Then 자동으로 CONFIRMED 전환된다

- Given CONFIRMED 상태일 때
- When 확정을 시도하면
- Then 예외가 발생한다

## Task
- T01: Commission.confirm() + CommissionConfirmed 이벤트
- T02: ConfirmCommissionUseCase + API
- T03: (선택) 스케줄러 — 14일 경과 자동 확정
- T04: 단위 테스트 + 통합 테스트
