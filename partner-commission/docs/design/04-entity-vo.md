# 04. Entity / Value Object 도출

> "가능하면 Entity보다 Value Object를 선호하라." — Vaughn Vernon

---

## 식별 과정

### 입력: 1단계 용어집의 명사

```
Partner, PartnerAccount, Membership, Tier, TrackingLink, TrackingCode,
CouponCode, Click, ConversionEvent, CancellationEvent, AttributionDecision,
AttributionWindow, AttributionStrategy, AttributionEvidence, Commission,
CommissionRule, Deduction, Statement, StatementLineItem, DeductionLineItem,
Tenant, ApiKey, Webhook, ProgramConfig, SettlementPeriod, Money ...
```

### 판단 흐름

```
명사를 하나 꺼냄
  → "이걸 추적해야 하나?" (이력, 상태 변화, 생명주기)
     → Yes → Entity
              → "외부에서 직접 접근해야 하나?"
                 → Yes → Aggregate Root (Repository 가짐)
                 → No  → 내부 Entity (Root 통해서만 접근)
     → No  → "값이 같으면 같은 건가?"
              → Yes → Value Object
```

### 판단 기준

| 질문 | Entity | VO |
|------|--------|----|
| 고유 ID로 구분해야 하나? | ✓ | ✗ |
| 시간에 따라 상태가 변하나? | ✓ | ✗ |
| 생성/변경/삭제 생명주기가 있나? | ✓ | ✗ |
| 속성이 전부 같으면 같은 건가? | ✗ | ✓ |
| 변경 시 새 객체로 교체해도 되나? | ✗ | ✓ |

### Root vs 내부 Entity 구분 기준

| 질문 | Root | 내부 Entity |
|------|------|-------------|
| 외부에서 ID로 직접 조회하나? | ✓ | ✗ |
| 독립적으로 생성/삭제 가능한가? | ✓ | ✗ (Root와 함께) |
| Repository가 필요한가? | ✓ | ✗ |
| 다른 Aggregate가 ID로 참조하나? | ✓ | ✗ |

---

## 분류 결과

## Attribution BC

| 구분 | 이름 | 속성 | 이유 |
|------|------|------|------|
| Entity (Root) | AttributionDecision | id, tenantId, conversionEventId, partnerId, evidenceType, evidenceId, strategy, status, decidedAt | 상태 변화 (ATTRIBUTED → REVOKED). 커미션이 참조 |
| Entity (Root) | ConversionEvent | id, tenantId, externalId, amount, eventType, evidenceId, evidenceType, metadata, receivedAt | 고객사가 보낸 전환 기록. 독립 조회 필요 |
| VO | AttributionWindow | duration | 불변, 설정값 |
| VO | AttributionEvidence | type (CLICK/REFERRAL_CODE), referenceId | 불변, 귀속 증거 래핑 |
| enum | AttributionStrategy | LAST_CLICK, FIRST_CLICK | 귀속 전략 |
| enum | AttributionStatus | ATTRIBUTED, REVOKED, UNATTRIBUTED | 판정 상태 |
| enum | EvidenceType | CLICK, REFERRAL_CODE | 증거 유형 |

## Commission BC

| 구분 | 이름 | 속성 | 이유 |
|------|------|------|------|
| Entity (Root) | Commission | id, tenantId, attributionDecisionId, partnerId, amount, rule, status, calculatedAt, confirmedAt | 상태 변화 (PENDING → CONFIRMED → SETTLED / CANCELLED) |
| Entity (Root) | Deduction | id, tenantId, partnerId, originalCommissionId, amount, reason, createdAt | 확정 후 취소 시 생성. 명세서가 참조 |
| VO | CommissionRule | type (PERCENTAGE/FIXED), rate, fixedAmount | 불변, 산정 시 스냅샷 |
| VO | Money | amount, currency | 불변, 값으로 비교 |
| enum | CommissionStatus | PENDING, CONFIRMED, CANCELLED, SETTLED | 커미션 상태 |
| enum | RuleType | PERCENTAGE, FIXED | 규칙 유형 |

## Tracking BC

