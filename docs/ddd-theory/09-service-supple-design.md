# DDD 이론 9: Domain Service vs Application Service & Supple Design 패턴

> **학습 목표**
> - Domain Service와 Application Service의 차이를 명확히 이해한다
> - Service를 사용해야 할 때와 사용하지 말아야 할 때를 구분한다
> - Supple Design의 6가지 패턴을 이해하고 적용할 수 있다
> - 의도를 드러내는 인터페이스를 설계할 수 있다
> - 부작용 없는 함수를 작성할 수 있다

---

## 1. Domain Service vs Application Service

### 1.1 핵심 인용

> "도메인의 중요한 프로세스나 변환이 Entity나 Value Object의 자연스러운 책임이 아닐 때,
> 그 연산을 Service로 선언된 독립적인 인터페이스로 모델에 추가하라."
>
> — Eric Evans, Domain-Driven Design

### 1.2 Service가 필요한 경우

| 상황 | 설명 | 예시 |
|------|------|------|
| 여러 Aggregate에 걸친 연산 | 하나의 Entity에 자연스럽게 속하지 않는 도메인 로직 | 계좌 간 송금 |
| 외부 시스템 연동 | 결제 게이트웨이, 이메일 발송 등 인프라 의존 | 결제 처리 |
| 복잡한 비즈니스 규칙 | 여러 도메인 객체의 상태를 조합해야 하는 규칙 | 가격 정책 계산 |

### 1.3 상세 비교표

| 구분 | Domain Service | Application Service |
|------|---------------|-------------------|
| **위치** | 도메인 레이어 | 애플리케이션 레이어 |
| **책임** | 도메인 로직 수행 | 유스케이스 조율 (Orchestration) |
| **상태** | 무상태 (Stateless) | 무상태 (Stateless) |
| **의존성** | 도메인 객체만 의존 | Repository, Domain Service, 인프라 등 |
| **트랜잭션** | 관여하지 않음 | 트랜잭션 경계 관리 |
| **테스트** | 단위 테스트 용이 (mock 불필요) | 통합 테스트 필요 (mock 필요) |
| **예시** | TransferService, PricingService | OrderApplicationService |
| **인프라 접근** | ❌ 불가 | ✅ 가능 |
| **이벤트 발행** | 도메인 이벤트 생성 가능 | 이벤트 발행 조율 |

### 1.4 Domain Service 구현 (Kotlin)

```kotlin
/**
 * Domain Service: 순수한 도메인 로직만 포함
 * - 인프라 의존성 없음
 * - 무상태
 * - 여러 Aggregate에 걸친 비즈니스 로직 수행
 */
class PricingService {

    fun calculateOrderTotal(
        items: List<OrderItem>,
        customer: Customer,
        coupon: Coupon?
    ): PricingResult {
        // 1. 기본 가격 계산
        val subtotal = items.fold(Money.zero()) { sum, item ->
            sum.add(item.lineTotal)
        }

        // 2. 회원 등급 할인
        val memberDiscount = calculateMemberDiscount(subtotal, customer.membershipLevel)

        // 3. 쿠폰 할인
        val couponDiscount = coupon?.let {
            calculateCouponDiscount(subtotal, it)
        } ?: Money.zero()

        // 4. 배송비 계산
        val shippingFee = calculateShippingFee(
            subtotal.subtract(memberDiscount).subtract(couponDiscount)
        )

        return PricingResult(subtotal, memberDiscount, couponDiscount, shippingFee)
    }

    private fun calculateMemberDiscount(amount: Money, level: MembershipLevel): Money {
        val rate = when (level) {
            MembershipLevel.BRONZE -> 0.0
            MembershipLevel.SILVER -> 0.03
            MembershipLevel.GOLD -> 0.05
            MembershipLevel.VIP -> 0.10
        }
        return amount.multiply(rate)
    }

    private fun calculateShippingFee(amount: Money): Money {
        return if (amount.isGreaterThanOrEqual(Money.won(50_000)))
            Money.zero()
        else
            Money.won(3_000)
    }
}
```

**송금 Domain Service 예시:**

```kotlin
class TransferService {

    fun transfer(from: Account, to: Account, amount: Money) {
        require(from.id != to.id) { "같은 계좌로 송금할 수 없습니다" }
        from.withdraw(amount)
        to.deposit(amount)
    }
}
```

