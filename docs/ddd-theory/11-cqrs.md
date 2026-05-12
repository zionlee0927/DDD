# DDD 이론 11: CQRS (Command Query Responsibility Segregation)

> Command와 Query의 책임을 분리하여 읽기와 쓰기를 독립적으로 최적화하는 아키텍처 패턴

---

## 1. CQS vs CQRS

### 1.1 CQS (Command Query Separation) — 메서드 수준 분리

> "모든 메서드는 상태를 변경하는 Command이거나, 데이터를 반환하는 Query여야 한다. 둘 다 해서는 안 된다."
> — Bertrand Meyer

하나의 객체 내에서 Command와 Query 메서드를 구분하는 원칙이다.

```kotlin
class Account {
    private var balance: Money = Money.ZERO

    // Command: 상태 변경, Unit 반환
    fun deposit(amount: Money) {
        require(amount.isPositive()) { "입금액은 양수여야 합니다" }
        balance = balance.add(amount)
    }

    // Query: 상태 조회, 값 반환
    fun getBalance(): Money = balance
}
```

### 1.2 CQRS (Command Query Responsibility Segregation) — 아키텍처 수준 분리

읽기와 쓰기를 완전히 다른 모델로 분리하는 아키텍처 패턴이다.

```kotlin
// Write Model (Command 측) - 도메인 로직에 집중
class OrderAggregate(
    val id: OrderId,
    private var status: OrderStatus,
    private val items: MutableList<OrderItem>,
    private val events: MutableList<DomainEvent> = mutableListOf()
) {
    fun confirm() {
        require(status == OrderStatus.CREATED) { "생성 상태에서만 확인 가능" }
        status = OrderStatus.CONFIRMED
        events.add(OrderConfirmedEvent(id, Instant.now()))
    }

    fun domainEvents(): List<DomainEvent> = events.toList()
    fun clearEvents() = events.clear()
}

// Read Model (Query 측) - 조회 성능에 최적화
data class OrderSummaryView(
    val orderId: String,
    val customerName: String,
    val status: String,
    val itemCount: Int,
    val totalAmount: BigDecimal,
    val createdAt: Instant
)

data class OrderDetailView(
    val orderId: String,
    val customer: CustomerInfo,
    val items: List<OrderItemInfo>,
    val shippingAddress: AddressInfo,
    val status: String,
    val totalAmount: BigDecimal,
    val createdAt: Instant,
    val confirmedAt: Instant? = null
)
```

### 1.3 핵심 차이점

| 구분 | CQS | CQRS |
|------|-----|------|
| 적용 범위 | 메서드 수준 | 아키텍처 수준 |
| 모델 | 단일 모델 | Write/Read 모델 분리 |
| 저장소 | 동일 DB | 분리 가능 |
| 복잡도 | 낮음 | 높음 |
| 확장성 | 제한적 | 독립적 스케일링 |

---

## 2. CQRS 4단계 (Levels of CQRS)

### Level 1: 코드 분리 (같은 DB, 핸들러만 분리)

```kotlin
// Command Handler
class CreateOrderHandler(
    private val repository: OrderRepository
) {
    fun handle(command: CreateOrderCommand) {
        val order = Order.create(command.customerId, command.items)
        repository.save(order)
    }
}

// Query Handler - 같은 DB에서 조회
class GetOrderHandler(
    private val repository: OrderRepository
) {
    fun handle(query: GetOrderByIdQuery): OrderDetailDto {
        val order = repository.findById(query.orderId)
            ?: throw OrderNotFoundException(query.orderId)
        return order.toDetailDto()
    }
}
```

### Level 2: 모델 분리 (같은 DB, Write/Read 모델 분리)

```kotlin
// Write Model - 정규화된 테이블
@Entity
@Table(name = "orders")
class OrderEntity(
    @Id val id: UUID,
    val customerId: UUID,
    @Enumerated(EnumType.STRING) var status: OrderStatus,
    val createdAt: Instant
)

// Read Model - 비정규화된 뷰 테이블
@Entity
@Table(name = "order_summary_view")
class OrderSummaryEntity(
    @Id val orderId: UUID,
    val customerName: String,
    val status: String,
    val itemCount: Int,
    val totalAmount: BigDecimal,
    val createdAt: Instant
)
```

