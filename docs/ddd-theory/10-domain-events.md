# DDD 이론 10: Domain Events

> "도메인 전문가가 관심을 가지는 어떤 사건이 발생했음을 나타낸다. Domain Event는 과거형으로 명명된다."
> — Vaughn Vernon

## 1. Domain Event의 본질

Domain Event는 도메인에서 발생한 **비즈니스적으로 의미 있는 사건**이다.
과거에 일어난 일이므로 **불변(Immutable)**이며, **과거형(Past Tense)**으로 명명한다.

### 1.1 좋은 이벤트 이름 vs 나쁜 이벤트 이름

| ✅ 좋은 이름 | ❌ 나쁜 이름 | 이유 |
|---|---|---|
| `OrderCreated` | `CreateOrder` | 명령형은 이벤트가 아님 |
| `PaymentCompleted` | `OrderEvent` | 너무 일반적 |
| `ItemShipped` | `OrderUpdated` | 무엇이 변경되었는지 불명확 |
| `CustomerRegistered` | `CustomerData` | 이벤트가 아닌 데이터 |

### 1.2 Domain Event 기본 구조 (Kotlin)

```kotlin
import java.time.Instant
import java.util.UUID

// Domain Event 기본 인터페이스
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String
    val aggregateType: String
    val eventType: String
}

// 추적 가능한 이벤트 (Correlation/Causation ID 포함)
interface TrackedEvent : DomainEvent {
    val correlationId: String  // 전체 비즈니스 흐름 추적
    val causationId: String    // 직접 원인이 된 이벤트 ID
}

// 구체적인 Domain Event
data class OrderConfirmedEvent(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemSnapshot>,
    val totalAmount: Long,
    override val correlationId: String,
    override val causationId: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String = orderId,
    override val aggregateType: String = "Order",
    override val eventType: String = "OrderConfirmed"
) : TrackedEvent

data class OrderItemSnapshot(
    val productId: String,
    val quantity: Int,
    val unitPrice: Long
)
```

---

## 2. Domain Event vs Integration Event

| 구분 | Domain Event | Integration Event |
|------|-------------|-------------------|
| **범위** | Bounded Context 내부 | Bounded Context 간 |
| **전달 방식** | 인메모리 / 동기 | 메시지 브로커 / 비동기 |
| **스키마** | 도메인 객체 포함 가능 | 원시 타입만 (직렬화 가능) |
| **버전 관리** | 덜 엄격 | 엄격한 스키마 버전 관리 필수 |
| **결합도** | 내부 모델에 의존 가능 | 외부 계약으로 독립적 |

```kotlin
// Domain Event - 내부에서만 사용, 도메인 객체 참조 가능
data class OrderConfirmedDomainEvent(
    val order: Order,  // 도메인 객체 직접 참조
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String = order.id.value,
    override val aggregateType: String = "Order",
    override val eventType: String = "OrderConfirmed"
) : DomainEvent

// Integration Event - 외부 Context로 발행, 원시 타입만 사용
data class OrderConfirmedIntegrationEvent(
    val orderId: String,
    val customerId: String,
    val totalAmount: Long,
    val items: List<OrderItemDto>,
    val version: Int = 1,
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: Instant = Instant.now()
)

data class OrderItemDto(
    val productId: String,
    val quantity: Int,
    val unitPrice: Long
)
```

---

## 3. 이벤트 설계 원칙

1. **불변성**: 이벤트는 발생한 사실이므로 변경 불가 (`data class` + `val`)
2. **자기 완결성**: 핸들러가 추가 조회 없이 처리할 수 있도록 충분한 정보 포함
3. **과거형 명명**: `OrderCreated`, `PaymentCompleted`
4. **고유 식별**: 모든 이벤트에 `eventId` 부여 (멱등성 보장 기반)
5. **시간 기록**: `occurredAt`으로 발생 시점 명시
6. **추적 가능성**: `correlationId`, `causationId`로 흐름 추적

---

## 4. Outbox 패턴

Aggregate 저장과 이벤트 발행의 원자성을 보장하는 패턴이다.
트랜잭션 내에서 이벤트를 Outbox 테이블에 저장하고, 별도 프로세스가 폴링하여 발행한다.