### 1.5 Application Service 구현 (Kotlin)

```kotlin
/**
 * Application Service: 유스케이스 조율
 * - 트랜잭션 경계 관리
 * - Repository 호출
 * - Domain Service 위임
 * - 이벤트 발행
 */
class OrderApplicationService(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val pricingService: PricingService,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional
    fun createOrder(command: CreateOrderCommand): OrderId {
        // 1. 필요한 데이터 조회
        val customer = customerRepository.findById(CustomerId.from(command.customerId))
            ?: throw CustomerNotFoundError(command.customerId)

        val products = productRepository.findByIds(
            command.items.map { ProductId.from(it.productId) }
        )

        // 2. 도메인 객체 생성
        val order = Order.create(
            customer.id,
            Address.from(command.shippingAddress)
        )

        // 3. 주문 항목 추가
        command.items.forEach { item ->
            val product = products.find { it.id.value == item.productId }
                ?: throw ProductNotFoundError(item.productId)

            order.addItem(product.id, product.name, Quantity.of(item.quantity), product.price)
        }

        // 4. 가격 계산 (Domain Service 위임)
        val pricing = pricingService.calculateOrderTotal(
            order.items,
            customer,
            command.couponCode?.let { findCoupon(it) }
        )
        order.applyPricing(pricing)

        // 5. 저장
        orderRepository.save(order)

        // 6. 이벤트 발행
        eventPublisher.publishAll(order.domainEvents)

        return order.id
    }
}
```

### 1.6 언제 어디에 로직을 둘 것인가?

```
로직이 하나의 Entity에 자연스럽게 속하는가?
  ├─ YES → Entity에 둔다
  └─ NO → 여러 Aggregate에 걸친 순수 도메인 로직인가?
            ├─ YES → Domain Service에 둔다
            └─ NO → Application Service에 둔다 (조율, 인프라 접근)
```

---

## 2. Supple Design 패턴 (유연한 설계)

### 핵심 인용

> "유연한 설계(Supple Design)는 클라이언트 개발자가 도메인 객체를
> 자연스럽게 조합하여 의미 있는 표현을 만들 수 있게 한다."
>
> — Eric Evans, Domain-Driven Design

---

### 2.1 Intention-Revealing Interfaces (의도를 드러내는 인터페이스)

**원칙:** 메서드 이름만으로 의도를 명확히 전달한다. 구현을 보지 않아도 무엇을 하는지 알 수 있어야 한다.

#### ❌ 안티패턴: 의도가 불명확

```kotlin
class Order {
    fun process() { /* ... */ }
    fun handle() { /* ... */ }
    fun doIt() { /* ... */ }
    fun update(data: Any) { /* ... */ }
}
```

#### ✅ 올바른 적용: 의도가 명확

```kotlin
class Order {
    fun confirm() { /* 주문 확정 */ }
    fun cancel(reason: CancellationReason) { /* 주문 취소 */ }
    fun ship(trackingNumber: TrackingNumber) { /* 배송 시작 */ }
    fun addItem(item: OrderItem) { /* 항목 추가 */ }
    fun removeItem(itemId: OrderItemId) { /* 항목 제거 */ }
}
```

**사용 시점:**
- 모든 public 메서드와 클래스에 적용
- 도메인 전문가가 읽어도 이해할 수 있는 이름 사용
- Ubiquitous Language를 반영

**안티패턴:**
- `process()`, `handle()`, `execute()` 같은 범용적 이름
- 기술 용어 남용 (`updateDB()`, `syncCache()`)
- 약어 사용 (`calcTtl()` → `calculateTotal()`)

---

### 2.2 Side-Effect-Free Functions (부작용 없는 함수)

**원칙:** 같은 입력에 항상 같은 출력을 반환하고, 상태를 변경하지 않는다.

#### ❌ 안티패턴: 부작용 있음

```kotlin
class Money(var amount: Long, val currency: Currency) {
    // 자신의 상태를 변경 (부작용!)
    fun add(other: Money) {
        this.amount += other.amount
    }
}

// 사용 시 예측 불가
val a = Money(1000, Currency.KRW)
a.add(Money(500, Currency.KRW))
// a가 변경됨 → 다른 곳에서 a를 참조하면 문제 발생
```

