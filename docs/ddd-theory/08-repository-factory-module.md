# DDD 이론 8: Repository, Factory, Module 패턴

> 영속성 추상화를 위한 Repository 패턴, 복잡한 객체 생성을 캡슐화하는 Factory 패턴,
> 그리고 코드 구조화를 위한 Module 설계 원칙에 대한 종합 레퍼런스 문서입니다.

---

## 1. Repository 패턴

### 1.1 핵심 개념

> "Repository는 특정 타입의 모든 객체를 개념적으로 집합(Set)처럼 표현한다.
> 컬렉션처럼 동작하지만, 더 정교한 쿼리 기능을 제공한다."
>
> — Eric Evans, *Domain-Driven Design*

> "Repository는 Aggregate를 획득하는 수단을 캡슐화한다.
> 오직 Aggregate Root만이 Repository를 통해 직접 접근 가능해야 한다."
>
> — Vaughn Vernon, *Implementing Domain-Driven Design*

Repository 패턴은 도메인 레이어와 데이터 접근 레이어 사이의 중재자 역할을 합니다.
도메인 객체가 마치 인메모리 컬렉션에 있는 것처럼 접근할 수 있게 해주며,
실제 영속성 메커니즘(DB, 파일, 외부 API 등)을 완전히 숨깁니다.

### 1.2 Repository가 필요한 이유

**Repository 없이 (안티패턴):**
- 도메인 로직이 SQL, ORM 등 인프라 기술에 직접 의존
- 테스트 시 실제 DB가 필요
- 영속성 기술 변경 시 도메인 코드 수정 필요
- 도메인 모델의 순수성 훼손

**Repository 사용 시:**
- 도메인 레이어는 영속성 메커니즘을 전혀 모름
- 인메모리 구현으로 빠른 단위 테스트 가능
- 영속성 기술 교체가 자유로움
- 도메인 모델의 순수성 유지

### 1.3 Repository의 핵심 특성

| 특성 | 설명 |
|------|------|
| 컬렉션 인터페이스 | `add`, `remove`, `find` 같은 컬렉션 메서드 제공 |
| Aggregate Root 전용 | 내부 Entity/VO는 Root와 함께 저장/로드 |
| 도메인 레이어에 인터페이스 정의 | 구현은 인프라 레이어에 위치 (DIP) |
| 도메인 객체 반환 | DTO나 Entity가 아닌 완전한 도메인 모델 반환 |

### 1.4 Kotlin 구현 예제

#### 도메인 레이어: Repository 인터페이스

```kotlin
// 도메인 레이어에 정의되는 기본 Repository 인터페이스
interface Repository<T : AggregateRoot<ID>, ID : Any> {
    fun findById(id: ID): T?
    fun save(aggregate: T)
    fun delete(aggregate: T)
    fun exists(id: ID): Boolean
}

// 주문 도메인 전용 Repository
interface OrderRepository : Repository<Order, OrderId> {
    fun findByCustomerId(customerId: CustomerId): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
    fun findPendingOrdersOlderThan(date: LocalDateTime): List<Order>
    fun findAll(page: PageRequest): Page<Order>
    fun countByStatus(status: OrderStatus): Long
}
```

#### Application Service에서의 사용

```kotlin
class OrderApplicationService(
    private val orderRepository: OrderRepository
) {
    fun getOrder(orderId: String): OrderDto {
        val order = orderRepository.findById(OrderId.from(orderId))
            ?: throw OrderNotFoundException(orderId)
        return OrderDto.from(order)
    }

    fun confirmOrder(orderId: String) {
        val order = orderRepository.findById(OrderId.from(orderId))
            ?: throw OrderNotFoundException(orderId)

        order.confirm()

        orderRepository.save(order)
    }
}
```


### 1.5 Repository 구현 전략 비교

| 전략 | 장점 | 단점 | 적합한 경우 |
|------|------|------|-------------|
| ORM 기반 | 생산성, 익숙함 | 임피던스 불일치 | 일반적인 CRUD |
| 순수 SQL | 성능 최적화 | 매핑 코드 많음 | 복잡한 쿼리 |
| Document DB | Aggregate 자연스럽게 저장 | 조인 어려움 | 복잡한 Aggregate |
| Event Sourcing | 완전한 이력 보존 | 복잡성 높음 | 감사 필수 도메인 |