```kotlin
// Outbox 엔티티
@Entity
@Table(name = "outbox_events")
data class OutboxEvent(
    @Id val id: String = UUID.randomUUID().toString(),
    val aggregateId: String,
    val aggregateType: String,
    val eventType: String,
    val payload: String,  // JSON 직렬화된 이벤트
    val correlationId: String,
    val createdAt: Instant = Instant.now(),
    var published: Boolean = false,
    var publishedAt: Instant? = null,
    var retryCount: Int = 0
)

// Application Service - 트랜잭션 내에서 Outbox 저장
@Service
class OrderApplicationService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun confirmOrder(orderId: String) {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.confirm()
        orderRepository.save(order)

        // 같은 트랜잭션 내에서 Outbox에 저장
        order.domainEvents.forEach { event ->
            outboxRepository.save(
                OutboxEvent(
                    aggregateId = event.aggregateId,
                    aggregateType = event.aggregateType,
                    eventType = event.eventType,
                    payload = objectMapper.writeValueAsString(event),
                    correlationId = (event as? TrackedEvent)?.correlationId ?: event.eventId
                )
            )
        }
        order.clearDomainEvents()
    }
}

// Outbox Publisher - 별도 스케줄러로 폴링 발행
@Component
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val messageBroker: MessageBroker
) {
    @Scheduled(fixedDelay = 5000) // 5초마다 폴링
    fun publishPendingEvents() {
        val events = outboxRepository.findUnpublished(limit = 100)

        events.forEach { event ->
            try {
                messageBroker.publish(
                    topic = event.eventType,
                    key = event.aggregateId,
                    payload = event.payload
                )
                event.published = true
                event.publishedAt = Instant.now()
                outboxRepository.save(event)
            } catch (e: Exception) {
                event.retryCount++
                outboxRepository.save(event)
                logger.error("Failed to publish event ${event.id}", e)
            }
        }
    }
}
```

---

## 5. 멱등성(Idempotent) 처리

동일 이벤트가 여러 번 전달되어도 결과가 동일하도록 보장한다.

```kotlin
// 처리 완료 기록 테이블
@Entity
@Table(name = "processed_events")
data class ProcessedEvent(
    @Id val eventId: String,
    val processedAt: Instant = Instant.now()
)

// 멱등성 보장 데코레이터
class IdempotentEventHandler<T : DomainEvent>(
    private val inner: EventHandler<T>,
    private val processedEventRepository: ProcessedEventRepository
) : EventHandler<T> {

    @Transactional
    override suspend fun handle(event: T) {
        if (processedEventRepository.existsById(event.eventId)) {
            logger.info("Event ${event.eventId} already processed, skipping")
            return
        }

        inner.handle(event)

        processedEventRepository.save(
            ProcessedEvent(eventId = event.eventId)
        )
    }
}

// 사용 예시
val handler = IdempotentEventHandler(
    inner = ReserveInventoryHandler(inventoryRepository),
    processedEventRepository = processedEventRepository
)
```

---

## 6. Saga 패턴

여러 서비스에 걸친 비즈니스 트랜잭션을 이벤트로 조율하는 패턴이다.
분산 트랜잭션(2PC) 대신 **보상 트랜잭션(Compensating Transaction)**으로 최종 일관성을 유지한다.

### 6.1 Choreography (안무) 방식

각 서비스가 이벤트를 발행하고 구독하여 자율적으로 동작한다.

**장점**: 느슨한 결합, 단순한 서비스
**단점**: 흐름 파악 어려움, 순환 의존 위험