#### ✅ 올바른 적용: 부작용 없음 (불변)

```kotlin
data class Money private constructor(
    val amount: Long,
    val currency: Currency
) {
    // 새 객체 반환 (불변)
    fun add(other: Money): Money {
        require(currency == other.currency) { "통화가 다릅니다" }
        return Money(amount + other.amount, currency)
    }

    fun subtract(other: Money): Money {
        require(currency == other.currency) { "통화가 다릅니다" }
        return Money(amount - other.amount, currency)
    }

    fun multiply(factor: Double): Money {
        return Money((amount * factor).toLong(), currency)
    }

    companion object {
        fun won(amount: Long) = Money(amount, Currency.KRW)
        fun zero() = Money(0, Currency.KRW)
    }
}

// 사용: 원본 불변, 새 객체 생성
val a = Money.won(1000)
val b = a.add(Money.won(500))
// a는 그대로 1000원, b는 1500원
```

**사용 시점:**
- Value Object의 모든 연산
- 쿼리 메서드 (상태 조회)
- 도메인 계산 로직

**안티패턴:**
- Value Object의 상태를 변경하는 메서드
- 계산과 상태 변경을 하나의 메서드에 혼합
- 숨겨진 부작용 (로깅 외의 외부 상태 변경)

---

### 2.3 Assertions (단언)

**원칙:** 사전조건(Precondition), 사후조건(Postcondition), 불변식(Invariant)을 명시적으로 표현한다.

```kotlin
class Order private constructor(
    val id: OrderId,
    private var _status: OrderStatus,
    private val _items: MutableList<OrderItem>
) {
    val status: OrderStatus get() = _status
    val items: List<OrderItem> get() = _items.toList()

    fun confirm() {
        // 사전조건 (Precondition)
        require(_status == OrderStatus.DRAFT) {
            "주문은 DRAFT 상태에서만 확정할 수 있습니다. 현재: $_status"
        }
        require(_items.isNotEmpty()) {
            "주문에 최소 하나의 항목이 있어야 합니다"
        }

        _status = OrderStatus.CONFIRMED

        // 사후조건 (Postcondition)
        check(_status == OrderStatus.CONFIRMED)
        assertInvariant()
    }

    private fun assertInvariant() {
        // 불변식: 확정된 주문은 항상 항목이 있어야 함
        check(_items.isNotEmpty()) { "확정된 주문에 항목이 없습니다" }
        // 불변식: 총액은 항상 0 이상
        check(calculateTotal().amount >= 0) { "총액이 음수입니다" }
    }
}

// Value Object에서의 Assertion
data class Money private constructor(val amount: Long, val currency: Currency) {
    companion object {
        fun of(amount: Long, currency: Currency): Money {
            // 사전조건
            require(amount >= 0) { "금액은 음수일 수 없습니다: $amount" }
            return Money(amount, currency)
        }
    }
}
```

**사용 시점:**
- 도메인 규칙을 코드로 명시할 때
- 복잡한 상태 전이가 있을 때
- 디버깅과 문서화를 동시에 달성하고 싶을 때

**안티패턴:**
- 조건 검증 없이 상태 변경
- 예외 메시지 없는 단순 throw
- 불변식을 주석으로만 표현

---

### 2.4 Conceptual Contours (개념적 윤곽)

**원칙:** 도메인의 자연스러운 경계를 따라 설계한다. 함께 변경되는 것은 함께, 독립적인 것은 분리한다.

#### ❌ 안티패턴: 부자연스러운 경계

```kotlin
// 너무 세분화 → 조합이 번거로움
class Street(val value: String)
class City(val value: String)
class ZipCode(val value: String)
class Country(val value: String)

// 너무 뭉뚱그림 → 변경 영향 범위가 큼
data class CustomerData(
    val name: String,
    val email: String,
    val phone: String,
    val street: String,
    val city: String,
    val zipCode: String,
    val cardNumber: String,
    val cardExpiry: String
)
```

#### ✅ 올바른 적용: 자연스러운 경계

