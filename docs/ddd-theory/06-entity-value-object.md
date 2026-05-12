# DDD 이론 06: Entity와 Value Object 심화

> "어떤 객체는 속성이 아니라 연속성과 식별성에 의해 정의된다." — Eric Evans

## 1. Entity의 본질: 식별성(Identity)

### Entity란?

**고유한 식별자(Identity)**로 구분되는 도메인 객체. 속성이 모두 바뀌어도 같은 식별자를 가지면 같은 Entity.

**테세우스의 배:** 널빤지를 전부 교체해도 같은 배인가? → DDD에서 Entity는 "예". 식별성이 유지되는 한 같은 Entity.

### Entity의 핵심 특성

| 특성 | 설명 |
|---|---|
| 고유 식별자 | 유일한 ID. UUID, 시퀀스, 자연키 등 |
| 생명주기 | 생성 → 변경 → 삭제. 시간에 따라 상태 변화 |
| 가변성 | 속성 변경 가능 (단, 식별자는 불변) |
| 추적 가능성 | 이력 관리, 감사 로그 필요 |

### 식별자 전략

| 전략 | 장점 | 단점 | 적합한 경우 |
|---|---|---|---|
| UUID | 분산 환경, 충돌 없음 | 길이, 정렬 어려움 | 분산 시스템 |
| ULID | 시간순 정렬, UUID 호환 | 상대적으로 새로운 표준 | 이벤트 기반 |
| 시퀀스 | 간단, 정렬 용이 | 분산 환경 어려움 | 단일 DB |
| 자연키 | 의미 있는 값 | 변경 가능성 | ISBN, 차대번호 |
| Snowflake | 분산, 시간순, 컴팩트 | 시계 동기화 필요 | 대규모 분산 |

### 식별자를 Value Object로 래핑

```kotlin
// 타입 안전성 확보 — 컴파일 타임에 오류 감지
data class OrderId(val value: String) {
    companion object {
        fun generate(): OrderId = OrderId(UUID.randomUUID().toString())
        fun from(value: String): OrderId {
            require(value.isNotBlank()) { "OrderId cannot be empty" }
            return OrderId(value)
        }
    }
}

data class CustomerId(val value: String)

// ❌ 컴파일 에러: OrderId와 CustomerId는 다른 타입
fun findOrder(orderId: OrderId): Order
findOrder(customerId) // 타입 불일치!
```

### Entity 동등성

```kotlin
abstract class Entity<T>(val id: T) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity<*>) return false
        return id == other.id  // 식별자 기반 동등성
    }
    override fun hashCode(): Int = id.hashCode()
}
```

### Entity 설계 원칙

- 의미 있는 비즈니스 메서드 제공 (무분별한 setter 금지)
- 상태 변경은 도메인 규칙을 통해서만
- 항상 유효한 상태 유지 (불변식 보호)

---

## 2. Value Object의 본질: 값 동등성과 불변성

> "어떤 객체는 속성만으로 정의된다. 식별성이 없고, 속성 값이 같으면 같은 것으로 취급한다." — Eric Evans

### Value Object란?

**속성의 조합**으로 정의되는 객체. 식별자 없음. 모든 속성이 같으면 같은 객체.

**현실 비유:** 만원짜리 지폐 두 장 — 일련번호는 다르지만 "같은 만원"으로 취급. 어떤 만원을 내든 상관없음.

### 7가지 특성

| # | 특성 | 설명 |
|---|---|---|
| 1 | **불변성** | 생성 후 상태 변경 불가. 변경 필요 시 새 객체 생성 |
| 2 | **값 동등성** | 모든 속성이 같으면 같은 객체 (참조가 아닌 값 비교) |
| 3 | **교체 가능성** | 같은 값의 다른 인스턴스로 언제든 교체 가능 |
| 4 | **자가 검증** | 생성 시점에 유효성 검증, 항상 유효한 상태 보장 |
| 5 | **부작용 없음** | 메서드가 상태 변경 안 함. 새 객체 반환 |
| 6 | **개념적 완전성** | 관련 속성을 하나의 의미 있는 단위로 묶음 |
| 7 | **도메인 로직 캡슐화** | 해당 값 관련 비즈니스 로직을 내부에 포함 |

### Money Value Object 구현

```kotlin
data class Money private constructor(
    val amount: Long,  // 센트 단위로 저장 (부동소수점 회피)
    val currency: Currency
) {
    init {
        require(amount >= 0) { "금액은 0 이상이어야 합니다: $amount" }
    }

    companion object {
        fun of(amount: Long, currency: Currency) = Money(amount, currency)
        fun won(amount: Long) = Money(amount, Currency.KRW)
        fun zero(currency: Currency) = Money(0, currency)
    }

    // 연산 — 새 객체 반환 (불변성)
    fun add(other: Money): Money {
        ensureSameCurrency(other)
        return Money(amount + other.amount, currency)
    }

    fun subtract(other: Money): Money {
        ensureSameCurrency(other)
        require(amount >= other.amount) { "잔액 부족" }
        return Money(amount - other.amount, currency)
    }

    fun multiply(factor: Int): Money = Money(amount * factor, currency)

    // 비교
    fun isGreaterThan(other: Money): Boolean {
        ensureSameCurrency(other)
        return amount > other.amount
    }

    fun isZero(): Boolean = amount == 0L

    private fun ensureSameCurrency(other: Money) {
        require(currency == other.currency) { "통화 불일치: $currency vs ${other.currency}" }
    }
}
```

