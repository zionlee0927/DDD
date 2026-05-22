# 05. 불변식 (Invariant) 식별

> "불변식은 Aggregate 내부에서 어떤 상황에서든 항상 참이어야 하는 규칙이다. 이것이 Aggregate 경계를 결정한다."

---

## 식별 과정

### 입력

- 4단계 Entity/VO의 **속성**
- 2단계 이벤트의 **상태 전이**
- 2단계 커맨드의 **실행 조건**

### 불변식을 찾는 5가지 소스

| 소스 | 질문 | 예시 |
|------|------|------|
| Entity 속성 | 이 값에 허용 범위가 있는가? | 금액 ≥ 0, 윈도우 > 0 |
| Entity 간 관계 | 함께 지켜야 할 정합성이 있는가? | netAmount = totalAmount - deductionAmount |
| 상태 전이 | 어떤 상태에서 어떤 상태로만 갈 수 있는가? | PENDING → CONFIRMED만 가능 |
| 커맨드 실행 조건 | 이 행위가 실행되려면 뭐가 보장되어야 하는가? | 귀속 판정 시 윈도우 내 증거 존재 |
| Entity 간 경계 | 트랜잭션 범위에서 깨지면 안 되는 것은? | 명세서 netAmount = 항목 합계 |

### 판단 기준: 불변식 vs 정책

| 구분 | 불변식 (Invariant) | 정책 (Policy) |
|------|-------------------|---------------|
| 범위 | Aggregate **내부** | Aggregate **간** |
| 보장 | 같은 트랜잭션에서 항상 참 | 결과적 일관성 (비동기) |
| 위반 시 | 즉시 거부 (예외 발생) | 보상 트랜잭션으로 복구 |
| 예시 | 커미션 금액 > 0 | 귀속 판정 → 커미션 산정 |

---

## 분류 결과

## Attribution BC

| # | 불변식 | 설명 |
|---|--------|------|
| A1 | 하나의 ConversionEvent에 대해 AttributionDecision은 최대 1개 | 중복 판정 방지 |
| A2 | 귀속 증거(Click/Coupon)는 AttributionWindow 내에 있어야 한다 | 만료된 증거로 귀속 불가 |
| A3 | 상태 전이: ATTRIBUTED → REVOKED만 가능 | REVOKED에서 되돌릴 수 없음 |
| A4 | UNATTRIBUTED 상태에서는 상태 변경 불가 | 최종 상태 |
| A5 | ConversionEvent의 amount는 0 이상이어야 한다 | 음수 전환 금액 불가 |
| A6 | ConversionEvent의 evidenceId 또는 evidenceType 중 하나는 존재해야 한다 | 증거 없으면 수신은 되지만 UNATTRIBUTED |

## Commission BC

| # | 불변식 | 설명 |
|---|--------|------|
| C1 | 커미션 금액은 0보다 커야 한다 | 0원/음수 커미션 불가 |
| C2 | 커미션 금액 ≤ 전환 금액 | 전환 금액 초과 커미션 불가 |
| C3 | 상태 전이: PENDING → CONFIRMED 또는 PENDING → CANCELLED | PENDING에서만 분기 |
| C4 | 상태 전이: CONFIRMED → SETTLED만 가능 | 확정 후에는 정산만 가능 |
| C5 | CANCELLED 커미션은 상태 변경 불가 | 최종 상태 |
| C6 | SETTLED 커미션은 상태 변경 불가 | 최종 상태 (취소 시 Deduction으로 처리) |
| C7 | CommissionRule은 산정 시점에 스냅샷으로 고정 | 이후 규칙 변경 영향 없음 |
| C8 | Deduction의 amount는 0보다 커야 한다 | 0원 차감 불가 |
| C9 | Deduction은 CONFIRMED 또는 SETTLED 상태의 커미션에 대해서만 생성 가능 | PENDING/CANCELLED는 그냥 취소 |

## Tracking BC

| # | 불변식 | 설명 |
|---|--------|------|
| T1 | ReferralCode의 code는 테넌트 내에서 유일해야 한다 | 중복 코드 방지 |
| T2 | TrackingLink의 trackingCode는 테넌트 내에서 유일해야 한다 | 중복 코드 방지 |
| T3 | 만료된 TrackingLink로는 클릭이 기록되지 않는다 | 만료 체크 |
| T4 | 만료된 ReferralCode는 귀속 증거로 사용 불가 | 만료 체크 |

## Partner BC

| # | 불변식 | 설명 |
|---|--------|------|
| P1 | 파트너 이메일은 유일해야 한다 | 중복 가입 방지 |
| P2 | 동일 파트너 + 동일 테넌트의 Membership은 최대 1개 | 중복 소속 방지 |
| P3 | INACTIVE 파트너는 새로운 Membership을 생성할 수 없다 | 비활성 계정 제한 |
| P4 | SUSPENDED Membership의 파트너는 해당 테넌트에서 커미션 산정 대상이 아니다 | 정지된 소속 제한 |

## Statement BC

| # | 불변식 | 설명 |
|---|--------|------|
| S1 | Statement.netAmount = totalAmount - deductionAmount | 금액 정합성 |
| S2 | Statement.totalAmount = StatementLineItem 합계 | 항목 합계 일치 |
| S3 | Statement.deductionAmount = DeductionLineItem 합계 | 차감 합계 일치 |
| S4 | FINALIZED 상태의 Statement는 변경 불가 | 확정된 명세는 불변 |
| S5 | 동일 파트너 + 동일 기간의 Statement는 최대 1개 | 중복 명세 방지 |

## Tenant BC

| # | 불변식 | 설명 |
|---|--------|------|
| TN1 | API 키는 테넌트 내에서 유일해야 한다 | 중복 키 방지 |
| TN2 | INACTIVE 테넌트는 API 호출 불가 | 비활성 고객사 차단 |
| TN3 | AttributionWindow는 1일 이상이어야 한다 | 0일 윈도우 불가 |
| TN4 | SettlementPeriod의 startDate < endDate | 기간 역전 불가 |

---

## 상태 전이도

### AttributionDecision
```
ATTRIBUTED → REVOKED
UNATTRIBUTED (최종)
```

### Commission
```
PENDING → CONFIRMED → SETTLED
   ↓
CANCELLED (최종)
```

### Statement
```
DRAFT → FINALIZED (최종)
```