### Level 3: DB 분리 (Write DB + Read DB, 동기화 필요)

```kotlin
// Write 측: PostgreSQL (정규화, 트랜잭션)
class OrderWriteRepository(
    private val jdbcTemplate: JdbcTemplate // PostgreSQL
) {
    fun save(order: OrderAggregate) { /* ... */ }
}

// Read 측: Elasticsearch (검색 최적화)
class OrderReadRepository(
    private val elasticsearchClient: ElasticsearchClient
) {
    fun search(query: SearchOrdersQuery): List<OrderSummaryView> {
        return elasticsearchClient.search(
            SearchRequest.of { s ->
                s.index("orders")
                    .query { q -> q.bool { b ->
                        query.status?.let { b.filter { f -> f.term { t -> t.field("status").value(it) } } }
                        b
                    }}
                    .from((query.page - 1) * query.pageSize)
                    .size(query.pageSize)
            },
            OrderSummaryView::class.java
        ).hits().hits().mapNotNull { it.source() }
    }
}
```

### Level 4: Event Sourcing + Projection

```kotlin
// 이벤트 저장소에서 이벤트를 저장하고, Projection으로 Read Model 구축
class EventSourcedOrderRepository(
    private val eventStore: EventStore
) {
    fun save(aggregate: OrderAggregate) {
        val events = aggregate.domainEvents()
        eventStore.append(aggregate.id.value, events)
        aggregate.clearEvents()
    }

    fun load(orderId: OrderId): OrderAggregate {
        val events = eventStore.getEvents(orderId.value)
        return OrderAggregate.reconstitute(events)
    }
}
```

---

## 3. Write/Read Model 분리

### 3.1 Write Model — Aggregate 중심

```kotlin
class OrderAggregate private constructor(
    val id: OrderId,
    private var customerId: CustomerId,
    private var status: OrderStatus,
    private val items: MutableList<OrderItem>,
    private var shippingAddress: Address,
    private val events: MutableList<DomainEvent> = mutableListOf()
) {
    companion object {
        fun create(
            customerId: CustomerId,
            items: List<OrderItemRequest>,
            shippingAddress: Address
        ): OrderAggregate {
            require(items.isNotEmpty()) { "주문 항목은 최소 1개 이상" }

            val orderId = OrderId.generate()
            val orderItems = items.map { OrderItem.create(it) }
            val aggregate = OrderAggregate(orderId, customerId, OrderStatus.CREATED, orderItems.toMutableList(), shippingAddress)

            aggregate.events.add(
                OrderCreatedEvent(
                    orderId = orderId.value,
                    customerId = customerId.value,
                    items = orderItems.map { it.toSnapshot() },
                    totalAmount = aggregate.calculateTotal(),
                    occurredAt = Instant.now()
                )
            )
            return aggregate
        }
    }

    fun confirm() {
        check(status == OrderStatus.CREATED) { "확인 불가 상태: $status" }
        status = OrderStatus.CONFIRMED
        events.add(OrderConfirmedEvent(id.value, Instant.now()))
    }

    fun cancel(reason: String) {
        check(status in listOf(OrderStatus.CREATED, OrderStatus.CONFIRMED)) { "취소 불가 상태: $status" }
        status = OrderStatus.CANCELLED
        events.add(OrderCancelledEvent(id.value, reason, Instant.now()))
    }

    private fun calculateTotal(): BigDecimal =
        items.sumOf { it.unitPrice.amount * it.quantity.value.toBigDecimal() }
}
```

### 3.2 Read Model — 화면/API 최적화

