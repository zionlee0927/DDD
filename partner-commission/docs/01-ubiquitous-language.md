# 01. 유비쿼터스 언어 / 용어집

> "유비쿼터스 언어는 Bounded Context 내에서만 유효하다. 이 용어가 코드에 그대로 반영된다."

---

## 귀속 (Attribution) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 귀속 | Attribution | 특정 전환이 어떤 파트너의 추천 덕분인지 판정하는 것 |
| 귀속 판정 | AttributionDecision | 전환 이벤트에 대해 파트너를 매칭한 결과 |
| 귀속 윈도우 | AttributionWindow | 클릭/쿠폰 사용 후 전환까지 인정하는 기간 (기본 30일) |
| 귀속 전략 | AttributionStrategy | 여러 클릭 충돌 시 해소 방식 (LAST_CLICK, FIRST_CLICK) |
| 전환 이벤트 | ConversionEvent | 고객사가 전송하는 "전환 발생" 신호 (주문, 방문 등) |
| 취소 이벤트 | CancellationEvent | 고객사가 전송하는 "전환 취소" 신호 |
| 클릭 | Click | 소비자가 추적 링크를 클릭한 기록 |
| 귀속 증거 | AttributionEvidence | 귀속을 연결하는 수단 (click_id 또는 referral_code) |
| 충돌 해소 | ConflictResolution | 클릭과 추천 코드가 동시에 있을 때 우선순위 결정 |

## 커미션 (Commission) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 커미션 | Commission | 파트너가 받을 추천 수수료 |
| 커미션 규칙 | CommissionRule | 커미션 계산 방식 (정률, 정액, 티어별) |
| 커미션 산정 | CommissionCalculation | 귀속 판정 후 규칙에 따라 금액을 계산하는 행위 |
| 커미션 확정 | CommissionConfirmation | PENDING → CONFIRMED 전이. 확정 이벤트 수신 또는 14일 경과 |
| 커미션 취소 | CommissionCancellation | 전환 취소로 인해 커미션을 무효화하는 행위 |
| 차감 | Deduction | 이미 확정된 커미션이 취소될 때 다음 정산에서 빼는 것 |
| 커미션 상태 | CommissionStatus | PENDING, CONFIRMED, CANCELLED, SETTLED |
| 확정 조건 | ConfirmationCondition | 커미션이 확정되는 기준 (이벤트 기반 or 기간 경과) |
| 규칙 우선순위 | RulePriority | 여러 규칙 충돌 시 적용 순서 |

## 파트너 (Partner) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 파트너 | Partner | 추천 활동을 하고 커미션을 받는 사람 |
| 파트너 계정 | PartnerAccount | 우리 플랫폼의 글로벌 계정 |
| 소속 | Membership | 파트너가 특정 테넌트의 프로그램에 참여하는 관계 |
| 티어 | Tier | 테넌트별 파트너 등급 (실적에 따라 커미션율 차등) |

## 추적 (Tracking) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 추적 링크 | TrackingLink | 파트너별 고유 URL. 클릭 시 우리 서버를 거쳐 리다이렉트 |
| 추적 코드 | TrackingCode | 추적 링크 URL에 포함된 고유 식별 코드 |
| 추천 코드 | ReferralCode | 파트너 전용 식별 코드. 오프라인/QR/직접 전달 등으로 사용 |
| 리다이렉트 | Redirect | 클릭 기록 후 고객사 URL로 전환하는 행위 |

## 테넌트 (Tenant) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 테넌트 | Tenant | 우리 SaaS를 사용하는 고객사 |
| API 키 | ApiKey | 테넌트 인증용 키 |
| 웹훅 | Webhook | 이벤트 발생 시 고객사에게 HTTP 콜백 |
| 프로그램 설정 | ProgramConfig | 테넌트별 귀속/커미션/정산 규칙 묶음 |

## 정산 명세 (Statement) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 정산 명세 | Statement | 정산 주기 동안 확정된 커미션을 파트너별로 집계한 문서 |
| 정산 주기 | SettlementPeriod | 명세서 생성 빈도 (주간, 월간 등) |
| 명세 항목 | StatementLineItem | 명세서 내 개별 커미션 건 |
| 차감 항목 | DeductionLineItem | 이전 정산 이후 취소된 건의 차감 내역 |

## 알림 (Notification) 영역

| 한국어 | 영문 (코드) | 정의 |
|--------|-------------|------|
| 웹훅 발송 | WebhookDelivery | 고객사에게 이벤트 결과를 HTTP로 전달 |
| 알림 | Notification | 파트너/고객사에게 보내는 이메일 등 |
