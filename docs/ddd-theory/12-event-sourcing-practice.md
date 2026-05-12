# DDD 이론 12: Event Sourcing과 DDD 실천

> 참조: https://techmentor-avo.pages.dev/theory/ddd-theory-12

Event Sourcing의 원리와 구현, Event Storming 워크샵, DDD 아키텍처 패턴, 점진적 도입 전략을 다룹니다.

---

## 1. Event Sourcing 원리

> "현재 상태를 저장하는 대신, 상태 변경을 일으킨 이벤트들의 시퀀스를 저장한다.
> 현재 상태는 이벤트들을 재생하여 도출한다." — Martin Fowler

### 1.1 State Sourcing vs Event Sourcing

| 구분 | State Sourcing | Event Sourcing |
|------|---------------|----------------|
| 저장 방식 | 현재 상태만 저장 (UPDATE) | 이벤트 시퀀스 저장 (Append-only) |
| 이력 | 손실됨 | 완전한 감사 로그 보존 |
| 복원 | 현재 상태만 조회 가능 | 임의 시점 상태 재생 가능 |

```
State Sourcing:  Account(id=1, balance=1000) → 어떻게 1000이 되었는지 모름

Event Sourcing:  AccountOpened(initial=0) → Deposited(500) → Deposited(700) → Withdrawn(200)
                 재생: 0 + 500 + 700 - 200 = 1000 (전체 이력 보존)
```

### 1.2 Event Sourcing 구현 (Kotlin)

```kotlin
// 도메인 이벤트 기본 인터페이스
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String
}

// 계좌 관련 이벤트
data class AccountOpened(
    override val aggregateId: String,
    val initialDeposit: Money,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

data class MoneyDeposited(
    override val aggregateId: String,
    val amount: Money,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

data class MoneyWithdrawn(
    override val aggregateId: String,
    val amount: Money,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

data class AccountClosed(
    override val aggregateId: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
```

```kotlin
// Event Sourced Aggregate 기반 클래스
abstract class EventSourcedAggregate {
    private val _uncommittedEvents = mutableListOf<DomainEvent>()
    val uncommittedEvents: List<DomainEvent> get() = _uncommittedEvents.toList()
    var version: Int = 0; private set

    protected fun apply(event: DomainEvent) {
        applyEvent(event)
        _uncommittedEvents.add(event)
    }

    fun loadFromHistory(events: List<DomainEvent>) {
        events.forEach { event ->
            applyEvent(event)
            version++
        }
    }

    fun clearUncommittedEvents() { _uncommittedEvents.clear() }

    protected abstract fun applyEvent(event: DomainEvent)
}
```

```kotlin
// 구체적인 Event Sourced Aggregate
class Account private constructor() : EventSourcedAggregate() {
    lateinit var id: AccountId; private set
    var balance: Money = Money.ZERO; private set
    var status: AccountStatus = AccountStatus.ACTIVE; private set

    companion object {
        fun open(id: AccountId, initialDeposit: Money): Account {
            return Account().also {
                it.apply(AccountOpened(id.value, initialDeposit))
            }
        }
    }

    fun deposit(amount: Money) {
        require(status == AccountStatus.ACTIVE) { "계좌가 활성 상태가 아닙니다" }
        apply(MoneyDeposited(id.value, amount))
    }

    fun withdraw(amount: Money) {
        require(status == AccountStatus.ACTIVE) { "계좌가 활성 상태가 아닙니다" }
        require(balance >= amount) { "잔액이 부족합니다" }
        apply(MoneyWithdrawn(id.value, amount))
    }

    fun close() {
        require(balance == Money.ZERO) { "잔액이 남아있습니다" }
        apply(AccountClosed(id.value))
    }

    override fun applyEvent(event: DomainEvent) {
        when (event) {
            is AccountOpened -> {
                id = AccountId(event.aggregateId)
                balance = event.initialDeposit
                status = AccountStatus.ACTIVE
            }
            is MoneyDeposited -> balance = balance + event.amount
            is MoneyWithdrawn -> balance = balance - event.amount
            is AccountClosed -> status = AccountStatus.CLOSED
        }
    }
}
```

---

## 2. Event Store

