# DDD 이론 07: Aggregate 설계 심화

> "Aggregate는 데이터 변경의 단위로 취급되는 연관된 객체들의 클러스터다. 각 Aggregate는 루트와 경계를 가진다." — Eric Evans

## 1. 왜 Aggregate가 필요한가?

### Aggregate 없이 발생하는 문제

**일관성 깨짐:**
```kotlin
orderItemRepository.delete(item)  // 주문항목 삭제
// order.totalAmount는 여전히 이전 값 → 불일치!
```

**불변식 위반:**
```kotlin
order.items.add(newItem)  // 항목 추가
// recalculateTotal() 호출 안 하면 → totalAmount ≠ items 합계
```

**동시성 충돌:**
```kotlin
User A: order.addItem(itemA)  // 트랜잭션 1
User B: order.addItem(itemB)  // 트랜잭션 2
// Lost Update 발생!
```

### Aggregate가 해결하는 것

| 문제 | 해결 |
|---|---|
| 일관성 깨짐 | Aggregate 내부는 항상 일관된 상태 유지 |
| 불변식 위반 | Root를 통해서만 접근 → 규칙 항상 지켜짐 |
| 동시성 충돌 | Aggregate 단위 Optimistic Locking |
| 트랜잭션 범위 모호 | 하나의 트랜잭션 = 하나의 Aggregate |

---

## 2. Aggregate의 구성 요소

```
┌─────────────────────────────────────────────┐
│              Aggregate                        │
│                                              │
│   ┌──────────────────────┐                   │
│   │   Aggregate Root     │ ← 유일한 진입점   │
│   │     (Entity)         │                   │
│   └──────────┬───────────┘                   │
│              │                               │
│     ┌────────┼────────┐                      │
│     ▼        ▼        ▼                      │
│  Entity   Value     Entity                   │
│           Object    (내부)                    │
│                                              │
│  ← Aggregate Boundary →                      │
└─────────────────────────────────────────────┘

- 외부에서는 Root를 통해서만 접근
- 내부 객체는 외부에 직접 노출 안 됨
- 트랜잭션 = 하나의 Aggregate
```

### 핵심 용어

| 용어 | 설명 |
|---|---|
| **Aggregate Root** | 진입점 Entity. 전역 식별자. Repository는 Root만 저장/조회 |
| **Boundary** | 일관성 보장 범위. 내부 객체는 하나의 트랜잭션에서 함께 변경 |
| **Invariant** | 항상 참이어야 하는 비즈니스 규칙 |
| **Local Identity** | 내부 Entity의 식별자. Aggregate 내에서만 유일 |

---

## 3. Aggregate Root의 책임

### 1. 유일한 진입점

```kotlin
// ❌ 내부 객체 직접 조작
order.items[0].quantity = 5

// ✓ Root를 통한 조작
order.updateItemQuantity(lineNumber = 1, quantity = Quantity.of(5))
```

### 2. 불변식 보호

```kotlin
class Order : AggregateRoot<OrderId>() {
    fun addItem(item: OrderItem) {
        check(status == OrderStatus.DRAFT) { "확정된 주문은 수정 불가" }
        items.add(item)
        recalculateTotal()  // 불변식 유지
    }
}
```

### 3. 전역 식별자 제공

Root만 전역 ID. 내부 Entity는 지역 ID (Aggregate 내에서만 유일).

### 4. 트랜잭션 경계

하나의 트랜잭션에서 하나의 Aggregate만 수정. 여러 Aggregate 수정 필요 시 → 도메인 이벤트.

---

## 4. 불변식(Invariant)과 일관성 경계

### 불변식 종류

| 종류 | 범위 | 예시 |
|---|---|---|
| 단일 Entity 불변식 | Entity 내부 | 수량 ≥ 1, 금액 ≥ 0 |
| Aggregate 내 불변식 | Root + 내부 Entity 간 | totalAmount = items 합계 |
| 상태 전이 불변식 | 상태 변경 규칙 | DRAFT → CONFIRMED만 가능 |

### 강한 일관성 vs 최종 일관성

| | Aggregate 내부 | Aggregate 간 |
|---|---|---|
| 일관성 | **강한 일관성** (트랜잭션) | **최종 일관성** (이벤트) |
| 보장 | 항상 일관 | 결국 일관 |
| 메커니즘 | 같은 트랜잭션 | 도메인 이벤트 + 비동기 |

---

## 5. 올바른 Aggregate 크기

> "작은 Aggregate를 설계하라. 대부분은 Root + Value Object만으로 구성된다." — Vaughn Vernon

### 4가지 원칙

1. **작게 설계** — 진정한 불변식만 포함
2. **ID로 다른 Aggregate 참조** — 직접 객체 참조 금지
3. **최종 일관성 수용** — Aggregate 간은 이벤트로
4. **하나의 트랜잭션에서 하나의 Aggregate만 수정**

### 큰 Aggregate 경고 신호