```kotlin
// 주문 서비스 - 이벤트 발행
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher
) {
    fun createOrder(command: CreateOrderCommand) {
        val order = Order.create(command)
        orderRepository.save(order)
        eventPublisher.publish(OrderCreatedEvent(
            orderId = order.id.value,
            customerId = command.customerId,
            items = command.items,
            totalAmount = order.totalAmount,
            correlationId = order.id.value,
            causationId = ""
        ))
    }

    // 결제 실패 시 보상
    @EventListener
    fun onPaymentFailed(event: PaymentFailedEvent) {
        val order = orderRepository.findById(event.orderId)!!
        order.cancel(reason = "결제 실패: ${event.reason}")
        orderRepository.save(order)
    }
}

// 결제 서비스 - OrderCreated 구독
@Service
class PaymentEventHandler(
    private val paymentService: PaymentService,
    private val eventPublisher: EventPublisher
) {
    @EventListener
    fun onOrderCreated(event: OrderCreatedEvent) {
        try {
            val payment = paymentService.process(event.orderId, event.totalAmount)
            eventPublisher.publish(PaymentCompletedEvent(
                paymentId = payment.id,
                orderId = event.orderId,
                amount = event.totalAmount,
                correlationId = event.correlationId,
                causationId = event.eventId
            ))
        } catch (e: Exception) {
            eventPublisher.publish(PaymentFailedEvent(
                orderId = event.orderId,
                reason = e.message ?: "Unknown error",
                correlationId = event.correlationId,
                causationId = event.eventId
            ))
        }
    }
}

// 재고 서비스 - PaymentCompleted 구독
@Service
class InventoryEventHandler(
    private val inventoryService: InventoryService,
    private val eventPublisher: EventPublisher
) {
    @EventListener
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        try {
            inventoryService.reserve(event.orderId)
            eventPublisher.publish(InventoryReservedEvent(
                orderId = event.orderId,
                correlationId = event.correlationId,
                causationId = event.eventId
            ))
        } catch (e: InsufficientStockException) {
            eventPublisher.publish(InventoryReservationFailedEvent(
                orderId = event.orderId,
                failedItems = e.failedItems,
                correlationId = event.correlationId,
                causationId = event.eventId
            ))
        }
    }

    // 배송 실패 시 보상: 재고 해제
    @EventListener
    fun onShipmentFailed(event: ShipmentFailedEvent) {
        inventoryService.release(event.orderId)
    }
}
```

### 6.2 Orchestration (오케스트레이션) 방식

중앙 오케스트레이터가 전체 흐름을 제어한다.

**장점**: 명확한 흐름, 쉬운 모니터링
**단점**: 단일 실패점, 오케스트레이터 복잡도 증가

```kotlin
// Saga Step 정의
data class SagaStep(
    val name: String,
    val execute: suspend (SagaContext) -> Unit,
    val compensate: suspend (SagaContext) -> Unit
)

// Saga Context - 단계 간 데이터 공유
class SagaContext(val orderId: String) {
    private val data = mutableMapOf<String, Any>()
    fun put(key: String, value: Any) { data[key] = value }
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T = data[key] as T
}

// Order Saga Orchestrator
@Service
class OrderSagaOrchestrator(
    private val paymentService: PaymentService,
    private val inventoryService: InventoryService,
    private val shippingService: ShippingService,
    private val sagaRepository: SagaRepository
) {
    private val steps = listOf(
        SagaStep(
            name = "reserveInventory",
            execute = { ctx -> inventoryService.reserve(ctx.orderId) },
            compensate = { ctx -> inventoryService.release(ctx.orderId) }
        ),
        SagaStep(
            name = "processPayment",
            execute = { ctx ->
                val paymentId = paymentService.process(ctx.orderId, ctx.get("amount"))
                ctx.put("paymentId", paymentId)
            },
            compensate = { ctx -> paymentService.refund(ctx.get("paymentId")) }
        ),
        SagaStep(
            name = "createShipment",
            execute = { ctx -> shippingService.create(ctx.orderId) },
            compensate = { ctx -> shippingService.cancel(ctx.orderId) }
        )
    )

    suspend fun execute(orderId: String, amount: Long) {
        val context = SagaContext(orderId).apply { put("amount", amount) }
        val completedSteps = mutableListOf<SagaStep>()

        try {
            for (step in steps) {
                step.execute(context)
                completedSteps.add(step)
                sagaRepository.updateState(orderId, step.name, "COMPLETED")
            }
            sagaRepository.updateState(orderId, "saga", "COMPLETED")
        } catch (e: Exception) {
            // 역순으로 보상 트랜잭션 실행
            for (step in completedSteps.reversed()) {
                try {
                    step.compensate(context)
                    sagaRepository.updateState(orderId, step.name, "COMPENSATED")
                } catch (compensateError: Exception) {
                    sagaRepository.updateState(orderId, step.name, "COMPENSATION_FAILED")
                    alertManualIntervention(orderId, step.name, compensateError)
                }
            }
            sagaRepository.updateState(orderId, "saga", "FAILED")
        }
    }

    private fun alertManualIntervention(orderId: String, step: String, error: Exception) {
        logger.error("Manual intervention required: order=$orderId, step=$step", error)
    }
}
```

---

## 7. 보상 트랜잭션 (Compensating Transaction)

Saga에서 실패 시 이전 단계의 효과를 취소하는 역방향 작업이다.

