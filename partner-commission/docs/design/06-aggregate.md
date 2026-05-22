# 06. Aggregate 설계

> "작은 Aggregate를 설계하라. 대부분은 Root + Value Object만으로 구성된다." — Vaughn Vernon

---

## 식별 과정

### 입력

- 4단계 Entity/VO (Root, 내부 Entity 구분)
- 5단계 불변식 (같은 트랜잭션에서 지켜야 할 규칙)

### 판단 기준

```
"이 불변식을 지키려면 어떤 객체들이 같은 트랜잭션에서 함께 변경되어야 하는가?"
→ 함께 변경되어야 하는 것 = 하나의 Aggregate
```

### Aggregate 크기 원칙

| 원칙 | 설명 |
|------|------|
| 작게 설계 | 진정한 불변식만 포함 |
| ID로 다른 Aggregate 참조 | 직접 객체 참조 금지 |
| 하나의 트랜잭션 = 하나의 Aggregate | 여러 Aggregate 수정 시 이벤트 사용 |
| 최종 일관성 수용 | Aggregate 간은 비동기 |

### 큰 Aggregate 경고 신호

- 내부 Entity 3개 이상
- 무한 증가 컬렉션
- 잦은 Optimistic Lock 실패

---

## Aggregate 설계 결과

### Attribution BC

```
[ConversionEvent Aggregate]
  ConversionEvent (Root)
  └── AttributionEvidence (VO)
  └── Money (VO)

[AttributionDecision Aggregate]
  AttributionDecision (Root)
  └── AttributionEvidence (VO)

불변식: A1~A6
```

2개 Aggregate로 분리. 이유:
- ConversionEvent: 고객사가 보낸 원본 기록. 수신 시 생성되고 이후 불변
- AttributionDecision: 판정 결과. 상태 전이 있음 (ATTRIBUTED → REVOKED)
- A1(하나의 전환에 판정 1개)은 AttributionDecision 생성 시 중복 체크로 보장

### Commission BC

```
[Commission Aggregate]
  Commission (Root)
  └── CommissionRule (VO)
  └── Money (VO)

[Deduction Aggregate]
  Deduction (Root)
  └── Money (VO)

불변식: C1~C9
```

2개 Aggregate로 분리. 이유:
- Commission: 개별 커미션 건. PENDING → CONFIRMED → SETTLED / CANCELLED 생명주기
- Deduction: 확정 후 취소 시 생성. Statement가 참조. 독립 생명주기
- C9(Deduction은 CONFIRMED/SETTLED에서만 생성)는 Application Service에서 Commission 상태 확인 후 Deduction 생성

### Tracking BC

```
[TrackingLink Aggregate]
  TrackingLink (Root)

[Click Aggregate]
  Click (Root)

[ReferralCode Aggregate]
  ReferralCode (Root)

불변식: T1~T4
```

3개 Aggregate. 모두 단일 Root. 이유:
- 각각 독립적 생명주기
- Click은 Attribution이 ID로 참조
- T1/T2(유일성)는 Repository 레벨에서 보장

### Partner BC

```
[Partner Aggregate]
  Partner (Root)

[Membership Aggregate]
  Membership (Root)
  └── Tier (VO)

불변식: P1~P4
```

2개 Aggregate로 분리. 이유:
- Partner: 글로벌 계정. 독립 생명주기
- Membership: 파트너 × 테넌트 소속. Partner와 별도로 생성/변경/삭제
- P2(동일 파트너+테넌트 중복 방지)는 Repository 레벨에서 보장

### Statement BC

```
[Statement Aggregate]
  Statement (Root)
  └── StatementLineItem (내부 Entity)
  └── DeductionLineItem (내부 Entity)
  └── SettlementPeriod (VO)
  └── Money (VO)

불변식: S1~S5
```

1개 Aggregate. 이유:
- S1/S2/S3: netAmount = totalAmount - deductionAmount, 각 합계 일치 → 함께 변경 필수
- S4: FINALIZED 후 변경 불가 → Root가 판단
- StatementLineItem/DeductionLineItem은 Statement 없이 의미 없음

### Tenant BC

```
[Tenant Aggregate]
  Tenant (Root)
  └── WebhookConfig (VO)
  └── ProgramConfig (VO)

[ApiKey Aggregate]
  ApiKey (Root)

불변식: TN1~TN4
```

2개 Aggregate로 분리. 이유:
- Tenant: 고객사 정보 + 설정. ProgramConfig/WebhookConfig는 VO로 함께 관리
- ApiKey: 독립 생명주기 (발급/폐기). 인증 시 직접 조회 필요
- TN3/TN4(설정값 범위)는 Tenant Root 내부에서 검증

---

## 전체 Aggregate 목록

| BC | Aggregate | 구성 | 내부 Entity |
|----|-----------|------|-------------|
| Attribution | ConversionEvent | Root + Evidence(VO) + Money(VO) | 없음 |
| Attribution | AttributionDecision | Root + Evidence(VO) | 없음 |
| Commission | Commission | Root + CommissionRule(VO) + Money(VO) | 없음 |
| Commission | Deduction | Root + Money(VO) | 없음 |
| Tracking | TrackingLink | Root | 없음 |
| Tracking | Click | Root | 없음 |
| Tracking | ReferralCode | Root | 없음 |
| Partner | Partner | Root | 없음 |
| Partner | Membership | Root + Tier(VO) | 없음 |
| Statement | Statement | Root + Period(VO) + Money(VO) | StatementLineItem, DeductionLineItem |
| Tenant | Tenant | Root + ProgramConfig(VO) + WebhookConfig(VO) | 없음 |
| Tenant | ApiKey | Root | 없음 |

---

## Aggregate 간 참조 (ID만)

```
AttributionDecision.conversionEventId  → ConversionEvent
AttributionDecision.partnerId          → Partner
Commission.attributionDecisionId       → AttributionDecision
Commission.tenantId                    → Tenant
Commission.partnerId                   → Partner
Deduction.originalCommissionId         → Commission
Deduction.partnerId                    → Partner
TrackingLink.partnerId                 → Partner
ReferralCode.partnerId                 → Partner
Click.trackingCode                     → TrackingLink (조회용)
Membership.partnerId                   → Partner
Membership.tenantId                    → Tenant
Statement.partnerId                    → Partner
Statement.tenantId                     → Tenant
StatementLineItem.commissionId         → Commission
DeductionLineItem.deductionId          → Deduction
ApiKey.tenantId                        → Tenant
```
