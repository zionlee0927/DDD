# S01: 커미션 확정 시 정산 항목이 추가된다

## 유즈케이스
CommissionConfirmed 이벤트를 수신하여 해당 파트너의 현재 DRAFT Statement에 항목을 추가한다. DRAFT가 없으면 새로 생성한다.

## AC

- Given 커미션이 확정되었을 때
- When CommissionConfirmed 이벤트가 발행되면
- Then 해당 파트너의 현재 기간 DRAFT Statement에 LineItem이 추가된다
- And totalAmount, netAmount가 재계산된다

- Given 해당 기간의 DRAFT Statement가 없을 때
- When CommissionConfirmed 이벤트가 발행되면
- Then 새 Statement(DRAFT)가 생성되고 LineItem이 추가된다

## Task
- T01: Statement aggregate + 내부 Entity + VO 설계/구현
- T02: CommissionConfirmedListener + AddStatementItemUseCase
- T03: Infrastructure (JPA + ACL)
- T04: 단위 테스트 + 통합 테스트