```kotlin
// 목록 조회용 (간결한 정보)
data class OrderSummaryDto(
    val orderId: String,
    val customerName: String,
    val status: String,
    val itemCount: Int,
    val totalAmount: BigDecimal,
    val createdAt: Instant
)

// 상세 조회용 (풍부한 정보)
data class OrderDetailDto(
    val orderId: String,
    val customer: CustomerInfo,
    val items: List<OrderItemDto>,
    val shippingAddress: AddressDto,
    val status: String,
    val totalAmount: BigDecimal,
    val createdAt: Instant,
    val confirmedAt: Instant?,
    val shippedAt: Instant?,
    val timeline: List<OrderTimelineEntry>
)

// 대시보드용 (집계 정보)
data class CustomerOrderStatsDto(
    val customerId: String,
    val totalOrders: Int,
    val totalSpent: BigDecimal,
    val cancelledOrders: Int,
    val averageOrderAmount: BigDecimal
)
```

---

## 4. Command Bus / Query Bus

### 4.1 Command Bus 구현

```kotlin
// Command 인터페이스
interface Command

data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemRequest>,
    val shippingAddress: AddressRequest
) : Command

data class ConfirmOrderCommand(val orderId: String) : Command
data class CancelOrderCommand(val orderId: String, val reason: String) : Command

// Command Handler 인터페이스
interface CommandHandler<T : Command> {
    fun handle(command: T)
}

// Command Bus
class CommandBus {
    private val handlers = mutableMapOf<Class<*>, CommandHandler<*>>()

    fun <T : Command> register(commandClass: Class<T>, handler: CommandHandler<T>) {
        handlers[commandClass] = handler
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Command> dispatch(command: T) {
        val handler = handlers[command::class.java] as? CommandHandler<T>
            ?: throw IllegalStateException("No handler for ${command::class.simpleName}")
        handler.handle(command)
    }
}
```

### 4.2 Query Bus 구현

```kotlin
// Query 인터페이스
interface Query<R>

data class GetOrderByIdQuery(val orderId: String) : Query<OrderDetailDto>
data class SearchOrdersQuery(
    val status: String? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val page: Int = 1,
    val pageSize: Int = 20
) : Query<PaginatedResult<OrderSummaryDto>>

// Query Handler 인터페이스
interface QueryHandler<Q : Query<R>, R> {
    fun handle(query: Q): R
}

// Query Bus
class QueryBus {
    private val handlers = mutableMapOf<Class<*>, QueryHandler<*, *>>()

    fun <Q : Query<R>, R> register(queryClass: Class<Q>, handler: QueryHandler<Q, R>) {
        handlers[queryClass] = handler
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> dispatch(query: Query<R>): R {
        val handler = handlers[query::class.java] as? QueryHandler<Query<R>, R>
            ?: throw IllegalStateException("No handler for ${query::class.simpleName}")
        return handler.handle(query)
    }
}
```

### 4.3 Controller 통합

```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus
) {
    // Command: POST, PUT, DELETE
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun createOrder(@RequestBody request: CreateOrderRequest) {
        commandBus.dispatch(CreateOrderCommand(request.customerId, request.items, request.shippingAddress))
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun confirmOrder(@PathVariable id: String) {
        commandBus.dispatch(ConfirmOrderCommand(id))
    }

    // Query: GET
    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: String): OrderDetailDto {
        return queryBus.dispatch(GetOrderByIdQuery(id))
    }

    @GetMapping
    fun searchOrders(
        @RequestParam status: String?,
        @RequestParam page: Int?,
        @RequestParam pageSize: Int?
    ): PaginatedResult<OrderSummaryDto> {
        return queryBus.dispatch(SearchOrdersQuery(status = status, page = page ?: 1, pageSize = pageSize ?: 20))
    }
}
```

---

## 5. Projection (이벤트 → Read Model 변환)

### 5.1 Projector 구현

