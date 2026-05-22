# S01: 전환 수신 → 귀속 판정 ✅

## 유즈케이스
고객사가 전환 이벤트를 전송하면, 클릭/추천코드 증거를 기반으로 귀속 판정하고 AttributionDecided 이벤트를 발행한다.

## AC

- Given 유효한 클릭이 존재할 때
- When 전환 이벤트가 수신되면
- Then ATTRIBUTED + 파트너 매칭

- Given 유효한 추천코드가 존재할 때
- When 전환 이벤트가 수신되면
- Then ATTRIBUTED + 파트너 매칭

- Given 증거가 없을 때
- Then UNATTRIBUTED

- Given 동일 externalId로 중복 전환
- Then 409 에러

## 완료
- AttributionDecision aggregate + ConversionEvent
- ReceiveConversionFacadeService + Processor 전략 패턴
- 단위 테스트 + 통합 테스트 + E2E