### 1.6 인프라 레이어: JPA 기반 구현 (Kotlin)

```kotlin
@Repository
class JpaOrderRepository(
    private val jpaRepository: SpringDataOrderRepository,
    private val mapper: OrderMapper
) : OrderRepository {

    override fun findById(id: OrderId): Order? {
        return jpaRepository.findByIdOrNull(id.value)?.let { mapper.toDomain(it) }
    }

    override fun save(aggregate: Order) {
        val entity = mapper.toEntity(aggregate)
        jpaRepository.save(entity)
    }

    override fun delete(aggregate: Order) {
        jpaRepository.deleteById(aggregate.id.value)
    }

    override fun exists(id: OrderId): Boolean {
        return jpaRepository.existsById(id.value)
    }

    override fun findByCustomerId(customerId: CustomerId): List<Order> {
        return jpaRepository.findByCustomerId(customerId.value)
            .map { mapper.toDomain(it) }
    }

    override fun findByStatus(status: OrderStatus): List<Order> {
        return jpaRepository.findByStatus(status.name)
            .map { mapper.toDomain(it) }
    }

    override fun findPendingOrdersOlderThan(date: LocalDateTime): List<Order> {
        return jpaRepository.findByStatusAndCreatedAtBefore(
            OrderStatus.PENDING.name, date
        ).map { mapper.toDomain(it) }
    }

    override fun findAll(page: PageRequest): Page<Order> {
        val result = jpaRepository.findAll(page.toPageable())
        return Page(
            content = result.content.map { mapper.toDomain(it) },
            totalElements = result.totalElements,
            pageNumber = result.number,
            pageSize = result.size
        )
    }

    override fun countByStatus(status: OrderStatus): Long {
        return jpaRepository.countByStatus(status.name)
    }
}
```

### 1.7 Mapper 패턴 (도메인 ↔ 영속성 변환)

```kotlin
class OrderMapper {
    fun toDomain(entity: OrderEntity): Order {
        return Order.reconstitute(
            id = OrderId.from(entity.id),
            customerId = CustomerId.from(entity.customerId),
            status = OrderStatus.valueOf(entity.status),
            items = entity.lineItems.map { item ->
                OrderLineItem.create(
                    lineNumber = item.lineNumber,
                    productId = ProductId.from(item.productId),
                    productName = item.productName,
                    quantity = Quantity.of(item.quantity),
                    unitPrice = Money.of(item.unitPrice, Currency.of(item.currency))
                )
            },
            shippingAddress = Address.create(
                street = entity.shippingStreet,
                city = entity.shippingCity,
                zipCode = entity.shippingZipCode
            ),
            totalAmount = Money.of(entity.totalAmount, Currency.KRW),
            createdAt = entity.createdAt,
            version = entity.version
        )
    }

    fun toEntity(order: Order): OrderEntity {
        return OrderEntity(
            id = order.id.value,
            customerId = order.customerId.value,
            status = order.status.name,
            totalAmount = order.totalAmount.amount,
            shippingStreet = order.shippingAddress.street,
            shippingCity = order.shippingAddress.city,
            shippingZipCode = order.shippingAddress.zipCode,
            createdAt = order.createdAt,
            version = order.version,
            lineItems = order.items.map { item ->
                OrderLineItemEntity(
                    orderId = order.id.value,
                    lineNumber = item.lineNumber,
                    productId = item.productId.value,
                    productName = item.productName,
                    quantity = item.quantity.value,
                    unitPrice = item.unitPrice.amount,
                    currency = item.unitPrice.currency.code
                )
            }
        )
    }
}
```

### 1.8 In-Memory Repository (테스트용)