```kotlin
interface Projector {
    fun project(event: DomainEvent)
}

@Component
class OrderReadModelProjector(
    private val jdbcTemplate: JdbcTemplate
) : Projector {

    @EventHandler
    fun on(event: OrderCreatedEvent) {
        jdbcTemplate.update("""
            INSERT INTO order_read_model (id, customer_id, status, total_amount, item_count, created_at)
            VALUES (?, ?, 'CREATED', ?, ?, ?)
        """, event.orderId, event.customerId, event.totalAmount, event.items.size, event.occurredAt)
    }

    @EventHandler
    fun on(event: OrderConfirmedEvent) {
        jdbcTemplate.update("""
            UPDATE order_read_model SET status = 'CONFIRMED', confirmed_at = ? WHERE id = ?
        """, event.occurredAt, event.orderId)
    }

    @EventHandler
    fun on(event: OrderCancelledEvent) {
        jdbcTemplate.update("""
            UPDATE order_read_model SET status = 'CANCELLED', cancellation_reason = ? WHERE id = ?
        """, event.reason, event.orderId)
    }

    override fun project(event: DomainEvent) {
        when (event) {
            is OrderCreatedEvent -> on(event)
            is OrderConfirmedEvent -> on(event)
            is OrderCancelledEvent -> on(event)
        }
    }
}
```

### 5.2 다중 Read Model Projector

```kotlin
@Component
class CustomerOrderStatsProjector(
    private val jdbcTemplate: JdbcTemplate
) {
    @EventHandler
    fun on(event: OrderCreatedEvent) {
        jdbcTemplate.update("""
            INSERT INTO customer_order_stats (customer_id, total_orders, total_spent)
            VALUES (?, 1, ?)
            ON CONFLICT (customer_id) DO UPDATE SET
                total_orders = customer_order_stats.total_orders + 1,
                total_spent = customer_order_stats.total_spent + ?
        """, event.customerId, event.totalAmount, event.totalAmount)
    }

    @EventHandler
    fun on(event: OrderCancelledEvent) {
        jdbcTemplate.update("""
            UPDATE customer_order_stats SET
                cancelled_orders = cancelled_orders + 1,
                total_orders = total_orders - 1
            WHERE customer_id = (SELECT customer_id FROM order_read_model WHERE id = ?)
        """, event.orderId)
    }
}
```

---

## 6. Read Model 재구축 (Rebuild)

Read Model에 버그가 있거나 새로운 뷰가 필요할 때, 이벤트를 재생하여 재구축한다.

### 6.1 전체 재구축

```kotlin
@Component
class ReadModelRebuilder(
    private val eventStore: EventStore,
    private val projectors: List<Projector>,
    private val jdbcTemplate: JdbcTemplate
) {
    fun rebuild(readModelName: String) {
        logger.info("Rebuilding $readModelName...")

        // 1. 기존 Read Model 삭제
        jdbcTemplate.execute("TRUNCATE TABLE $readModelName")

        // 2. 모든 이벤트를 배치로 재생
        var processedCount = 0L
        val batchSize = 1000
        var lastEventId: String? = null

        while (true) {
            val events = eventStore.getEventsAfter(lastEventId, batchSize)
            if (events.isEmpty()) break

            events.forEach { event ->
                projectors.forEach { it.project(event) }
                processedCount++
            }

            lastEventId = events.last().eventId
            logger.info("Processed $processedCount events...")
        }

        logger.info("Rebuild complete. Total: $processedCount events")
    }
}
```

### 6.2 무중단 재구축 (Zero-Downtime Rebuild)

```kotlin
class ZeroDowntimeRebuilder(
    private val eventStore: EventStore,
    private val jdbcTemplate: JdbcTemplate
) {
    fun rebuildWithSwap(readModelName: String, projector: Projector) {
        val newTable = "${readModelName}_new"

        // 1. 새 테이블 생성 (기존 스키마 복제)
        jdbcTemplate.execute("CREATE TABLE $newTable (LIKE $readModelName INCLUDING ALL)")

        // 2. 새 테이블에 Projection 수행
        eventStore.streamAllEvents().forEach { event -> projector.project(event) }

        // 3. 원자적 테이블 스왑
        jdbcTemplate.execute("""
            BEGIN;
            ALTER TABLE $readModelName RENAME TO ${readModelName}_old;
            ALTER TABLE $newTable RENAME TO $readModelName;
            COMMIT;
        """)

        // 4. 이전 테이블 삭제
        jdbcTemplate.execute("DROP TABLE IF EXISTS ${readModelName}_old")
    }
}
```

---

