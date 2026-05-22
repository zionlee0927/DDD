# S03: 귀속 철회 시 차감을 생성한다

## 유즈케이스
AttributionRevoked 이벤트를 수신했을 때, 해당 커미션이 CONFIRMED 또는 SETTLED이면 Deduction을 생성한다.

## AC

- Given CONFIRMED 상태의 커미션이 존재할 때
- When AttributionRevoked 이벤트가 발행되면
- Then Deduction이 생성된다
- And DeductionCreated 이벤트가 발행된다

- Given SETTLED 상태의 커미션이 존재할 때
- When AttributionRevoked 이벤트가 발행되면
- Then Deduction이 생성된다

## Task
- T01: Deduction aggregate 설계/구현
- T02: CreateDeductionUseCase
- T03: AttributionRevokedListener에 차감 분기 추가
- T04: 단위 테스트 + 통합 테스트