```kotlin
class InMemoryOrderRepository : OrderRepository {
    private val store = mutableMapOf<String, Order>()

    override fun findById(id: OrderId): Order? = store[id.value]

    override fun save(aggregate: Order) {
        store[aggregate.id.value] = aggregate
    }

    override fun delete(aggregate: Order) {
        store.remove(aggregate.id.value)
    }

    override fun exists(id: OrderId): Boolean = store.containsKey(id.value)

    override fun findByCustomerId(customerId: CustomerId): List<Order> =
        store.values.filter { it.customerId == customerId }

    override fun findByStatus(status: OrderStatus): List<Order> =
        store.values.filter { it.status == status }

    override fun findPendingOrdersOlderThan(date: LocalDateTime): List<Order> =
        store.values.filter { it.status == OrderStatus.PENDING && it.createdAt < date }

    override fun findAll(page: PageRequest): Page<Order> {
        val all = store.values.toList()
        val start = page.offset
        val end = minOf(start + page.size, all.size)
        return Page(all.subList(start, end), all.size.toLong(), page.page, page.size)
    }

    override fun countByStatus(status: OrderStatus): Long =
        store.values.count { it.status == status }.toLong()

    // 테스트 헬퍼
    fun clear() = store.clear()
    fun count(): Int = store.size
}
```

### 1.9 Specification 패턴

```kotlin
// 복잡한 쿼리 조건을 도메인 객체로 캡슐화
interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean
    fun toQuery(): QueryCondition
}

class OrderByStatusSpec(private val status: OrderStatus) : Specification<Order> {
    override fun isSatisfiedBy(candidate: Order): Boolean =
        candidate.status == status

    override fun toQuery(): QueryCondition =
        QueryCondition("status", status.name)
}

class OrderByDateRangeSpec(
    private val startDate: LocalDateTime,
    private val endDate: LocalDateTime
) : Specification<Order> {
    override fun isSatisfiedBy(candidate: Order): Boolean =
        candidate.createdAt in startDate..endDate

    override fun toQuery(): QueryCondition =
        QueryCondition("createdAt", BetweenRange(startDate, endDate))
}

// 조합 가능한 And Specification
class AndSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(candidate: T): Boolean =
        left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate)

    override fun toQuery(): QueryCondition =
        left.toQuery().and(right.toQuery())
}

// 사용 예시
val pendingOrdersThisMonth = AndSpecification(
    OrderByStatusSpec(OrderStatus.PENDING),
    OrderByDateRangeSpec(startOfMonth, endOfMonth)
)
val orders = orderRepository.findBySpec(pendingOrdersThisMonth)
```


---

## 2. Factory 패턴

### 2.1 핵심 개념

> "복잡한 객체와 Aggregate의 인스턴스를 생성하는 책임을 별도의 객체에 위임하라.
> 이 객체 자체는 도메인 모델에서 책임이 없지만, 도메인 설계의 일부다."
>
> — Eric Evans, *Domain-Driven Design*

> "Factory는 Aggregate 생성의 복잡성을 캡슐화하며,
> 클라이언트가 Aggregate의 내부 구조를 알 필요 없이 올바른 상태의 객체를 얻을 수 있게 한다."
>
> — Vaughn Vernon, *Implementing Domain-Driven Design*

Factory 패턴은 복잡한 객체 생성 로직을 캡슐화하여, 클라이언트 코드가 생성 과정의
세부사항을 알 필요 없이 올바르게 구성된 도메인 객체를 얻을 수 있게 합니다.

### 2.2 Factory가 필요한 경우

1. **복잡한 Aggregate 생성** — 여러 Entity와 Value Object로 구성된 Aggregate
2. **생성 로직이 도메인 지식을 포함** — 비즈니스 규칙에 따라 다른 객체 생성
3. **외부 서비스 의존** — 생성 시 외부 서비스 호출 필요 (ID 생성, 검증 등)
4. **다형성 객체 생성** — 조건에 따라 다른 타입의 객체 생성

### 2.3 Factory vs 생성자 선택 기준

| 상황 | 권장 방식 |
|------|-----------|
| 단순한 Value Object | 생성자 또는 정적 팩토리 메서드 |
| 단순한 Entity | Aggregate Root의 팩토리 메서드 |
| 복잡한 Aggregate | 별도 Factory 클래스 |
| 외부 서비스 의존 | 별도 Factory 클래스 |
| 다형성 객체 | 별도 Factory 클래스 |