```kotlin
// Event Store 인터페이스
interface EventStore {
    suspend fun append(streamId: String, events: List<DomainEvent>, expectedVersion: Int)
    suspend fun getEvents(streamId: String, fromVersion: Int = 0): List<DomainEvent>
    suspend fun getAllEvents(fromPosition: Long = 0, limit: Int = 100): EventPage
}

data class EventPage(val events: List<DomainEvent>, val lastPosition: Long)

// PostgreSQL 기반 Event Store 구현
class PostgresEventStore(private val db: Database) : EventStore {

    override suspend fun append(
        streamId: String,
        events: List<DomainEvent>,
        expectedVersion: Int
    ) {
        db.transaction { tx ->
            // Optimistic Concurrency Check
            val currentVersion = tx.queryScalar<Int>(
                "SELECT COALESCE(MAX(version), 0) FROM events WHERE stream_id = ?",
                streamId
            )
            if (currentVersion != expectedVersion) {
                throw ConcurrencyException(
                    "Expected version $expectedVersion, but was $currentVersion"
                )
            }

            var version = expectedVersion
            events.forEach { event ->
                version++
                tx.execute("""
                    INSERT INTO events (event_id, stream_id, version, event_type, payload, created_at)
                    VALUES (?, ?, ?, ?, ?::jsonb, ?)
                """, event.eventId, streamId, version,
                    event::class.simpleName, serialize(event), event.occurredAt
                )
            }
        }
    }

    override suspend fun getEvents(streamId: String, fromVersion: Int): List<DomainEvent> {
        return db.query("""
            SELECT event_type, payload FROM events
            WHERE stream_id = ? AND version > ?
            ORDER BY version ASC
        """, streamId, fromVersion).map { row ->
            deserialize(row.getString("event_type"), row.getString("payload"))
        }
    }
}
```

### Event Sourcing 장단점

| 장점 | 단점 |
|------|------|
| ✅ 완전한 감사 로그 | ❌ 학습 곡선 높음 |
| ✅ 시간 여행 (특정 시점 복원) | ❌ 이벤트 스키마 진화 복잡 |
| ✅ 이벤트 재생으로 버그 재현 | ❌ 조회 시 재생 비용 (Snapshot 필요) |
| ✅ 새로운 Read Model 추가 용이 | ❌ 삭제 어려움 (GDPR 대응) |
| ✅ 도메인 이벤트 자연스럽게 도출 | ❌ 디버깅 복잡도 증가 |

---

## 3. Snapshot 패턴

이벤트가 많아지면 재생 시간이 길어짐 → Snapshot으로 특정 시점 상태를 저장하여 재생 시작점을 앞당김.

```kotlin
// Snapshot 인터페이스
data class Snapshot(
    val aggregateId: String,
    val version: Int,
    val state: ByteArray,
    val createdAt: Instant
)

interface SnapshotStore {
    suspend fun save(snapshot: Snapshot)
    suspend fun get(aggregateId: String): Snapshot?
}

// Snapshot을 활용한 Repository
class EventSourcedAccountRepository(
    private val eventStore: EventStore,
    private val snapshotStore: SnapshotStore,
    private val snapshotFrequency: Int = 100 // 100 이벤트마다 스냅샷
) {
    suspend fun findById(id: AccountId): Account? {
        val streamId = "account-${id.value}"

        // 1. 스냅샷 조회
        val snapshot = snapshotStore.get(streamId)

        // 2. 스냅샷 이후 이벤트만 조회
        val fromVersion = snapshot?.version ?: 0
        val events = eventStore.getEvents(streamId, fromVersion)

        if (snapshot == null && events.isEmpty()) return null

        // 3. Aggregate 복원
        val account = Account.reconstitute()
        snapshot?.let { account.restoreFromSnapshot(it.state) }
        account.loadFromHistory(events)
        return account
    }

    suspend fun save(account: Account) {
        val streamId = "account-${account.id.value}"
        val events = account.uncommittedEvents
        if (events.isEmpty()) return

        eventStore.append(streamId, events, account.version)

        // 주기적 스냅샷 저장
        val newVersion = account.version + events.size
        if (newVersion % snapshotFrequency == 0) {
            snapshotStore.save(Snapshot(
                aggregateId = streamId,
                version = newVersion,
                state = account.toSnapshot(),
                createdAt = Instant.now()
            ))
        }
        account.clearUncommittedEvents()
    }
}
```

---

## 4. 이벤트 스키마 진화 (Schema Evolution)

이벤트는 불변이므로 스키마 변경 시 Upcaster 패턴을 사용하여 구버전 → 신버전으로 변환합니다.

