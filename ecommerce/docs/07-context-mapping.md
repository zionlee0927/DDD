# 07. Context Mapping

> "Context Map은 현재 상태를 있는 그대로 보여줘야 한다. 이상적인 상태가 아니라." — Eric Evans

---

## 식별 과정

### 입력

- 3단계 BC 목록 (10개)
- 2단계 정책 (이벤트 → 커맨드 연결 = BC 간 통신)
- 6단계 Aggregate 간 참조 (ID 참조 = 의존 방향)

### 판단 기준

| 관계의 성격              | 패턴          |
|----------------------|-------------|
| 여러 소비자에게 표준 API 제공 | OHS         |
| 내부 BC끼리, 동기 호출     | C-S         |
| 양방향 긴밀 협력          | Partnership |
| 비동기 이벤트 구독         | Events      |
| 외부 시스템 격리          | ACL         |

---

## Context Map

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           External Systems                               │
│  ┌──────────┐  ┌──────────┐                                            │
│  │   PG사   │  │ 외부 물류사│                                            │
│  │(토스,페이팔)│  │          │                                            │
│  └────┬─────┘  └────┬─────┘                                            │
│       │ACL          │ACL                                                │
└───────┼─────────────┼───────────────────────────────────────────────────┘
        │             │
        ▼             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Generic / Supporting                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Payment  │  │ Shipping │  │ Catalog  │  │Promotion │               │
│  │ Context  │  │ Context  │  │ Context  │  │ Context  │               │
│  │          │  │          │  │  [OHS]   │  │          │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────────┘
        │             │             │             │
        │Partnership  │Events       │OHS          │C-S
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Order Domain                                   │
│                                                                          │
│  ┌──────────┐     C-S      ┌──────────┐     C-S      ┌──────────┐     │
│  │   Cart   │─────────────→│  Order   │─────────────→│Inventory │     │
│  │ Context  │              │ Context  │              │ Context  │     │
│  └──────────┘              └────┬─────┘              └──────────┘     │
│       ↑                         │                         ↑            │
│       │ Events                  │ Events                  │ Events     │
│       └─────────────────────────┤                         │            │
│                                 │                    ┌────┴─────┐      │
│                                 │                    │ Shipping │      │
│                                 │                    └──────────┘      │
└─────────────────────────────────┼───────────────────────────────────────┘
                                  │
                                  │ Events
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Core Domain                                    │
│                                                                          │
│  ┌──────────┐     C-S      ┌──────────┐    Events    ┌──────────┐     │
│  │ Partner  │─────────────→│ Tracking │─────────────→│Commission│     │
│  │ Context  │              │ Context  │              │ Context  │     │
│  └──────────┘              └──────────┘              └────┬─────┘     │
│       ↑                                                   │            │
│       │ Events                                            │            │
│       └───────────────────────────────────────────────────┘            │
│                          (정산 완료 → 티어 재평가)                        │
└─────────────────────────────────────────────────────────────────────────┘
```

### 관계 요약

```
• Catalog → Order/Cart       : OHS (여러 소비자에게 상품 정보 제공)
• Cart → Order               : C-S (주문 생성 시 카트 데이터 조회)
• Order → Inventory          : C-S (재고 선점, 동기)
• Order → Promotion          : C-S (할인 조회, 동기)
• Partner → Tracking         : C-S (링크 발급 시 파트너 정보 조회)
• Order ↔ Payment            : Partnership (결제 요청/완료, 양방향)
• Order → Shipping           : Events (주문 확정 → 배송 시작)
• Order → Tracking           : Events (주문 확정 → 귀속 판정)
• Order → Cart               : Events (주문 확정 → 상품 제거)
• Shipping → Inventory       : Events (배송 출고 → 재고 확정 차감)
• Tracking → Commission      : Events (귀속 판정 → 커미션 산정)
• Commission → Partner       : Events (정산 완료 → 티어 재평가)
• Payment → PG사             : ACL (외부 PG 격리)
• Shipping → 외부 물류사       : ACL (외부 물류 격리)
```

### 범례

```
────→ OHS         : Open Host Service (동기, 여러 소비자)
────→ C-S         : Customer-Supplier (동기)
◄───► Partnership : 양방향 협력 (비동기)
────→ Events      : 도메인 이벤트 (비동기)
────→ ACL         : Anti-Corruption Layer (외부 격리)
```

---

## 통신 방식

| 관계                 | 매핑 패턴      | 통신 방식 | 이유                  |
|--------------------|------------|---------|---------------------|
| Catalog → Order/Cart | OHS        | 동기     | 상품 정보 즉시 조회 필요    |
| Cart → Order       | C-S        | 동기     | 주문 생성 시 카트 데이터 조회 |
| Order → Inventory  | C-S        | 동기     | 재고 선점, 즉시 품절 응답   |
| Order → Promotion  | C-S        | 동기     | 할인 계산 즉시 필요       |
| Partner → Tracking | C-S        | 동기     | 링크 발급 시 파트너 정보    |
| Order ↔ Payment    | Partnership | 비동기    | PG 처리 시간 불확정      |
| Order → Shipping   | Events     | 비동기    | 배송은 독립적으로 진행      |
| Order → Tracking   | Events     | 비동기    | 귀속 판정은 후처리        |
| Order → Cart       | Events     | 비동기    | 주문 확정 후 상품 제거     |
| Shipping → Inventory | Events   | 비동기    | 배송 출고 시 재고 확정     |
| Tracking → Commission | Events  | 비동기    | 커미션 산정은 별도 생명주기  |
| Commission → Partner | Events   | 비동기    | 티어 재평가는 정산 후 처리  |
| Payment → PG사     | ACL        | 비동기    | 외부 PG API 격리       |
| Shipping → 물류사   | ACL        | 비동기    | 외부 물류 API 격리       |

---

## 패키지 구조

```
com.ecommerce/
├── shared/             ← Shared Kernel (Money, Address, ProductId)
├── partner/            ← ⭐ Core
├── tracking/           ← ⭐ Core
├── commission/         ← ⭐ Core
├── order/              ← Supporting
├── cart/               ← Supporting
├── catalog/            ← Supporting
├── inventory/          ← Supporting
├── shipping/           ← Supporting
├── promotion/          ← Supporting
└── payment/            ← Generic
    └── adapter/out/pg/ ← ACL (토스, 페이팔)
```