```kotlin
// 보상 트랜잭션 예시
interface CompensatableAction<T> {
    suspend fun execute(input: T): Any
    suspend fun compensate(input: T)
}

class ReserveInventoryAction(
    private val inventoryRepository: InventoryRepository
) : CompensatableAction<ReserveInventoryInput> {

    override suspend fun execute(input: ReserveInventoryInput): Any {
        val inventory = inventoryRepository.findByProductId(input.productId)
            ?: throw ProductNotFoundException(input.productId)
        inventory.reserve(input.quantity)
        inventoryRepository.save(inventory)
        return inventory.id
    }

    // 보상: 예약 해제
    override suspend fun compensate(input: ReserveInventoryInput) {
        val inventory = inventoryRepository.findByProductId(input.productId)!!
        inventory.release(input.quantity)
        inventoryRepository.save(inventory)
    }
}
```

---

## 8. 이벤트 버전 관리 (Event Versioning)

이벤트 스키마가 변경될 때 하위 호환성을 유지하는 전략이다.

```kotlin
// V1: 초기 이벤트
data class OrderCreatedEventV1(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemDto>,
    val totalAmount: Long,
    val version: Int = 1
)

// V2: 필드 추가 (하위 호환)
data class OrderCreatedEventV2(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemDto>,
    val totalAmount: Long,
    val couponCode: String? = null,      // 새 필드 (nullable)
    val discountAmount: Long = 0L,       // 새 필드 (기본값)
    val version: Int = 2
)

// 이벤트 업캐스터: V1 → V2 변환
interface EventUpcaster {
    fun canUpcast(eventType: String, version: Int): Boolean
    fun upcast(payload: Map<String, Any?>): Map<String, Any?>
}

class OrderCreatedV1ToV2Upcaster : EventUpcaster {
    override fun canUpcast(eventType: String, version: Int): Boolean =
        eventType == "OrderCreated" && version == 1

    override fun upcast(payload: Map<String, Any?>): Map<String, Any?> =
        payload + mapOf(
            "couponCode" to null,
            "discountAmount" to 0L,
            "version" to 2
        )
}

// 업캐스터 체인
class EventUpcasterChain(private val upcasters: List<EventUpcaster>) {
    fun upcast(eventType: String, version: Int, payload: Map<String, Any?>): Map<String, Any?> {
        var current = payload
        var currentVersion = version
        for (upcaster in upcasters) {
            if (upcaster.canUpcast(eventType, currentVersion)) {
                current = upcaster.upcast(current)
                currentVersion++
            }
        }
        return current
    }
}
```

---

## 9. Dead Letter Queue (DLQ)

처리 실패한 이벤트를 별도 큐에 저장하여 분석 및 재처리하는 패턴이다.

```kotlin
// DLQ 엔티티
@Entity
@Table(name = "dead_letter_queue")
data class DeadLetterEvent(
    @Id val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val eventType: String,
    val payload: String,
    val errorMessage: String,
    val stackTrace: String?,
    val retryCount: Int,
    val failedAt: Instant = Instant.now(),
    var resolvedAt: Instant? = null,
    var resolution: String? = null  // RETRIED, SKIPPED, MANUAL
)

// 재시도 + DLQ 전략
class ResilientEventConsumer(
    private val handler: EventHandler<DomainEvent>,
    private val dlqRepository: DeadLetterRepository,
    private val alertService: AlertService,
    private val maxRetries: Int = 3
) {
    suspend fun consume(event: DomainEvent) {
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                handler.handle(event)
                return // 성공
            } catch (e: Exception) {
                lastError = e
                if (!isRetryable(e) || attempt == maxRetries - 1) {
                    sendToDeadLetterQueue(event, e, attempt + 1)
                    return
                }
                delay(calculateBackoff(attempt))
            }
        }
    }

    private fun calculateBackoff(attempt: Int): Long =
        (2.0.pow(attempt) * 1000).toLong() // 1s, 2s, 4s...

    private fun isRetryable(e: Exception): Boolean = when (e) {
        is NetworkException, is TimeoutException -> true
        is ValidationException, is BusinessRuleException -> false
        else -> true
    }

    private suspend fun sendToDeadLetterQueue(
        event: DomainEvent, error: Exception, retryCount: Int
    ) {
        dlqRepository.save(DeadLetterEvent(
            eventId = event.eventId,
            eventType = event.eventType,
            payload = objectMapper.writeValueAsString(event),
            errorMessage = error.message ?: "Unknown",
            stackTrace = error.stackTraceToString(),
            retryCount = retryCount
        ))
        alertService.notify("Event ${event.eventId} moved to DLQ after $retryCount retries")
    }
}

// DLQ 재처리 서비스
@Service
class DlqReprocessingService(
    private val dlqRepository: DeadLetterRepository,
    private val eventDispatcher: EventDispatcher
) {
    fun reprocess(deadLetterId: String) {
        val dlEvent = dlqRepository.findById(deadLetterId)!!
        val event = deserialize(dlEvent.payload, dlEvent.eventType)
        eventDispatcher.dispatch(event)
        dlEvent.resolvedAt = Instant.now()
        dlEvent.resolution = "RETRIED"
        dlqRepository.save(dlEvent)
    }

    fun skip(deadLetterId: String, reason: String) {
        val dlEvent = dlqRepository.findById(deadLetterId)!!
        dlEvent.resolvedAt = Instant.now()
        dlEvent.resolution = "SKIPPED"
        dlqRepository.save(dlEvent)
    }
}
```