```kotlin
// 이벤트 버전 관리
interface VersionedEvent {
    val schemaVersion: Int
}

// V1: 초기 버전 (단일 통화 가정)
data class MoneyDepositedV1(
    override val aggregateId: String,
    val amount: Long, // 단일 통화
    override val schemaVersion: Int = 1
) : DomainEvent, VersionedEvent

// V2: 다중 통화 지원
data class MoneyDepositedV2(
    override val aggregateId: String,
    val amount: Long,
    val currency: String, // 새 필드
    override val schemaVersion: Int = 2
) : DomainEvent, VersionedEvent

// Upcaster: 구버전 → 신버전 변환
interface EventUpcaster {
    fun canUpcast(eventType: String, version: Int): Boolean
    fun upcast(event: Map<String, Any>): Map<String, Any>
}

class MoneyDepositedV1ToV2Upcaster : EventUpcaster {
    override fun canUpcast(eventType: String, version: Int) =
        eventType == "MoneyDeposited" && version == 1

    override fun upcast(event: Map<String, Any>): Map<String, Any> =
        event + mapOf("currency" to "KRW", "schemaVersion" to 2)
}

// Upcasting을 적용하는 Event Store 래퍼
class UpcastingEventStore(
    private val inner: EventStore,
    private val upcasters: List<EventUpcaster>
) : EventStore by inner {

    override suspend fun getEvents(streamId: String, fromVersion: Int): List<DomainEvent> {
        return inner.getEvents(streamId, fromVersion).map { event ->
            upcasters.fold(event) { current, upcaster -> applyUpcaster(current, upcaster) }
        }
    }
}
```

---

## 5. Event Storming 방법론

> "Event Storming은 복잡한 비즈니스 도메인을 빠르게 탐색하는 워크샵 기반 방법이다." — Alberto Brandolini

### 5.1 진행 단계

| 단계 | 활동 | 포스트잇 색상 | 작성 형태 |
|------|------|--------------|-----------|
| 1 | Domain Events 나열 | 🟠 오렌지 | 과거형 ("주문이 생성되었다") |
| 2 | Commands 식별 | 🔵 파란색 | 명령형 ("주문하기") |
| 3 | Aggregates 식별 | 🟡 노란색 | 명사 ("주문", "결제") |
| 4 | Bounded Context 경계 그리기 | — | 관련 Aggregate 묶기 |

### 5.2 Event Storming 결과 예시

```
[Order Context]
  고객 → [주문하기] → 《주문》 → (주문이 생성되었다)
                        ↓
         [주문 확정하기] → 《주문》 → (주문이 확정되었다)

[Payment Context]
  (주문이 생성되었다) → [결제하기] → 《결제》 → (결제가 완료되었다) / (결제가 실패했다)

[Shipping Context]
  (결제가 완료되었다) → [배송 시작하기] → 《배송》 → (배송이 시작되었다) → (배송이 완료되었다)
```

### 5.3 Event Storming 팁

**✅ Do:**
- 도메인 전문가 반드시 참여
- 큰 벽면/화이트보드 사용
- 포스트잇 색상 규칙 준수
- 질문/문제점은 빨간 포스트잇으로 표시
- 타임박싱 (2~4시간)

**❌ Don't:**
- 기술적 세부사항에 빠지지 않기
- 완벽함 추구하지 않기
- 한 사람이 독점하지 않기
- 처음부터 코드 생각하지 않기

---

## 6. Hexagonal / Clean Architecture와 DDD

### 6.1 Hexagonal Architecture (Ports & Adapters)

```
                 ┌──────────────────────────────┐
  REST API ─────▶│   Inbound Ports (Driving)    │
  GraphQL ──────▶│                              │
  CLI ──────────▶│      ┌──────────────┐        │
                 │      │   Domain     │        │
                 │      │ (Core Logic) │        │
                 │      └──────────────┘        │
                 │                              │
                 │   Outbound Ports (Driven)    │
                 └──────────┬───────────────────┘
                            │
              ┌─────────────┼─────────────────┐
              ▼             ▼                 ▼
         PostgreSQL      Redis            Kafka
```

### 6.2 Kotlin 프로젝트 구조

```
src/main/kotlin/
├── domain/                      # 핵심 도메인 (의존성 없음)
│   ├── order/
│   │   ├── Order.kt            # Aggregate
│   │   ├── OrderItem.kt        # Entity
│   │   ├── OrderStatus.kt      # Value Object
│   │   └── events/
│   │       └── OrderCreated.kt
│   └── shared/
│       └── Money.kt
├── application/                 # 유스케이스 (Port 정의)
│   ├── port/
│   │   ├── inbound/            # Driving Ports
│   │   │   └── CreateOrderUseCase.kt
│   │   └── outbound/           # Driven Ports
│   │       ├── OrderRepository.kt
│   │       └── PaymentGateway.kt
│   └── service/
│       └── CreateOrderService.kt
└── infrastructure/              # Adapters (구현체)
    ├── inbound/
    │   └── rest/
    │       └── OrderController.kt
    └── outbound/
        ├── persistence/
        │   └── JpaOrderRepository.kt
        └── messaging/
            └── KafkaEventPublisher.kt
```