### 2.4 Kotlin 구현 예제

#### 패턴 1: Aggregate Root의 팩토리 메서드

```kotlin
class Order private constructor(val id: OrderId) {

    lateinit var customerId: CustomerId
        private set
    lateinit var shippingAddress: Address
        private set
    var status: OrderStatus = OrderStatus.DRAFT
        private set
    private val _items: MutableList<OrderLineItem> = mutableListOf()
    val items: List<OrderLineItem> get() = _items.toList()
    lateinit var createdAt: LocalDateTime
        private set
    var version: Long = 0
        private set

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()
    val domainEvents: List<DomainEvent> get() = _domainEvents.toList()

    companion object {
        // 새로운 주문 생성용 팩토리 메서드
        fun create(customerId: CustomerId, shippingAddress: Address): Order {
            val order = Order(OrderId.generate())
            order.customerId = customerId
            order.shippingAddress = shippingAddress
            order.status = OrderStatus.DRAFT
            order.createdAt = LocalDateTime.now()
            order._domainEvents.add(OrderCreatedEvent(order.id))
            return order
        }

        // Repository에서 재구성할 때 사용하는 팩토리 메서드
        fun reconstitute(
            id: OrderId,
            customerId: CustomerId,
            status: OrderStatus,
            items: List<OrderLineItem>,
            shippingAddress: Address,
            totalAmount: Money,
            createdAt: LocalDateTime,
            version: Long
        ): Order {
            val order = Order(id)
            order.customerId = customerId
            order.status = status
            order._items.addAll(items)
            order.shippingAddress = shippingAddress
            order.createdAt = createdAt
            order.version = version
            return order
        }
    }

    fun confirm() {
        require(status == OrderStatus.DRAFT) { "DRAFT 상태에서만 확정 가능" }
        require(_items.isNotEmpty()) { "주문 항목이 비어있음" }
        status = OrderStatus.CONFIRMED
        _domainEvents.add(OrderConfirmedEvent(id))
    }
}
```

#### 패턴 2: 별도 Factory 클래스 (복잡한 생성 로직)

```kotlin
class OrderFactory(
    private val productRepository: ProductRepository,
    private val pricingService: PricingService,
    private val inventoryService: InventoryService
) {
    suspend fun createOrder(
        customerId: CustomerId,
        items: List<CreateOrderItemCommand>,
        shippingAddress: Address
    ): Order {
        // 1. 상품 정보 조회
        val productIds = items.map { ProductId.from(it.productId) }
        val products = productRepository.findByIds(productIds)

        // 2. 재고 확인
        items.forEach { item ->
            val available = inventoryService.checkAvailability(
                ProductId.from(item.productId),
                Quantity.of(item.quantity)
            )
            if (!available) {
                throw InsufficientInventoryException(item.productId)
            }
        }

        // 3. 가격 계산 (할인, 프로모션 적용)
        val pricedItems = pricingService.calculatePrices(items, customerId)

        // 4. Order Aggregate 생성
        val order = Order.create(customerId, shippingAddress)

        pricedItems.forEach { pricedItem ->
            val product = products.first { it.id == pricedItem.productId }
            order.addItem(
                productId = pricedItem.productId,
                productName = product.name,
                quantity = pricedItem.quantity,
                unitPrice = pricedItem.unitPrice
            )
        }

        return order
    }
}
```

#### 패턴 3: 다형성 Factory

