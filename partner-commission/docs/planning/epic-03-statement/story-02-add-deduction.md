# S02: 차감 발생 시 차감 항목이 추가된다

## 유즈케이스
DeductionCreated 이벤트를 수신하여 해당 파트너의 현재 DRAFT Statement에 차감 항목을 추가한다.

## AC

- Given 차감이 생성되었을 때
- When DeductionCreated 이벤트가 발행되면
- Then DRAFT Statement에 DeductionLineItem이 추가된다
- And deductionAmount, netAmount가 재계산된다

## Task
- T01: DeductionLineItem 추가 로직
- T02: DeductionCreatedListener
- T03: 테스트