### Address Value Object 구현

```kotlin
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
) {
    init {
        require(street.isNotBlank()) { "street은 필수" }
        require(city.isNotBlank()) { "city는 필수" }
        require(zipCode.isNotBlank()) { "zipCode는 필수" }
    }
}
```

---

## 3. Entity vs Value Object 선택 기준

### 핵심 질문

| 질문 | Entity | Value Object |
|---|---|---|
| 추적해야 하는가? | Yes | No |
| 값이 같으면 같은가? | No (ID로 구분) | Yes |
| 교체해도 문제없는가? | No | Yes |
| 상태가 변하는가? | Yes | No (새 객체 생성) |
| 생명주기가 있는가? | Yes | No |

### 원칙

> "가능하면 Entity보다 Value Object를 선호하라" — Vaughn Vernon

이유: 불변이라 추론 쉬움, 스레드 안전, 테스트 쉬움, 부작용 없음.

### 확실하지 않을 때

**Value Object로 시작** → 나중에 추적이 필요해지면 Entity로 전환.

### 컨텍스트에 따라 달라짐

같은 개념도 컨텍스트에 따라 Entity 또는 VO:
- "좌석" — 예약 Context에서는 Entity (A-12를 추적), 통계 Context에서는 VO (유형별 집계)
- "주소" — 배송 Context에서는 VO (값), 부동산 Context에서는 Entity (이력 추적)

---

## 4. Primitive Obsession 안티패턴

도메인 개념을 기본 타입(String, Int, Long)으로 표현하는 안티패턴.

### 문제

```kotlin
// ❌ Primitive Obsession
fun createOrder(
    userId: String,      // 어떤 형식? 검증은?
    amount: Long,        // 통화는? 음수 가능?
    email: String,       // 유효한 이메일?
    quantity: Int         // 0 가능? 음수?
)

// userId 자리에 email을 넣어도 컴파일 에러 없음!
createOrder(email, amount, userId, quantity)  // 버그!
```

### 해결

```kotlin
// ✓ Value Object로 래핑
fun createOrder(
    userId: UserId,
    amount: Money,
    email: Email,
    quantity: Quantity
)

// 컴파일 타임에 오류 감지
createOrder(email, amount, userId, quantity)  // 컴파일 에러!
```

### 리팩토링 전략

1. **새 코드부터 적용** — 기존 코드는 점진적으로
2. **핵심 도메인 우선** — Core Domain의 핵심 개념부터
3. **경계에서 변환** — API 레이어에서 primitive ↔ VO 변환
4. **점진적 확산** — 한 번에 전부 바꾸지 않음

**주의:** 모든 primitive를 VO로 감쌀 필요 없음. **도메인에서 의미 있는 개념만.**

---

## 5. 영속성 전략

### Entity 영속성

Repository를 통해 저장/조회. Mapper 패턴으로 도메인 ↔ DB 변환.

```kotlin
// 도메인 모델 (순수)
class Order(val id: OrderId, ...) 

// JPA Entity (영속성)
@Entity class OrderJpaEntity(val id: String, ...)

// Mapper
fun OrderJpaEntity.toDomain(): Order
fun Order.toJpaEntity(): OrderJpaEntity
```

### Value Object 영속성

| 전략 | 설명 | 적합한 경우 |
|---|---|---|
| @Embedded | Entity 테이블에 컬럼으로 포함 | 단일 VO |
| JSON 컬럼 | 직렬화하여 하나의 컬럼에 저장 | 복잡한 VO |
| 별도 테이블 | 1:N 관계로 저장 | VO 컬렉션 |

```kotlin
@Entity
class OrderJpaEntity(
    @Id val id: String,
    
    // @Embedded — VO를 컬럼으로
    @Embedded val shippingAddress: AddressEmbeddable,
    
    // JSON — 복잡한 VO
    @Column(columnDefinition = "jsonb")
    val metadata: String
)
```

---

## 6. 도메인별 적용 예시

### 이커머스

| Entity | Value Object |
|---|---|
| Order, Customer, Product | Money, Address, OrderItem, SKU |

### 금융

| Entity | Value Object |
|---|---|
| Account, Transaction | Money, Currency, DateRange, AccountNumber |

### 예약

| Entity | Value Object |
|---|---|
| Reservation, Room | TimeSlot, DateRange, GuestCount, RoomType |

---

## 핵심 정리

1. **Entity** = 식별자 + 생명주기 + 가변. ID가 같으면 같은 것.
2. **Value Object** = 값 동등성 + 불변. 속성이 같으면 같은 것.
3. **VO를 선호하라** — 불변, 안전, 테스트 쉬움.
4. **식별자도 VO로 래핑** — 타입 안전성 확보.
5. **Primitive Obsession 제거** — 도메인 개념을 VO로 표현.
6. **컨텍스트에 따라 달라짐** — 같은 개념도 Context별로 Entity/VO 다를 수 있음.
7. **확실하지 않으면 VO로 시작** — 필요 시 Entity로 전환.
