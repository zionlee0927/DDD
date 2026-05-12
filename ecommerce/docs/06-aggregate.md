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

| 원칙                          | 설명                          |
|-----------------------------|-------------------------------|
| 작게 설계                      | 진정한 불변식만 포함                  |
| ID로 다른 Aggregate 참조         | 직접 객체 참조 금지                  |
| 하나의 트랜잭션 = 하나의 Aggregate   | 여러 Aggregate 수정 시 이벤트 사용     |
| 최종 일관성 수용                  | Aggregate 간은 비동기              |

### 큰 Aggregate 경고 신호

- 내부 Entity 3개 이상
- 무한 증가 컬렉션
- 잦은 Optimistic Lock 실패

---

## Aggregate 설계 결과

### Partner BC

```
[Partner Aggregate]
  Partner (Root)
  └── Tier (VO)

불변식: P1, P2, P3
```

단일 Root + VO. 단순한 구조.

### Tracking BC

```
[TrackingLink Aggregate]
  TrackingLink (Root)
  └── TrackingCode (VO)

[Click Aggregate]
  Click (Root)

[AttributionDecision Aggregate]
  AttributionDecision (Root)
  └── AttributionWindow (VO)

불변식: T1~T5
```

3개 Aggregate로 분리. 이유:
- Click은 AttributionDecision이 참조 → 독립 Root
- AttributionDecision은 Commission이 참조 → 독립 Root
- 각각 독립적 생명주기

### Commission BC

```
[Commission Aggregate]
  Commission (Root)
  └── CommissionRule (VO)
  └── Money (VO)

[Settlement Aggregate]
  Settlement (Root)
  └── Money (VO)

불변식: C1~C6
```

2개 Aggregate로 분리. 이유:
- Commission: 개별 커미션 건. 산정→확정→취소 생명주기
- Settlement: 정산 묶음. 기간/파트너 기준으로 Commission을 조회하여 집계
- Settlement는 Commission ID 목록을 직접 들고 있지 않음 (무한 증가 방지)
- Settlement는 집계 결과(totalAmount, count, period)만 보유

### Order BC

```
[Order Aggregate]
  Order (Root)
  └── OrderItem (내부 Entity)
  └── Money (VO)
  └── Address (VO)

불변식: O1~O10
```

Order + OrderItem이 하나의 Aggregate. 이유:
- O2: totalAmount = items 합계 → 함께 변경 필수
- O10: 모든 item 취소 시 주문 전체 취소 → Root가 판단
- O9: 상품 중복 체크 → Root가 책임

### Cart BC

```
[Cart Aggregate]
  Cart (Root)
  └── CartItem (내부 Entity)
  └── Money (VO)

불변식: CT1~CT4
```

Cart + CartItem이 하나의 Aggregate. 이유:
- CT2: 같은 상품 중복 방지 → Root가 체크
- CT1: 수량 0이면 제거 → Root가 관리

### Catalog BC

```
[Product Aggregate]
  Product (Root)
  └── Money (VO)
  └── Category (VO)

불변식: CA1~CA3
```

단일 Root + VO.

### Inventory BC

```
[Stock Aggregate]
  Stock (Root)
  └── Quantity (VO)

불변식: I1~I5
```

단일 Root + VO. 동시성 제어의 핵심 단위.
- I1: availableQuantity 정합성 → Root 내부에서 계산
- I4: 선점 시 가용 재고 체크 → Root가 검증

### Payment BC

```
[Payment Aggregate]
  Payment (Root)
  └── Money (VO)

불변식: PM1~PM6
```

단일 Root + VO.

### Shipping BC

```
[Shipment Aggregate]
  Shipment (Root)
  └── TrackingNumber (VO)
  └── Address (VO)

불변식: S1~S4
```

단일 Root + VO.

### Promotion BC

```
[Coupon Aggregate]
  Coupon (Root)
  └── DiscountRule (VO)
  └── CouponCode (VO)

불변식: PR1~PR5
```

단일 Root + VO.

---

## 전체 Aggregate 목록

| BC         | Aggregate             | 구성                        | 내부 Entity |
|------------|----------------------|---------------------------|-----------|
| Partner    | Partner              | Root + Tier(VO)            | 없음        |
| Tracking   | TrackingLink         | Root + TrackingCode(VO)    | 없음        |
| Tracking   | Click                | Root                       | 없음        |
| Tracking   | AttributionDecision  | Root + AttributionWindow(VO) | 없음      |
| Commission | Commission           | Root + CommissionRule(VO)  | 없음        |
| Commission | Settlement           | Root + Money(VO)           | 없음        |
| Order      | Order                | Root + OrderItem(Entity)   | OrderItem |
| Cart       | Cart                 | Root + CartItem(Entity)    | CartItem  |
| Catalog    | Product              | Root + Category(VO)        | 없음        |
| Inventory  | Stock                | Root                       | 없음        |
| Payment    | Payment              | Root                       | 없음        |
| Shipping   | Shipment             | Root + TrackingNumber(VO)  | 없음        |
| Promotion  | Coupon               | Root + DiscountRule(VO)    | 없음        |

---

## Aggregate 간 참조 (ID만)

```
Order.customerId        → (외부: 회원)
Order.items[].productId → (외부: Catalog.Product)
Payment.orderId         → (외부: Order)
Shipment.orderId        → (외부: Order)
Stock.productId         → (외부: Catalog.Product)
TrackingLink.partnerId  → (외부: Partner)
TrackingLink.productId  → (외부: Catalog.Product)
Click.trackingCode      → (외부: TrackingLink)
AttributionDecision.orderId   → (외부: Order)
AttributionDecision.partnerId → (외부: Partner)
AttributionDecision.clickId   → (외부: Click)
Commission.attributionDecisionId → (외부: Tracking.AttributionDecision)
Commission.partnerId    → (스냅샷: Partner)
Commission.orderId      → (스냅샷: Order)
Settlement.partnerId    → (외부: Partner)
Settlement.period       → 정산 기간 (VO). Commission은 기간+partnerId로 조회
```