```kotlin
// 개념적으로 완전한 단위로 분리
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
)

data class PersonName(
    val firstName: String,
    val lastName: String
) {
    val fullName: String get() = "$firstName $lastName"
}

class Customer(
    val id: CustomerId,
    val name: PersonName,
    val email: Email,
    val phone: PhoneNumber,
    val address: Address,
    val paymentMethod: PaymentMethod
)
```

**사용 시점:**
- 클래스/모듈의 경계를 결정할 때
- 리팩토링 시 응집도를 높이고 싶을 때
- 도메인 개념이 자연스럽게 묶이는 단위를 찾을 때

**안티패턴:**
- God Object (모든 것을 하나에 담음)
- 과도한 분리 (하나의 개념을 여러 클래스로 쪼갬)
- 기술적 기준으로 분리 (도메인 기준이 아닌 getter/setter 기준)

---

### 2.5 Standalone Classes (독립적 클래스)

**원칙:** 가능한 한 독립적인 클래스를 만든다. 의존성이 적을수록 이해하고 테스트하기 쉽다.

```kotlin
/**
 * 독립적인 Value Object - 외부 의존성 없음
 * 자체적으로 완전한 기능을 제공
 */
data class Money private constructor(
    val amount: Long,
    val currency: Currency
) {
    fun add(other: Money): Money {
        require(currency == other.currency) { "통화 불일치" }
        return Money(amount + other.amount, currency)
    }

    fun subtract(other: Money): Money {
        require(currency == other.currency) { "통화 불일치" }
        return Money(amount - other.amount, currency)
    }

    fun multiply(factor: Double): Money =
        Money((amount * factor).toLong(), currency)

    fun isGreaterThan(other: Money): Boolean = amount > other.amount

    fun isGreaterThanOrEqual(other: Money): Boolean = amount >= other.amount

    fun format(): String = "${currency.symbol}${"%,d".format(amount)}"

    companion object {
        fun won(amount: Long) = Money(amount, Currency.KRW)
        fun zero() = Money(0, Currency.KRW)
        fun of(amount: Long, currency: Currency) = Money(amount, currency)
    }
}

/**
 * 독립적인 Domain Service - 외부 의존성 없이 순수 계산
 */
class TaxCalculator {
    fun calculate(amount: Money, taxRate: TaxRate): Money {
        return amount.multiply(taxRate.value)
    }
}
```

**사용 시점:**
- Value Object 설계 시
- 유틸리티성 Domain Service 설계 시
- 재사용 가능한 도메인 개념을 추출할 때

**안티패턴:**
- 불필요한 의존성 주입 (계산만 하는데 Repository 의존)
- 순환 의존성
- 테스트 시 과도한 mock이 필요한 구조

---

### 2.6 Closure of Operations (연산의 닫힘)

**원칙:** 연산의 반환 타입이 인자와 같은 타입이면 조합(composition)이 가능해진다.

```kotlin
data class Money private constructor(
    val amount: Long,
    val currency: Currency
) {
    // Closure of Operations: Money + Money = Money
    fun add(other: Money): Money {
        require(currency == other.currency)
        return Money(amount + other.amount, currency)
    }

    // Money - Money = Money
    fun subtract(other: Money): Money {
        require(currency == other.currency)
        return Money(amount - other.amount, currency)
    }

    companion object {
        fun won(amount: Long) = Money(amount, Currency.KRW)
        fun zero() = Money(0, Currency.KRW)
    }
}

// 자연스러운 조합 가능
val total = items
    .map { it.price }
    .fold(Money.zero()) { sum, price -> sum.add(price) }

// 체이닝 가능
val finalPrice = basePrice
    .add(shippingFee)
    .subtract(discount)
    .add(tax)
```

**Specification 패턴에서의 Closure of Operations:**

```kotlin
interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean

    // Specification AND Specification = Specification (Closure!)
    fun and(other: Specification<T>): Specification<T> =
        AndSpecification(this, other)

    // Specification OR Specification = Specification (Closure!)
    fun or(other: Specification<T>): Specification<T> =
        OrSpecification(this, other)

    // NOT Specification = Specification (Closure!)
    fun not(): Specification<T> = NotSpecification(this)
}

class AndSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(candidate: T): Boolean =
        left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate)
}

// 사용 예시: 자연스러운 조합
val eligibleForDiscount = isGoldMember
    .and(hasOrderOver(Money.won(100_000)))
    .and(isNotBlacklisted.not())
```