### 6.3 Kotlin 구현 예시

```kotlin
// Inbound Port
interface CreateOrderUseCase {
    suspend fun execute(command: CreateOrderCommand): OrderId
}

data class CreateOrderCommand(val customerId: String, val items: List<OrderItemDto>)

// Outbound Port
interface OrderRepository {
    suspend fun save(order: Order)
    suspend fun findById(id: OrderId): Order?
}

// Application Service (Port 구현)
class CreateOrderService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher
) : CreateOrderUseCase {

    override suspend fun execute(command: CreateOrderCommand): OrderId {
        val order = Order.create(
            customerId = CustomerId(command.customerId),
            items = command.items.map { it.toDomain() }
        )
        orderRepository.save(order)
        order.domainEvents.forEach { eventPublisher.publish(it) }
        return order.id
    }
}

// Infrastructure Adapter
class JpaOrderRepository(private val jpaRepo: SpringDataOrderRepo) : OrderRepository {
    override suspend fun save(order: Order) = jpaRepo.save(order.toEntity())
    override suspend fun findById(id: OrderId) = jpaRepo.findById(id.value)?.toDomain()
}
```

### 6.4 아키텍처 비교

| 특성 | Layered | Hexagonal | Clean |
|------|---------|-----------|-------|
| 의존성 방향 | 위 → 아래 | 바깥 → 안 | 바깥 → 안 |
| 도메인 위치 | 중간 레이어 | 중심 | 중심 |
| 인프라 교체 | 어려움 | 쉬움 | 쉬움 |
| 테스트 용이성 | 보통 | 높음 | 높음 |

**의존성 규칙**: 안쪽 레이어는 바깥쪽 레이어를 알지 못함. 의존성은 항상 안쪽을 향함.

---

## 7. DDD 점진적 도입 6단계

| 단계 | 활동 | 효과 |
|------|------|------|
| **1단계** | Ubiquitous Language 구축 | 도메인 전문가와 용어 통일, 코드에 반영 |
| **2단계** | Value Object 도입 | Primitive Obsession 제거, 타입 안전성 확보 |
| **3단계** | Entity와 Aggregate 식별 | 핵심 도메인부터 불변식 정의, 작게 시작 |
| **4단계** | Repository 패턴 적용 | 인프라 분리, 테스트 용이성 확보 |
| **5단계** | Domain Events 도입 | Aggregate 간 느슨한 결합, 비동기 처리 |
| **6단계** | CQRS / Event Sourcing (선택) | 필요한 경우에만, 복잡도 대비 이점 평가 |

```kotlin
// 1단계: Ubiquitous Language → 코드에 반영
// Bad: fun process(data: Map<String, Any>)
// Good:
fun placeOrder(command: PlaceOrderCommand): OrderConfirmation

// 2단계: Value Object 도입
@JvmInline value class Email(val value: String) {
    init { require(value.contains("@")) { "유효하지 않은 이메일" } }
}

@JvmInline value class Money(val amount: BigDecimal) {
    operator fun plus(other: Money) = Money(amount + other.amount)
    operator fun minus(other: Money) = Money(amount - other.amount)
    operator fun compareTo(other: Money) = amount.compareTo(other.amount)
}

// 3단계: Aggregate
class Order private constructor(val id: OrderId) {
    private val _items = mutableListOf<OrderItem>()
    val items: List<OrderItem> get() = _items.toList()
    var status: OrderStatus = OrderStatus.DRAFT; private set

    fun addItem(product: ProductId, quantity: Int, price: Money) {
        require(status == OrderStatus.DRAFT) { "확정된 주문은 수정 불가" }
        _items.add(OrderItem(product, quantity, price))
    }

    fun confirm() {
        require(_items.isNotEmpty()) { "빈 주문은 확정 불가" }
        status = OrderStatus.CONFIRMED
    }
}
```

---

## 8. 레거시 시스템 패턴

### 8.1 Strangler Fig 패턴

레거시 시스템을 점진적으로 교체. 새 기능은 DDD로 구현하고, 기존 기능을 하나씩 마이그레이션.

