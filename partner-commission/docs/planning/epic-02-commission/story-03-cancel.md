# S02: 귀속 철회 시 커미션을 취소한다

## 유즈케이스
AttributionRevoked 이벤트를 수신했을 때, 해당 커미션이 PENDING이면 CANCELLED로 전환한다.

## AC

- Given PENDING 상태의 커미션이 존재할 때
- When AttributionRevoked 이벤트가 발행되면
- Then 커미션이 CANCELLED로 전환된다
- And CommissionCancelled 이벤트가 발행된다

## Task
- T01: Commission.cancel() + CommissionCancelled 이벤트
- T02: AttributionRevokedListener에 취소 분기 추가
- T03: 단위 테스트