## 7. Eventual Consistency (최종 일관성) 처리

비동기 동기화 시 Write 후 Read Model이 즉시 갱신되지 않는 문제를 처리하는 전략들.

### 7.1 Optimistic UI (낙관적 UI 갱신)

클라이언트에서 Command 성공 시 로컬 상태를 즉시 갱신하고, 이후 서버 확인을 받는다.

```kotlin
// 서버 측: Command 성공 시 생성된 ID 반환
@PostMapping("/orders")
fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<CommandResult> {
    val orderId = OrderId.generate()
    commandBus.dispatch(CreateOrderCommand(orderId.value, request.customerId, request.items))
    return ResponseEntity.accepted().body(CommandResult(orderId = orderId.value))
}

data class CommandResult(val orderId: String)
```

### 7.2 Polling 기반 대기

```kotlin
@Component
class ReadModelAwaiter(
    private val queryBus: QueryBus
) {
    suspend fun <R> awaitReadModel(
        query: Query<R>,
        maxWaitMs: Long = 5000,
        intervalMs: Long = 100
    ): R {
        val deadline = System.currentTimeMillis() + maxWaitMs

        while (System.currentTimeMillis() < deadline) {
            try {
                return queryBus.dispatch(query)
            } catch (e: NotFoundException) {
                delay(intervalMs)
            }
        }
        throw TimeoutException("Read model not available within ${maxWaitMs}ms")
    }
}
```

### 7.3 버전 기반 일관성 확인

```kotlin
// Command 결과로 버전 반환
data class CommandResult(val aggregateId: String, val version: Long)

// Query 시 최소 버전 요구
data class GetOrderQuery(
    val orderId: String,
    val minVersion: Long? = null // null이면 최신 Read Model 사용
) : Query<OrderDetailDto>

class GetOrderQueryHandler(
    private val readRepository: OrderReadRepository
) : QueryHandler<GetOrderQuery, OrderDetailDto> {
    override fun handle(query: GetOrderQuery): OrderDetailDto {
        val result = readRepository.findById(query.orderId)
            ?: throw OrderNotFoundException(query.orderId)

        // 버전이 아직 반영되지 않았으면 대기 또는 Write Model에서 직접 조회
        if (query.minVersion != null && result.version < query.minVersion) {
            throw StaleReadModelException("Expected version >= ${query.minVersion}, got ${result.version}")
        }
        return result
    }
}
```

### 7.4 일관성 지연 일반 가이드

| 전략 | 적용 시점 | 장점 | 단점 |
|------|-----------|------|------|
| Optimistic UI | 대부분의 UI | 즉각적 UX | 실패 시 롤백 필요 |
| Polling | Command 직후 조회 | 구현 간단 | 지연 발생 |
| 버전 기반 | 정확성 필요 시 | 정확한 일관성 | 복잡도 증가 |
| Write Model 직접 조회 | 즉시 필요 시 | 강한 일관성 | CQRS 이점 감소 |

---

## 8. CQRS 적용 판단 기준

### ✅ 적합한 경우

- **읽기/쓰기 비율 불균형**: 읽기가 쓰기보다 10:1 이상 많은 경우
- **복잡한 조회 요구사항**: 다양한 뷰, 집계, 전문 검색이 필요한 경우
- **성능 요구사항 차이**: 읽기는 밀리초 응답, 쓰기는 정확성 우선
- **팀 분리**: 읽기/쓰기 팀이 독립적으로 개발해야 하는 경우
- **독립적 확장**: 읽기/쓰기를 별도로 스케일링해야 하는 경우
- **Event Sourcing 도입 시**: 자연스러운 조합

### ❌ 부적합한 경우

- **단순한 CRUD**: 복잡한 비즈니스 로직이 없는 경우
- **강한 일관성 필수**: 금융 거래 등 즉시 일관성이 반드시 필요한 경우
- **작은 팀/프로젝트**: 복잡도 대비 이점이 적은 경우
- **읽기/쓰기 균형**: 비율이 비슷한 경우
- **프로토타입/MVP**: 빠른 개발이 우선인 경우

### 트레이드오프 요약