```kotlin
// 추상 할인 클래스
sealed class Discount {
    abstract fun apply(amount: Money): Money
}

class PercentageDiscount(private val percentage: Int) : Discount() {
    override fun apply(amount: Money): Money =
        amount.multiply(percentage.toDouble() / 100.0)
}

class FixedAmountDiscount(private val fixedAmount: Money) : Discount() {
    override fun apply(amount: Money): Money =
        if (fixedAmount > amount) amount else fixedAmount
}

class BuyOneGetOneDiscount : Discount() {
    override fun apply(amount: Money): Money = amount.multiply(0.5)
}

// 다형성 Factory
class DiscountFactory {
    fun create(coupon: Coupon): Discount = when (coupon.type) {
        CouponType.PERCENTAGE -> PercentageDiscount(coupon.value)
        CouponType.FIXED_AMOUNT -> FixedAmountDiscount(Money.won(coupon.value))
        CouponType.BOGO -> BuyOneGetOneDiscount()
    }
}
```

#### Application Service에서 Factory 사용

```kotlin
class OrderApplicationService(
    private val orderFactory: OrderFactory,
    private val orderRepository: OrderRepository
) {
    suspend fun createOrder(command: CreateOrderCommand): OrderId {
        val order = orderFactory.createOrder(
            customerId = CustomerId.from(command.customerId),
            items = command.items,
            shippingAddress = Address.from(command.shippingAddress)
        )

        orderRepository.save(order)

        return order.id
    }
}
```


---

## 3. Module(패키지) 설계

### 3.1 핵심 개념

> "Module은 관련된 개념들을 그룹화하고, 복잡성을 관리하는 방법이다.
> 좋은 Module은 높은 응집도와 낮은 결합도를 가진다."
>
> — Eric Evans, *Domain-Driven Design*

Module은 코드를 논리적으로 구조화하는 단위입니다. DDD에서 Module은 단순한 기술적
패키지가 아니라, 도메인 개념의 의미 있는 그룹화를 나타냅니다.

### 3.2 Module 설계 4대 원칙

1. **높은 응집도 (High Cohesion)** — 관련된 개념들을 함께 그룹화. Module 내 요소들은 서로 밀접하게 관련
2. **낮은 결합도 (Low Coupling)** — Module 간 의존성 최소화. 다른 Module의 내부 구현에 의존하지 않음
3. **유비쿼터스 언어 반영** — Module 이름은 기술적 용어가 아닌 비즈니스 용어 사용
4. **Bounded Context 정렬** — Module 구조는 Bounded Context와 일치

### 3.3 패키지 구조 패턴 (Kotlin)

#### 패턴 1: 레이어 기반 구조

```
src/main/kotlin/com/example/shop/
├── domain/                        # 도메인 레이어
│   ├── model/
│   │   ├── Order.kt
│   │   ├── OrderLineItem.kt
│   │   └── OrderStatus.kt
│   ├── repository/
│   │   └── OrderRepository.kt    # 인터페이스
│   ├── service/
│   │   └── OrderDomainService.kt
│   └── event/
│       └── OrderCreatedEvent.kt
│
├── application/                   # 애플리케이션 레이어
│   ├── service/
│   │   └── OrderApplicationService.kt
│   ├── command/
│   │   └── CreateOrderCommand.kt
│   └── dto/
│       └── OrderDto.kt
│
├── infrastructure/                # 인프라 레이어
│   ├── persistence/
│   │   ├── JpaOrderRepository.kt
│   │   ├── OrderEntity.kt
│   │   └── OrderMapper.kt
│   └── messaging/
│       └── KafkaEventPublisher.kt
│
└── presentation/                  # 프레젠테이션 레이어
    └── controller/
        └── OrderController.kt
```

- **장점:** 레이어 간 의존성 명확, 익숙한 구조
- **단점:** 기능 추가 시 여러 폴더 수정 필요

#### 패턴 2: 기능(Feature) 기반 구조

```
src/main/kotlin/com/example/shop/
├── order/                         # 주문 기능
│   ├── domain/
│   │   ├── Order.kt
│   │   ├── OrderLineItem.kt
│   │   └── OrderRepository.kt
│   ├── application/
│   │   ├── CreateOrderUseCase.kt
│   │   └── ConfirmOrderUseCase.kt
│   ├── infrastructure/
│   │   └── JpaOrderRepository.kt
│   └── presentation/
│       └── OrderController.kt
│
├── customer/                      # 고객 기능
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── presentation/
│
├── product/                       # 상품 기능
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── presentation/
│
└── shared/                        # 공유 모듈
    ├── domain/
    │   ├── Money.kt
    │   └── Address.kt
    └── infrastructure/
        └── EventPublisher.kt
```