```kotlin
// API Gateway에서 라우팅으로 점진적 전환
class OrderRoutingGateway(
    private val legacyOrderService: LegacyOrderService,
    private val newOrderService: CreateOrderUseCase,
    private val featureFlag: FeatureFlag
) {
    suspend fun createOrder(request: OrderRequest): OrderResponse {
        return if (featureFlag.isEnabled("new-order-service")) {
            // 새 DDD 기반 서비스로 라우팅
            val orderId = newOrderService.execute(request.toCommand())
            OrderResponse(orderId.value)
        } else {
            // 레거시 서비스 유지
            legacyOrderService.create(request)
        }
    }
}
```

### 8.2 Anti-Corruption Layer (ACL)

레거시 모델이 새 도메인 모델을 오염시키지 않도록 번역 계층 구축.

```kotlin
// ACL: 레거시 모델 → 새 도메인 모델 변환
class LegacyOrderTranslator {
    fun toDomain(legacyOrder: LegacyOrderDto): Order {
        return Order.reconstitute(
            id = OrderId(legacyOrder.orderNo.toString()),
            items = legacyOrder.lines.map { line ->
                OrderItem(
                    productId = ProductId(line.itemCode),
                    quantity = line.qty,
                    price = Money(BigDecimal(line.unitPrice))
                )
            },
            status = mapStatus(legacyOrder.statusCode)
        )
    }

    private fun mapStatus(code: Int): OrderStatus = when (code) {
        0 -> OrderStatus.DRAFT
        1 -> OrderStatus.CONFIRMED
        9 -> OrderStatus.CANCELLED
        else -> OrderStatus.UNKNOWN
    }
}
```

### 8.3 Bubble Context

레거시 내에 작은 DDD 영역을 생성하고, 성공 사례를 만들어 점진적으로 확장.

```kotlin
// Bubble Context: 레거시 시스템 내 독립적 DDD 영역
// 새로운 "프로모션" 기능을 Bubble Context로 구현
class PromotionBubbleContext(
    private val promotionRepository: PromotionRepository,
    private val legacyAdapter: LegacySystemAdapter // ACL 역할
) {
    suspend fun applyPromotion(orderId: String, promoCode: String): DiscountResult {
        val promotion = promotionRepository.findByCode(promoCode)
            ?: throw PromotionNotFoundException(promoCode)

        // 레거시에서 주문 정보 가져오기 (ACL 통해)
        val orderInfo = legacyAdapter.getOrderInfo(orderId)

        val discount = promotion.calculateDiscount(orderInfo.totalAmount)
        promotion.markUsed()
        promotionRepository.save(promotion)

        // 레거시에 할인 결과 전달
        legacyAdapter.applyDiscount(orderId, discount)
        return DiscountResult(orderId, discount)
    }
}
```

---

## 9. DDD 적용 체크리스트

| # | 항목 | 확인 |
|---|------|------|
| 1 | 도메인 전문가와 정기적으로 대화하고 있는가? | ☐ |
| 2 | Ubiquitous Language가 코드에 반영되어 있는가? | ☐ |
| 3 | Bounded Context 경계가 명확한가? | ☐ |
| 4 | Aggregate가 불변식을 보호하고 있는가? | ☐ |
| 5 | 도메인 로직이 도메인 레이어에 있는가? (서비스/컨트롤러 아님) | ☐ |
| 6 | 인프라 의존성이 도메인에서 분리되어 있는가? | ☐ |
| 7 | 도메인 모델이 테스트 가능한가? (외부 의존 없이) | ☐ |
| 8 | 팀이 DDD 개념을 이해하고 있는가? | ☐ |

---

## 10. DDD 시리즈 총정리

| 세션 | 주제 | 핵심 개념 |
|------|------|-----------|
| 01 | DDD 소개 | 복잡성 관리, 전략적/전술적 설계 |
| 02 | Ubiquitous Language | 공통 언어, 도메인 전문가 협업 |
| 03 | 도메인과 서브도메인 | Core/Supporting/Generic 도메인 |
| 04 | Bounded Context | 컨텍스트 경계, 모델 분리 |
| 05 | Context Mapping | 컨텍스트 간 관계, 통합 패턴 |
| 06 | Entity와 Value Object | 식별성, 불변성, Primitive Obsession |
| 07 | Aggregate | 일관성 경계, Aggregate Root |
| 08 | Repository, Factory, Module | 영속성 추상화, 생성 패턴 |
| 09 | Service와 Supple Design | Domain/Application Service |
| 10 | Domain Events | 이벤트 발행, Saga, Outbox 패턴 |
| 11 | CQRS | Command/Query 분리, Read Model |
| 12 | Event Sourcing과 실천 | 이벤트 저장, Event Storming, 아키텍처 |

> "DDD는 기술이 아니라 사고방식이다. 도메인을 이해하고, 그 이해를 코드에 반영하는 것이 핵심이다." — Eric Evans