| 구분 | 이름 | 속성 | 이유 |
|------|------|------|------|
| Entity (Root) | TrackingLink | id, tenantId, partnerId, targetUrl, trackingCode, createdAt, expiresAt | 클릭 기반 귀속 수단. 생명주기 (생성 → 만료) |
| Entity (Root) | Click | id, tenantId, trackingCode, ipAddress, userAgent, clickedAt | 개별 클릭 추적. Attribution이 ID로 참조 |
| Entity (Root) | ReferralCode | id, tenantId, partnerId, code, createdAt, expiresAt | 코드 기반 귀속 수단. 생명주기 (생성 → 만료) |
| VO | TrackingCode | value | 불변, 값으로 비교 |

## Partner BC

| 구분 | 이름 | 속성 | 이유 |
|------|------|------|------|
| Entity (Root) | Partner | id, name, email, status, createdAt | 글로벌 계정. 생명주기 (가입 → 활성 → 비활성) |
| Entity (Root) | Membership | id, partnerId, tenantId, tier, status, joinedAt | 파트너 × 테넌트 소속 관계. 독립 생명주기 |
| VO | Tier | level, commissionRate | 불변, 등급 값 |
| enum | PartnerStatus | ACTIVE, INACTIVE | 계정 상태 |
| enum | MembershipStatus | ACTIVE, SUSPENDED | 소속 상태 |

## Statement BC

| 구분 | 이름 | 속성 | 이유 |
|------|------|------|------|
| Entity (Root) | Statement | id, tenantId, partnerId, period, totalAmount, deductionAmount, netAmount, status, createdAt | 정산 명세. 상태 변화 (DRAFT → FINALIZED) |
| Entity (내부) | StatementLineItem | id, commissionId, amount, description | 명세 내 개별 커미션 건 |
| Entity (내부) | DeductionLineItem | id, deductionId, amount, description | 명세 내 차감 건 |
| VO | SettlementPeriod | startDate, endDate | 불변, 기간 값 |
| VO | Money | amount, currency | 불변 |
| enum | StatementStatus | DRAFT, FINALIZED | 명세 상태 |

## Tenant BC

| 구분 | 이름 | 속성 | 이유 |
|------|------|------|------|
| Entity (Root) | Tenant | id, name, status, createdAt | 고객사. 생명주기 (등록 → 활성 → 비활성) |
| Entity (Root) | ApiKey | id, tenantId, key, status, createdAt, expiresAt | 독립 생명주기 (발급 → 폐기). 인증 시 직접 조회 |
| VO | WebhookConfig | url, secret, events | 불변, 설정값 |
| VO | ProgramConfig | attributionWindow, attributionStrategy, conflictResolution, confirmationCondition, settlementPeriod | 불변, 설정 묶음 |
| enum | TenantStatus | ACTIVE, INACTIVE | 테넌트 상태 |

---

## 공통 VO (Shared Kernel 후보)

| VO | 사용하는 BC |
|----|-------------|
| Money (amount, currency) | Commission, Statement |
| TenantId | 모든 BC (멀티테넌트 식별) |
| PartnerId | Attribution, Commission, Statement, Tracking |

---

## 판단 근거 요약

| 판단 | 기준 |
|------|------|
| ReferralCode → Entity (Root) | 코드 기반 귀속 수단. Attribution이 참조 |
| TrackingLink → Entity (Root) | 클릭 기반 귀속 수단. 클릭 시 직접 조회 필요 |
| ReferralCode → Entity (Root) | 코드 기반 귀속 수단. Attribution이 참조 |
| TrackingLink → Entity (Root) | 클릭 기반 귀속 수단. 클릭 시 직접 조회 필요 |
| Click → Entity (Root) | Attribution이 ID로 참조. 독립 조회 필요 |
| ConversionEvent → Entity (Root) | 고객사가 보낸 원본 기록. Attribution이 참조 |
| Deduction → Entity (Root) | Statement가 ID로 참조. 독립 생명주기 |
| Membership → Entity (Root) | 파트너 × 테넌트 관계. Partner와 별도 생명주기 |
| CommissionRule → VO | 산정 시점에 스냅샷. 이후 규칙 변경 영향 없음 |
| ProgramConfig → VO | 설정 묶음. 변경 시 새 객체로 교체 |
| StatementLineItem → 내부 Entity | Statement 없이 의미 없음. Root 통해서만 접근 |