**사용 시점:**
- 수학적 연산이 있는 Value Object (Money, Quantity, Duration)
- 조합 가능한 규칙/조건 (Specification 패턴)
- 컬렉션 연산과 함께 사용할 때

**안티패턴:**
- 반환 타입이 일관되지 않은 연산 체인
- void 반환으로 체이닝 불가능한 설계
- 타입이 달라서 조합할 수 없는 연산

---

## 3. 안티패턴 종합

### 3.1 빈약한 도메인 모델 (Anemic Domain Model)

```kotlin
// ❌ 모든 로직이 Service에 있고, Entity는 데이터만 보유
data class Order(
    var id: String,
    var status: String,
    var items: MutableList<OrderItem>,
    var totalAmount: Long
)

class OrderService {
    fun confirmOrder(order: Order) {
        order.status = "CONFIRMED"
        order.totalAmount = order.items.sumOf { it.price * it.quantity }
    }
}
```

```kotlin
// ✅ Entity가 자신의 로직을 소유
class Order private constructor(
    val id: OrderId,
    private var _status: OrderStatus,
    private val _items: MutableList<OrderItem>
) {
    fun confirm() {
        require(_status == OrderStatus.DRAFT)
        require(_items.isNotEmpty())
        _status = OrderStatus.CONFIRMED
    }

    fun calculateTotal(): Money =
        _items.fold(Money.zero()) { sum, item -> sum.add(item.lineTotal) }
}
```

### 3.2 Service 남용

- Entity에 자연스럽게 속하는 로직을 Service로 빼는 것
- 모든 비즈니스 로직을 Application Service에 넣는 것
- Domain Service가 Repository에 직접 접근하는 것

---

## 4. FAQ

**Q1: Domain Service와 Application Service를 어떻게 구분하나요?**

Domain Service는 순수한 도메인 로직(가격 계산, 송금 등)을 담당하고,
Application Service는 유스케이스 조율(트랜잭션, Repository 호출, 이벤트 발행)을 담당합니다.

**Q2: 모든 로직을 Service에 넣어도 되나요?**

아니요. 이것은 '빈약한 도메인 모델(Anemic Domain Model)' 안티패턴입니다.
Entity와 Value Object에 자연스럽게 속하는 로직은 해당 객체에 두세요.

**Q3: Side-Effect-Free Functions를 항상 사용해야 하나요?**

Value Object와 쿼리 메서드에서는 필수입니다.
Entity의 명령 메서드는 상태를 변경하므로 부작용이 있을 수 있지만, 명확히 문서화해야 합니다.

**Q4: Supple Design 패턴을 모두 적용해야 하나요?**

상황에 맞게 선택적으로 적용하세요.
가장 중요한 것은 **Intention-Revealing Interfaces**와 **Side-Effect-Free Functions**입니다.

---

## 5. 핵심 요약

| 패턴 | 핵심 원칙 | 효과 |
|------|-----------|------|
| Intention-Revealing Interfaces | 이름으로 의도 전달 | 가독성, 유지보수성 향상 |
| Side-Effect-Free Functions | 불변, 순수 함수 | 예측 가능성, 안전한 조합 |
| Assertions | 조건 명시적 표현 | 버그 조기 발견, 문서화 |
| Conceptual Contours | 자연스러운 경계 | 응집도 향상, 변경 영향 최소화 |
| Standalone Classes | 독립적 설계 | 테스트 용이, 재사용성 |
| Closure of Operations | 같은 타입 반환 | 자연스러운 조합, 체이닝 |

### Service 구분 요약

- **Domain Service**: 순수 도메인 로직, 인프라 무관, 무상태
- **Application Service**: 유스케이스 조율, 트랜잭션 관리, 인프라 접근
- **공통**: 둘 다 무상태(Stateless), Entity에 속하지 않는 로직만 담당

> "유연한 설계는 클라이언트 개발자가 도메인 객체를
> 자연스럽게 조합하여 의미 있는 표현을 만들 수 있게 한다."
>
> — Eric Evans

---

*출처: [TechMentor - DDD 이론 9](https://techmentor-avo.pages.dev/theory/ddd-theory-09)*
