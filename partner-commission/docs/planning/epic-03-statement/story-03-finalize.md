# S03: 정산 주기를 마감하여 명세서를 확정한다

## 유즈케이스
스케줄러(또는 수동 API)가 정산 주기 마감을 트리거하면, 해당 기간의 모든 DRAFT Statement를 FINALIZED로 전환한다.

## AC

- Given DRAFT 상태의 Statement가 존재할 때
- When 정산 주기 마감이 실행되면
- Then Statement가 FINALIZED로 전환된다
- And StatementFinalized 이벤트가 발행된다

- Given 이미 FINALIZED된 Statement에
- When 항목 추가를 시도하면
- Then 예외가 발생한다 (S4 불변식)

## Task
- T01: finalize() 도메인 로직 + StatementFinalized 이벤트
- T02: FinalizeStatementUseCase + API
- T03: 테스트