- **장점:** 기능별 응집도 높음, 마이크로서비스 전환 용이
- **단점:** 공유 코드 관리 필요

#### 패턴 3: Hexagonal (Ports & Adapters)

```
src/main/kotlin/com/example/shop/
├── core/                          # 핵심 도메인 (헥사곤 내부)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Order.kt
│   │   │   └── OrderLineItem.kt
│   │   ├── port/
│   │   │   ├── inbound/          # Driving Ports (Use Cases)
│   │   │   │   ├── CreateOrderUseCase.kt
│   │   │   │   └── ConfirmOrderUseCase.kt
│   │   │   └── outbound/         # Driven Ports
│   │   │       ├── OrderRepository.kt
│   │   │       └── PaymentGateway.kt
│   │   └── service/
│   │       └── OrderDomainService.kt
│   └── application/
│       └── service/
│           └── OrderService.kt    # Use Case 구현
│
└── adapter/                       # 어댑터 (헥사곤 외부)
    ├── inbound/                   # Driving Adapters
    │   ├── web/
    │   │   └── OrderController.kt
    │   └── cli/
    │       └── OrderCli.kt
    └── outbound/                  # Driven Adapters
        ├── persistence/
        │   └── JpaOrderRepository.kt
        └── payment/
            └── StripePaymentGateway.kt
```

- **장점:** 도메인 격리 완벽, 테스트 용이, 어댑터 교체 쉬움
- **단점:** 초기 구조 복잡, 학습 곡선 있음

### 3.4 Module 간 의존성 규칙

```
┌─────────────────────────────────────────────────────────┐
│                    의존성 방향 규칙                       │
│                                                         │
│   Presentation → Application → Domain ← Infrastructure │
│                                                         │
│   규칙:                                                 │
│   • 상위 레이어 → 하위 레이어 의존 가능                 │
│   • Domain은 어떤 것에도 의존하지 않음 (순수)           │
│   • Infrastructure → Domain (인터페이스 구현, DIP)      │
│   • 같은 레이어 간 순환 의존 금지                       │
└─────────────────────────────────────────────────────────┘
```

### 3.5 Kotlin에서의 Module 의존성 강제