| 장점 | 단점 |
|------|------|
| 독립적 최적화 | 복잡도 증가 |
| 독립적 확장 | 최종 일관성 처리 필요 |
| 읽기 성능 향상 | 데이터 중복 |
| 유연한 Read Model | 동기화 로직 필요 |
| 팀 독립성 | 운영 복잡도 증가 |

---

## 9. 점진적 도입 전략

```
Step 1: 코드 수준 분리 → 같은 DB, Command/Query 핸들러만 분리
Step 2: Read Model 추가 → 복잡한 조회에 비정규화 뷰 추가, 동기식 갱신
Step 3: 이벤트 기반 동기화 → Domain Event 발행 + Projector로 비동기 갱신
Step 4: DB 분리 (선택) → Write DB(PostgreSQL) + Read DB(Elasticsearch, Redis)
```

---

## 10. 실무 프로젝트 구조 (Kotlin + Spring)

```
src/main/kotlin/com/example/order/
├── application/
│   ├── command/
│   │   ├── CreateOrderCommand.kt
│   │   ├── CreateOrderHandler.kt
│   │   ├── ConfirmOrderCommand.kt
│   │   └── ConfirmOrderHandler.kt
│   ├── query/
│   │   ├── GetOrderByIdQuery.kt
│   │   ├── GetOrderByIdHandler.kt
│   │   ├── SearchOrdersQuery.kt
│   │   └── SearchOrdersHandler.kt
│   └── projector/
│       ├── OrderReadModelProjector.kt
│       └── CustomerStatsProjector.kt
├── domain/
│   ├── model/
│   │   ├── OrderAggregate.kt
│   │   ├── OrderItem.kt
│   │   └── OrderStatus.kt
│   ├── event/
│   │   ├── OrderCreatedEvent.kt
│   │   ├── OrderConfirmedEvent.kt
│   │   └── OrderCancelledEvent.kt
│   └── repository/
│       └── OrderRepository.kt
├── infrastructure/
│   ├── persistence/
│   │   ├── JpaOrderRepository.kt       # Write
│   │   └── ElasticOrderReadRepository.kt # Read
│   └── messaging/
│       └── SpringEventPublisher.kt
└── interfaces/
    └── rest/
        └── OrderController.kt
```

---

## FAQ

**Q: CQRS는 항상 Event Sourcing과 함께 사용해야 하나요?**
아니요. 독립적인 패턴입니다. CQRS만 단독으로 사용해도 됩니다.

**Q: Read Model이 여러 개여도 되나요?**
네. 용도별로 여러 Read Model을 만들 수 있습니다 (목록용, 상세용, 검색용 등).

**Q: Command가 값을 반환해도 되나요?**
순수 CQRS에서는 void이지만, 실무에서는 생성된 ID나 버전 번호 정도는 반환합니다.

**Q: 최종 일관성 지연은 얼마나 되나요?**
일반적으로 밀리초~초 단위. 대부분의 사용자는 인지하지 못합니다.

**Q: 모든 Bounded Context에 CQRS를 적용해야 하나요?**
아니요. 이점이 명확한 Context에만 적용하세요. 단순 CRUD는 전통적 방식이 적합합니다.

---

## 핵심 요약

| 영역 | 핵심 포인트 |
|------|-------------|
| **Command** | 상태 변경 의도 표현, 도메인 로직 실행, Aggregate 중심, 트랜잭션 일관성 |
| **Query** | 데이터 조회 전용, 비정규화된 Read Model, 화면/API 최적화, 캐싱 용이 |
| **동기화** | Projector로 Read Model 갱신, 이벤트 기반 비동기, 최종 일관성 수용, 재구축 가능 |
| **판단** | 복잡도 vs 유연성, 일관성 vs 성능, 점진적 도입 권장, 필요한 곳에만 적용 |

> "CQRS는 읽기와 쓰기의 요구사항이 다르다는 것을 인정하고, 각각에 최적화된 모델을 제공하는 것이다." — Greg Young

---

*참고: https://techmentor-avo.pages.dev/theory/ddd-theory-11*