---

## 10. Correlation ID / Causation ID

분산 시스템에서 이벤트 흐름을 추적하기 위한 식별자이다.

- **Correlation ID**: 하나의 비즈니스 흐름 전체를 관통하는 ID (예: 주문 ID)
- **Causation ID**: 현재 이벤트를 직접 유발한 이벤트의 ID

```kotlin
// 이벤트 발행 시 Correlation/Causation 전파
@Service
class EventPublisherImpl(
    private val messageBroker: MessageBroker
) : EventPublisher {

    fun publish(event: DomainEvent, causedBy: DomainEvent? = null) {
        val trackedEvent = when (event) {
            is TrackedEvent -> event
            else -> EnrichedEvent(
                delegate = event,
                correlationId = (causedBy as? TrackedEvent)?.correlationId
                    ?: event.aggregateId,
                causationId = causedBy?.eventId ?: ""
            )
        }
        messageBroker.publish(trackedEvent.eventType, serialize(trackedEvent))
    }
}

// 이벤트 로그로 전체 흐름 추적
@Service
class EventTracer(private val eventLogRepository: EventLogRepository) {

    fun traceFlow(correlationId: String): List<EventLogEntry> =
        eventLogRepository.findByCorrelationId(correlationId)
            .sortedBy { it.occurredAt }

    fun findCausationChain(eventId: String): List<EventLogEntry> {
        val chain = mutableListOf<EventLogEntry>()
        var currentId: String? = eventId
        while (currentId != null) {
            val entry = eventLogRepository.findByEventId(currentId) ?: break
            chain.add(entry)
            currentId = entry.causationId.takeIf { it.isNotBlank() }
        }
        return chain.reversed()
    }
}
```

---

## 11. 핵심 요약

| 개념 | 핵심 |
|------|------|
| Domain Event | 과거에 발생한 불변의 비즈니스 사건 |
| Integration Event | BC 간 통신을 위한 직렬화 가능한 이벤트 |
| Outbox 패턴 | 트랜잭션 + 이벤트 발행의 원자성 보장 |
| 멱등성 | eventId 기반 중복 처리 방지 |
| Saga (Choreography) | 이벤트 체인으로 자율적 조율 |
| Saga (Orchestration) | 중앙 오케스트레이터가 흐름 제어 |
| 보상 트랜잭션 | 실패 시 역방향으로 효과 취소 |
| 이벤트 버전 관리 | 업캐스터로 하위 호환성 유지 |
| DLQ | 실패 이벤트 격리 및 재처리 |
| Correlation/Causation ID | 분산 흐름 추적 및 디버깅 |

---

## 참고

- 원본: https://techmentor-avo.pages.dev/theory/ddd-theory-10
- 모든 상태 변경에 이벤트를 발행할 필요는 없다. 도메인 전문가가 관심을 가질 만한 사건만 이벤트로 모델링한다.
- 이벤트 순서는 일반적으로 보장되지 않는다. 순서가 중요하면 같은 파티션 키를 사용하거나 시퀀스 번호를 포함한다.
- 이벤트 스키마 변경 시 필드 추가는 허용하되, 삭제나 타입 변경은 새 버전의 이벤트를 만든다.