```kotlin
// build.gradle.kts - 멀티모듈 프로젝트로 의존성 강제
// :domain 모듈 - 외부 의존성 없음
// domain/build.gradle.kts
dependencies {
    // 도메인 모듈은 외부 프레임워크에 의존하지 않음
}

// :application 모듈
// application/build.gradle.kts
dependencies {
    implementation(project(":domain"))
}

// :infrastructure 모듈
// infrastructure/build.gradle.kts
dependencies {
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
}

// :presentation 모듈
// presentation/build.gradle.kts
dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

---

## 4. 실전 가이드라인

### 4.1 Repository 설계 가이드라인

- **하나의 Aggregate Root에 하나의 Repository** — 내부 Entity는 별도 Repository를 갖지 않음
- **쿼리 메서드 이름은 도메인 언어 사용** — `findByStatusCode(1)` ❌ → `findPendingOrders()` ✅
- **복잡한 보고서 쿼리는 CQRS로 분리** — Repository는 명령(Command) 측에 집중
- **낙관적 잠금(Optimistic Locking) 적용** — version 필드로 동시성 제어
- **테스트 시 InMemory 구현 활용** — 빠른 피드백 루프 확보

### 4.2 Factory 설계 가이드라인

- **단순한 객체에는 Factory 불필요** — 과도한 추상화 지양
- **Factory는 불변식(Invariant)을 보장** — 생성된 객체는 항상 유효한 상태
- **reconstitute 메서드 분리** — 새 생성과 DB 재구성은 다른 팩토리 메서드로
- **Factory에서 도메인 이벤트 발행 가능** — 생성 자체가 비즈니스 이벤트일 때

### 4.3 Module 설계 가이드라인

- **팀 규모에 맞는 구조 선택** — 소규모: 레이어 기반, 대규모: 기능 기반/Hexagonal
- **패키지 이름은 비즈니스 용어** — `com.shop.order`, `com.shop.customer` ✅
- **순환 의존 절대 금지** — ArchUnit 등으로 자동 검증
- **shared 모듈은 최소화** — 공유가 많으면 Bounded Context 경계 재검토

### 4.4 Repository vs DAO 차이점

| 관점 | Repository | DAO |
|------|-----------|-----|
| 추상화 대상 | 도메인 객체의 컬렉션 | 데이터 접근 기술 |
| 반환 타입 | Aggregate Root (도메인 객체) | Entity/DTO |
| 단위 | Aggregate 전체 | 단일 테이블/엔티티 |
| 언어 | 도메인 언어 (유비쿼터스 언어) | 기술 언어 |
| 위치 | 인터페이스: 도메인, 구현: 인프라 | 인프라 레이어 |

---

## 5. FAQ

**Q1: Repository에 복잡한 쿼리를 넣어도 되나요?**
도메인 관점의 쿼리는 괜찮습니다. 하지만 보고서용 복잡한 쿼리는 CQRS의 Query 모델로 분리하세요.

**Q2: Factory는 항상 필요한가요?**
아니요. 단순한 객체는 생성자나 정적 팩토리 메서드로 충분합니다. 복잡한 생성 로직이나 외부 의존성이 있을 때만 별도 Factory를 만드세요.

**Q3: Module 구조는 어떻게 결정하나요?**
팀 규모와 프로젝트 복잡도에 따라 결정합니다. 작은 프로젝트는 레이어 기반, 큰 프로젝트는 기능 기반이나 Hexagonal을 고려하세요.

**Q4: In-Memory Repository는 언제 사용하나요?**
단위 테스트에서 사용합니다. 실제 DB 없이 빠르게 테스트할 수 있고, 도메인 로직에 집중할 수 있습니다.

**Q5: Kotlin에서 Repository 인터페이스에 suspend를 붙여야 하나요?**
코루틴 기반 프로젝트라면 `suspend`를 붙이는 것이 자연스럽습니다. Spring WebFlux + R2DBC 환경이라면 필수이고, 전통적인 Spring MVC + JPA 환경이라면 불필요합니다.

---

## 6. 핵심 요약

| 패턴 | 핵심 역할 | 기억할 점 |
|------|-----------|-----------|
| **Repository** | 영속성 추상화, 컬렉션 인터페이스 | Aggregate Root 전용, 인터페이스는 도메인에, 구현은 인프라에 |
| **Factory** | 복잡한 객체 생성 캡슐화 | 불변식 보장, 단순하면 불필요, 다형성/외부의존 시 별도 클래스 |
| **Module** | 코드 구조화, 복잡성 관리 | 높은 응집도, 낮은 결합도, 도메인 언어 반영, 의존성 방향 준수 |

### 세 패턴의 관계

```
┌─────────────────────────────────────────────────────────────┐
│  Module (패키지 구조)                                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Domain Layer                                        │    │
│  │  ┌──────────────┐  ┌──────────────┐                 │    │
│  │  │ Aggregate    │  │ Repository   │ ← 인터페이스    │    │
│  │  │ (Factory로   │  │ Interface    │                 │    │
│  │  │  생성됨)     │  └──────────────┘                 │    │
│  │  └──────────────┘                                    │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Infrastructure Layer                                │    │
│  │  ┌──────────────┐  ┌──────────────┐                 │    │
│  │  │ Repository   │  │ Mapper       │                 │    │
│  │  │ Impl (JPA)   │  │ (변환 로직)  │                 │    │
│  │  └──────────────┘  └──────────────┘                 │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

> "Repository는 특정 타입의 모든 객체를 개념적으로 집합처럼 표현한다."
> — Eric Evans

---

*참고 자료: Eric Evans의 "Domain-Driven Design", Vaughn Vernon의 "Implementing Domain-Driven Design"*
*원본: https://techmentor-avo.pages.dev/theory/ddd-theory-08*
