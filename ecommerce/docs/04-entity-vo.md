# 04. Entity / Value Object 도출

> "가능하면 Entity보다 Value Object를 선호하라." — Vaughn Vernon

---

## 식별 과정

### 입력: 1단계 용어집의 명사

```
Partner, Tier, TrackingLink, TrackingCode, Click, AttributionWindow,
Commission, CommissionRule, Settlement, Order, OrderItem, Product,
Cart, CartItem, Payment, Stock, Shipment, Coupon, DiscountRule,
Money, Address, Category, CouponCode, Quantity ...
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

| 질문                  | Entity | VO |
|---------------------|--------|----|
| 고유 ID로 구분해야 하나?     | ✓      | ✗  |
| 시간에 따라 상태가 변하나?     | ✓      | ✗  |
| 생성/변경/삭제 생명주기가 있나?  | ✓      | ✗  |
| 속성이 전부 같으면 같은 건가?   | ✗      | ✓  |
| 변경 시 새 객체로 교체해도 되나? | ✗      | ✓  |

### Root vs 내부 Entity 구분 기준

| 질문                         | Root | 내부 Entity   |
|----------------------------|------|-------------|
| 외부에서 ID로 직접 조회하나?       | ✓    | ✗           |
| 독립적으로 생성/삭제 가능한가?      | ✓    | ✗ (Root와 함께) |
| Repository가 필요한가?         | ✓    | ✗           |
| 다른 Aggregate가 ID로 참조하나? | ✓    | ✗           |
| Root 없이 존재 의미가 있나?      | ✓    | ✗           |

### 속성 도출 기준

- 2단계 이벤트에서 필요한 데이터
- 불변식을 지키기 위해 필요한 데이터
- 상태 전이를 표현하기 위한 status 필드
- 다른 Aggregate 참조를 위한 ID 필드

---

## 분류 결과

## Partner BC

| 구분            | 이름        | 속성                                       | 이유                           |
|---------------|-----------|------------------------------------------|------------------------------|
| Entity (Root) | Partner   | id, name, email, tier, status, createdAt | 생명주기 있음 (등록→활성→비활성), 추적 필요   |
| VO            | Tier      | level, commissionRate                    | 값으로 비교. BRONZE/SILVER/GOLD 등 |
| VO            | PartnerId | value                                    | 식별자 래핑, 타입 안전성               |

## Tracking BC

| 구분            | 이름                  | 속성                                                           | 이유                  |
|---------------|---------------------|--------------------------------------------------------------|---------------------|
| Entity (Root) | TrackingLink        | id, partnerId, productId, trackingCode, createdAt            | 고유 식별, 생명주기 (생성→만료) |
| Entity (Root) | AttributionDecision | id, orderId, partnerId, clickId, strategy, status, decidedAt | 귀속 판정 결과. 재판정/취소 가능 |
| Entity (Root) | Click               | id, trackingCode, visitorId, clickedAt, expired              | AttributionDecision이 참조. 독립 조회 필요 |
| VO            | TrackingCode        | value                                                        | 불변, 값으로 비교          |
| VO            | AttributionWindow   | duration (예: 30일)                                            | 불변, 설정값             |
| enum          | AttributionStrategy | LAST_CLICK, MULTI_TOUCH                                      | 귀속 판정 방식            |
| enum          | AttributionStatus   | PENDING, CONFIRMED, REVOKED                                  | 판정 상태               |

## Commission BC

| 구분            | 이름               | 속성                                                         | 이유                   |
|---------------|------------------|------------------------------------------------------------|----------------------|
| Entity (Root) | Commission       | id, attributionDecisionId, partnerId, orderId, amount, status, createdAt | 상태 변화 (산정→확정→지급/취소). partnerId/orderId는 스냅샷 |
| Entity (Root) | Settlement       | id, partnerId, commissions, totalAmount, status, settledAt | 정산 묶음. 상태 변화 (대기→완료) |
| VO            | CommissionRule   | type (정률/정액), rate, fixedAmount                            | 불변, 계산 규칙            |
| VO            | Money            | amount, currency                                           | 불변, 값으로 비교           |
| enum          | CommissionStatus | PENDING, CONFIRMED, SETTLED, CANCELLED                     |                      |
| enum          | SettlementStatus | PENDING, COMPLETED                                         |                      |

## Order BC

| 구분            | 이름              | 속성                                                                             | 이유                     |
|---------------|-----------------|--------------------------------------------------------------------------------|------------------------|
| Entity (Root) | Order           | id, customerId, items, status, totalAmount, partnerId, trackingCode, createdAt | 상태 전이 (생성→확정→배송→완료→취소) |
| Entity (내부)   | OrderItem       | id, productId, productName, price, quantity, status                            | 개별 취소 가능 → 상태 변화       |
| VO            | OrderId         | value                                                                          | 식별자 래핑                 |
| VO            | Money           | amount, currency                                                               | 불변                     |
| VO            | Address         | street, city, zipCode                                                          | 불변, 값으로 비교             |
| enum          | OrderStatus     | CREATED, PAID, SHIPPED, DELIVERED, CANCELLED, CANCEL_REQUESTED                 |                        |
| enum          | OrderItemStatus | ORDERED, CANCELLED                                                             |                        |
| enum          | CancelReason    | USER_REQUEST, PAYMENT_FAILURE, TIMEOUT                                         |                        |

## Cart BC

| 구분            | 이름       | 속성                                          | 이유                       |
|---------------|----------|---------------------------------------------|--------------------------|
| Entity (Root) | Cart     | id, customerId, items                       | 생명주기 (생성→상품 추가/제거→주문 전환) |
| Entity (내부)   | CartItem | id, productId, productName, price, quantity | 수량 변경, 개별 삭제 → 상태 변화     |
| VO            | Money    | amount, currency                            | 불변                       |

## Catalog BC

| 구분            | 이름        | 속성                                               | 이유                  |
|---------------|-----------|--------------------------------------------------|---------------------|
| Entity (Root) | Product   | id, name, description, price, categoryId, status | 생명주기 (등록→판매중→품절→삭제) |
| VO            | ProductId | value                                            | 식별자 래핑              |
| VO            | Money     | amount, currency                                 | 불변                  |
| VO            | Category  | id, name                                         | 값으로 비교              |

## Inventory BC

| 구분            | 이름        | 속성                                                            | 이유                  |
|---------------|-----------|---------------------------------------------------------------|---------------------|
| Entity (Root) | Stock     | id, productId, totalQuantity, heldQuantity, availableQuantity | 상태 변화 (선점/해제/차감/복원) |
| VO            | ProductId | value                                                         | 식별자 래핑              |
| VO            | Quantity  | value                                                         | 불변, 자가 검증 (≥ 0)     |

## Payment BC

| 구분            | 이름            | 속성                                             | 이유                  |
|---------------|---------------|------------------------------------------------|---------------------|
| Entity (Root) | Payment       | id, orderId, amount, method, status, createdAt | 상태 전이 (요청→완료→환불/실패) |
| VO            | Money         | amount, currency                               | 불변                  |
| VO            | PaymentId     | value                                          | 식별자 래핑              |
| enum          | PaymentStatus | PENDING, COMPLETED, FAILED, REFUNDED           |                     |
| enum          | PaymentMethod | CARD, BANK_TRANSFER                            |                     |

## Shipping BC

| 구분            | 이름             | 속성                                                                   | 이유                   |
|---------------|----------------|----------------------------------------------------------------------|----------------------|
| Entity (Root) | Shipment       | id, orderId, status, trackingNumber, carrier, shippedAt, deliveredAt | 상태 전이 (준비→출고→배송중→완료) |
| VO            | TrackingNumber | value                                                                | 불변                   |
| VO            | Address        | street, city, zipCode                                                | 불변                   |
| enum          | ShipmentStatus | PREPARING, SHIPPED, IN_TRANSIT, DELIVERED                            |                      |

## Promotion BC

| 구분            | 이름           | 속성                                                                        | 이유                 |
|---------------|--------------|---------------------------------------------------------------------------|--------------------|
| Entity (Root) | Coupon       | id, code, discountRule, validFrom, validTo, usageLimit, usedCount, status | 생명주기 (생성→활성→만료→소진) |
| VO            | DiscountRule | type (정률/정액), value, minOrderAmount                                       | 불변, 계산 규칙          |
| VO            | CouponCode   | value                                                                     | 불변                 |
| enum          | CouponStatus | ACTIVE, EXPIRED, EXHAUSTED                                                |                    |

---

## 공통 VO (Shared Kernel 후보)

| VO                          | 사용하는 BC                                              |
|-----------------------------|------------------------------------------------------|
| Money (amount, currency)    | Order, Cart, Commission, Payment, Catalog, Promotion |
| Address (street, city, zip) | Order, Shipping                                      |
| ProductId                   | Catalog, Cart, Order, Inventory, Tracking            |

---

## 판단 근거 요약

| 판단                  | 기준                       |
|---------------------|--------------------------|
| CartItem → Entity   | 수량 변경, 개별 삭제 등 자체 생명주기   |
| OrderItem → Entity  | 개별 취소 가능 → 상태 변화         |
| Click → Entity (Root) | 개별 클릭 추적 + AttributionDecision이 ID로 참조 → Root |
| Money → VO          | 불변, 값으로 비교, 연산 시 새 객체 반환 |
| Address → VO        | 불변, 값으로 비교               |
| TrackingCode → VO   | 불변, 그 자체로 상태 변화 없음       |
| CommissionRule → VO | 불변, 계산 규칙                |
| Tier → VO           | 불변, 등급 값                 |