- 내부 Entity 3개 이상
- 무한 증가 컬렉션 (예: 주문의 모든 이력)
- 잦은 Optimistic Lock 실패
- 불필요하게 넓은 트랜잭션

### 분리 판단 기준

| 질문 | Yes → 같은 Aggregate | No → 분리 |
|---|---|---|
| 반드시 함께 변경되어야 하는가? | 같이 | 분리 |
| 동시성 충돌이 자주 발생하는가? | — | 분리 |
| 최종 일관성으로 충분한가? | — | 분리 |

### 예시: 주문과 배송

```
❌ 큰 Aggregate (주문 + 배송을 하나로)
→ 배송 상태 변경할 때마다 주문 전체에 락

✓ 분리된 Aggregate
Order Aggregate: 주문 생성, 항목 관리, 결제
Shipment Aggregate: 배송 상태, 추적
→ orderId로 참조, 이벤트로 연결
```

---

## 6. Aggregate 간 참조와 통신

### ID 참조 (직접 객체 참조 금지)

```kotlin
class Order(
    val id: OrderId,
    val customerId: CustomerId,  // ✓ ID로 참조
    // val customer: Customer    // ❌ 객체 직접 참조
)
```

### 통신 패턴

| 패턴 | 용도 | 예시 |
|---|---|---|
| 동기 조회 | 읽기 (다른 Aggregate 데이터 필요) | 주문 생성 시 상품 가격 조회 |
| 도메인 이벤트 | 쓰기 (다른 Aggregate 상태 변경) | 결제완료 → 주문확정 |
| Saga / Process Manager | 복잡한 프로세스 (여러 Aggregate 조율) | 주문→결제→재고→배송 |

### Saga 패턴

여러 Aggregate에 걸친 비즈니스 프로세스를 조율. 각 단계 실패 시 보상 트랜잭션으로 롤백.

```
[정상 흐름]
주문 생성 → 결제 처리 → 재고 차감 → 배송 생성

[실패 시 보상]
재고 차감 실패 → 결제 환불 → 주문 취소
```

---

## 7. Optimistic Locking

Aggregate 단위로 버전 관리. 동시성 충돌 감지.

```kotlin
@Entity
class OrderJpaEntity(
    @Id val id: String,
    @Version val version: Long = 0,  // 버전 필드
    // ...
)

// UPDATE orders SET status = 'CONFIRMED', version = version + 1
// WHERE id = ? AND version = ?
// → 영향받은 행이 0이면 OptimisticLockException
```

---

## 8. Repository 규칙

| 규칙 | 설명 |
|---|---|
| Root만 Repository를 가짐 | 내부 Entity는 별도 Repository 없음 |
| 전체를 원자적으로 저장/로드 | Root + 내부 Entity 한 번에 |
| 컬렉션처럼 동작 | add, findById, remove |
| 도메인 객체 반환 | JPA Entity가 아닌 도메인 모델 반환 |

```kotlin
interface OrderRepository {
    fun findById(id: OrderId): Order?
    fun save(order: Order): Order
    fun delete(order: Order)
}
```

---

## 9. 영속성 전략

| 전략 | 적합한 경우 |
|---|---|
| ORM (JPA) | 일반 CRUD, 단순한 Aggregate |
| Document DB (MongoDB) | 복잡한 Aggregate (자연스러운 문서 구조) |
| Event Sourcing | 감사 필수, 이력 추적, 시간 여행 필요 |

### ORM 주의사항

- 도메인 모델 ≠ JPA Entity (Mapper로 분리 권장)
- Lazy Loading 주의 (Aggregate 전체를 한 번에 로드)
- N+1 문제 방지

---

## 10. 설계 체크리스트

- [ ] Aggregate Root가 명확한가?
- [ ] 불변식이 식별되었는가?
- [ ] 외부에서 Root를 통해서만 접근하는가?
- [ ] 다른 Aggregate는 ID로만 참조하는가?
- [ ] 하나의 트랜잭션에서 하나의 Aggregate만 수정하는가?
- [ ] Aggregate가 너무 크지 않은가? (내부 Entity 3개 이하)
- [ ] 무한 증가 컬렉션이 없는가?
- [ ] Optimistic Locking이 적용되었는가?

---

## 핵심 정리

1. **Aggregate = 일관성 경계** — 내부는 항상 일관, 외부는 최종 일관성
2. **Root = 유일한 진입점** — 불변식 보호, 전역 ID, 트랜잭션 경계
3. **작게 설계** — 진정한 불변식만 포함, 대부분 Root + VO
4. **ID로 참조** — 다른 Aggregate는 절대 객체 직접 참조 금지
5. **하나의 트랜잭션 = 하나의 Aggregate** — 여러 개 수정 필요 시 이벤트
6. **Saga** — 여러 Aggregate 걸친 프로세스 조율 + 보상 트랜잭션
7. **Optimistic Locking** — Aggregate 단위 버전 관리로 동시성 제어
