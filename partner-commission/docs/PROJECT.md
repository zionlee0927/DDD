# Partner Commission

파트너 추천 프로그램을 운영하는 Headless SaaS. 고객사(테넌트)는 API만 연동하면 추적 링크 발급부터 귀속 판정, 커미션 계산, 정산 명세까지 전부 위임할 수 있다.

> "파트너한테 돈 잘못 줄 일이 없다"

---

## 서비스 흐름

```
1. 고객사가 추적 링크 / 추천 코드를 발급받아 파트너에게 전달
2. 소비자가 링크를 클릭하면 우리 서버가 클릭을 기록하고 고객사로 리다이렉트
3. 고객사에서 전환 발생 시 전환 이벤트 API 전송 → 귀속 판정 + 커미션 산정
4. 전환이 취소되면 취소 이벤트 API 전송 → 커미션 자동 회수
5. 정산일에 명세서 조회 → 고객사가 확인 후 직접 파트너에게 지급
```

우리가 하는 것: 추적 → 귀속 판정 → 커미션 계산 → 명세서
우리가 안 하는 것: 실제 송금, 세금, 대시보드 UI

---

## 목차

1. [아키텍처](#아키텍처)
2. [Bounded Context 구성](#bounded-context-구성)
3. [핵심 도메인 로직](#핵심-도메인-로직)
4. [DDD 전술 패턴 구현](#ddd-전술-패턴-구현)
5. [BC 간 통신 방식](#bc-간-통신-방식)
6. [테스트 구성](#테스트-구성)
7. [기술 스택](#기술-스택)
8. [주요 파일 위치](#주요-파일-위치)

---

## 아키텍처

### Hexagonal Architecture

```
[REST Controller / Event Listener]  ← Inbound Adapter
              ↓
    [UseCase Interface]              ← Port In
              ↓
    [Application Service]
              ↓
         [Domain]
              ↑
 [RepositoryImpl / ACL Adapter]      ← Outbound Adapter
              ↑
     [JPA / 타 BC Repository]
```

의존 방향은 항상 외부 → 내부(Infrastructure → Application → Domain). Domain은 아무것도 의존하지 않는다.

| 레이어 | 위치 | 역할 |
|--------|------|------|
| Domain | `{bc}/domain/` | 순수 비즈니스 로직. Spring/JPA 의존 금지 |
| Application | `{bc}/application/` | UseCase 오케스트레이션. Domain만 의존 |
| Infrastructure | `{bc}/infrastructure/` | DB, REST, 이벤트 연결. Application + Domain 의존 가능 |



### ArchUnit으로 아키텍처 규칙 강제

아키텍처 규칙을 빌드 시점에 자동 검증한다. 규칙을 어기면 테스트가 깨진다.

| 테스트 | 검증 내용 |
|--------|----------|
| `HexagonalArchitectureTest` | 레이어 의존 방향 (Domain이 Application 참조 금지 등) |
| `LayerDependencyTest` | Domain이 Spring/JPA에 의존하지 않음 |
| `ModuleBoundaryTest` | BC 간 참조 규칙 (domain/application에서 타 BC 직접 참조 금지) |
| `NamingConventionTest` | `*UseCase`, `*Controller`, `*JpaEntity` 등 네이밍 규칙 |

```kotlin
layeredArchitecture()
    .layer("Infrastructure").definedBy("..infrastructure..")
    .layer("Application").definedBy("..application..")
    .layer("Domain").definedBy("..domain..")
    .whereLayer("Application").mayOnlyAccessLayers("Domain")
    .whereLayer("Domain").mayNotAccessAnyLayer()
```

---

## Bounded Context 구성

### 전략적 설계

| BC | 유형 | 역할 |
|----|------|------|
| **Attribution** | Core | 전환 이벤트 수신, 귀속 판정/철회. 이 서비스의 존재 이유 |
| **Commission** | Core | 커미션 산정/확정/취소/차감. 정확성이 핵심 가치 |
| Tracking | Supporting | 추적 링크 발급, 클릭 기록, 추천 코드 관리 |
| Partner | Supporting | 파트너 계정, 테넌트별 소속, 티어 |
| Tenant | Supporting | 고객사 등록, API 키, 프로그램 설정 (다른 BC의 설정 원천) |
| Statement | Supporting | 정산 주기 마감, 파트너별 명세서 생성 |
| Notification | Generic | 웹훅 발송 |

Attribution과 Commission이 Core인 이유는 이 둘 없이는 비즈니스가 성립하지 않고, 경쟁 우위를 만드는 로직이 집중되기 때문이다. 그래서 두 BC에만 DDD 전술 패턴을 적극 적용했다.

### Attribution과 Commission을 왜 분리했나

같은 도메인처럼 보이지만 생명주기와 변경 빈도가 다르다.

| | Attribution | Commission |
|--|------------|------------|
| 생명주기 | 전환 수신 시 즉시 완료 | 산정 → 확정 → 정산까지 수주~수개월 |
| 변경 빈도 | 귀속 전략은 안정적 | 커미션 규칙은 이벤트 기간마다 변경 |
| 핵심 관심사 | "이 전환이 어떤 파트너 덕분인가" | "파트너에게 얼마를 줄 것인가" |
| 트랜잭션 | 귀속 판정 독립 트랜잭션 | 커미션 산정 독립 트랜잭션 |

귀속이 성공해도 커미션 산정이 실패하면 재시도할 수 있어야 한다. 트랜잭션이 엮여 있으면 이게 불가능하다.



---

## 핵심 도메인 로직

### Attribution — 전환 귀속 판정

`ReceiveConversionFacadeService`가 전체 흐름을 조율한다.

```
1. 중복 전환 체크       externalId로 멱등성 보장
2. 테넌트 설정 조회     귀속 윈도우, 귀속 전략
3. 귀속 증거 결정       clickId 또는 referralCode
4. ConversionEvent 생성
5. 귀속 판정           전략 패턴 (ClickProcessor / ReferralCodeProcessor)
6. AttributionDecision 생성  attributed 또는 unattributed
7. 저장 + AttributionDecided 이벤트 발행
```

귀속 판정 자체는 Domain Service(`AttributionJudges`)가 담당한다. 윈도우 만료 여부, 추적 링크 존재 여부, 추천 코드 유효성을 검사해서 `AttributionResult`를 반환한다.

```kotlin
class ClickAttributionJudge {
    fun judge(click: ClickData, trackingLink: TrackingLinkData?, config: AttributionConfig): AttributionResult {
        trackingLink ?: return AttributionResult.Unattributed
        val windowEnd = click.clickedAt.plus(config.attributionWindow.toMinutes(), ChronoUnit.MINUTES)
        if (now.isAfter(windowEnd)) return AttributionResult.Unattributed
        return AttributionResult.Attributed(trackingLink.partnerId)
    }
}
```

귀속에 성공하면 `AttributionDecided`, 실패하면 `AttributionFailed` 이벤트를 등록한다. 철회 시엔 `AttributionRevoked` 이벤트가 Commission BC로 전달된다.

### Commission — 커미션 생명주기

```
PENDING ──────────────→ CONFIRMED   (확정 이벤트 수신 or 14일 경과)
PENDING ──────────────→ CANCELLED   (전환 취소, 아직 PENDING인 경우)
CONFIRMED → 취소 불가 → Deduction 생성  (이미 확정된 건, 다음 정산에서 차감)
```

취소 처리가 핵심 판단 포인트다. `HandleRevocationService`에서 상태에 따라 분기한다.

- **PENDING이면** → `commission.cancel()` → 상태 `CANCELLED`로 전이
- **CONFIRMED이면** → `Deduction.create()` → 다음 정산에서 차감 항목으로 표시

이미 파트너가 인지한 확정 금액을 강제 취소하지 않고, 투명하게 차감 내역으로 남기는 방식이다.

금액 계산은 `CommissionCalculator`(Domain Service)가 담당한다.

```kotlin
class CommissionCalculator {
    fun calculate(conversionAmount: Money, rule: CommissionRule): Money {
        return when (rule.type) {
            RuleType.PERCENTAGE -> conversionAmount × rule.value / 100
            RuleType.FIXED      -> rule.value
        }
    }
}
```



---

## DDD 전술 패턴 구현

### Aggregate Root

공통 `AggregateRoot<ID>` 추상 클래스를 기반으로 모든 Aggregate이 동일한 패턴을 따른다.

```kotlin
abstract class AggregateRoot<ID>(id: ID) : Entity<ID>(id) {
    @Transient
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()

    protected fun registerEvent(event: DomainEvent) { domainEvents.add(event) }
    fun getAndClearDomainEvents(): List<DomainEvent> { ... }
}
```

각 Aggregate은 세 종류의 생성 경로를 갖는다.

- `create()` — 새 Aggregate 생성. 불변식 검증 + 도메인 이벤트 등록
- `reconstitute()` — DB에서 복원. 검증 생략, 이벤트 없음
- 상태 변경 메서드 (`cancel()`, `confirm()`, `revoke()`) — 상태 전이 + 이벤트 등록

```kotlin
class Commission private constructor(...) : AggregateRoot<CommissionId>(id) {

    fun cancel() {
        check(status == CommissionStatus.PENDING) { "PENDING 상태에서만 취소 가능" }
        status = CommissionStatus.CANCELLED
        registerEvent(CommissionCancelled(...))
    }

    companion object {
        fun create(...): Commission {
            val commission = Commission(...)
            commission.registerEvent(CommissionCalculated(...))
            return commission
        }

        fun reconstitute(...): Commission = Commission(...)
    }
}
```

생성자는 `private`이므로 반드시 factory method를 통해서만 생성된다.

### 핵심 Aggregate 목록

| Aggregate | BC | 상태 전이 |
|-----------|-----|----------|
| `AttributionDecision` | attribution | ATTRIBUTED / UNATTRIBUTED / REVOKED |
| `ConversionEvent` | attribution | 전환 이벤트 원본 기록 |
| `Commission` | commission | PENDING → CONFIRMED / CANCELLED / SETTLED |
| `Deduction` | commission | 확정 커미션 취소 시 생성되는 차감 항목 |
| `TrackingLink` | tracking | 파트너별 추적 URL |
| `ReferralCode` | tracking | 파트너별 추천 코드 |
| `Click` | tracking | 클릭 기록 |
| `Partner` | partner | 파트너 계정 |
| `Membership` | partner | 테넌트별 소속 + 티어 |
| `Tenant` | tenant | 고객사 + 프로그램 설정 |
| `ApiKey` | tenant | API 키 |

Aggregate 간 직접 참조는 금지. ID(Value Object)로만 참조한다.

```kotlin
class Commission(
    val partnerId: PartnerId,           // ✅ ID로 참조
    val attributionDecisionId: String,  // ✅ ID로 참조
    // val partner: Partner,            // ❌ 직접 참조 금지
)
```

하나의 트랜잭션에서 하나의 Aggregate만 수정한다는 원칙을 지키기 위해서다.



### Value Object

```kotlin
// 공통 VO — shared/domain/value/에서 모든 BC가 사용
data class Money(val amount: BigDecimal, val currency: String = "KRW")

@JvmInline value class TenantId(val value: UUID)
@JvmInline value class PartnerId(val value: UUID)
```

불변 객체(data class)이고, 동등성은 ID가 아닌 값으로 판단한다. ID VO는 `@JvmInline value class`로 타입 안전성을 확보하면서 런타임 오버헤드를 없앴다.

### Domain Event 발행 흐름

```
Aggregate.someMethod()
    → registerEvent(SomeEvent)

RepositoryImpl.save()
    → jpaRepository.save(entity)
    → commission.getAndClearDomainEvents()
        .forEach { eventPublisher.publishEvent(it) }

타 BC EventListener
    @TransactionalEventListener(AFTER_COMMIT)
    @Transactional(REQUIRES_NEW)
    fun on(event: SomeEvent) { ... }
```

`AFTER_COMMIT`을 쓰는 이유: 귀속 트랜잭션이 롤백되면 커미션 산정도 하지 말아야 한다. 커밋이 확정된 후에만 이벤트를 처리한다.

`REQUIRES_NEW`를 쓰는 이유: 커미션 산정 트랜잭션이 실패해도 귀속 트랜잭션은 이미 커밋된 상태. 독립적으로 재시도할 수 있다.

### Repository Pattern

Repository 인터페이스는 Domain에, 구현체는 Infrastructure에 위치한다. Domain 객체 ↔ JPA Entity 변환은 `RepositoryImpl`이 담당한다.

```kotlin
// Domain: 인터페이스만 정의
interface CommissionRepository {
    fun findById(id: CommissionId): Commission?
    fun save(commission: Commission): Commission
}

// Infrastructure: JPA로 구현 + 이벤트 발행
class CommissionRepositoryImpl(
    private val jpaRepository: CommissionJpaRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : CommissionRepository {
    override fun save(commission: Commission): Commission {
        val entity = jpaRepository.save(commission.toJpaEntity())
        commission.getAndClearDomainEvents().forEach { eventPublisher.publishEvent(it) }
        return entity.toDomain()
    }
}
```



---

## BC 간 통신 방식

### Context Mapping

| From | To | 관계 | 구현 |
|------|----|------|------|
| Attribution | Tracking | Customer-Supplier | ACL Adapter → Tracking ReadRepository |
| Attribution | Tenant | Customer-Supplier | ACL Adapter → Tenant ReadRepository |
| Commission | Tenant | Customer-Supplier | ACL Adapter → Tenant ReadRepository |
| Commission | Attribution | Events | `AttributionDecided` / `AttributionRevoked` 이벤트 구독 |

### BC 간 참조 규칙

```
domain/        → 타 BC 전면 금지 (shared만 허용)
application/   → 타 BC 전면 금지 (port/out 인터페이스로 격리)
infrastructure/out/acl/    → C-S 관계 BC의 value + ReadRepository만 허용
infrastructure/in/event/   → Events 관계 BC의 domain/event만 허용
```

이 규칙은 `ModuleBoundaryTest`가 자동으로 검증한다.

### ACL (Anti-Corruption Layer) Adapter

타 BC 데이터가 필요할 때 Application은 직접 참조하지 않는다. Port Out 인터페이스만 의존하고, Infrastructure ACL Adapter가 번역 책임을 진다.

```kotlin
// Application: 타 BC를 모르는 Port 인터페이스만 정의
interface LoadTenantConfigPort {
    fun loadConfig(tenantId: TenantId): AttributionConfig  // 자기 BC의 VO 반환
}

// Infrastructure: 타 BC 호출 + 자기 BC VO로 번역
@Component
class TenantConfigAdapter(
    private val tenantReadRepository: TenantReadRepository,
) : LoadTenantConfigPort {
    override fun loadConfig(tenantId: TenantId): AttributionConfig {
        val view = tenantReadRepository.findProgramConfigByTenantId(tenantId)
        return AttributionConfig(                              // 번역
            attributionWindow = Duration.ofMinutes(view.attributionWindowMinutes),
            strategy = AttributionStrategy.valueOf(view.strategy),
        )
    }
}
```

Application 레이어는 타 BC가 존재하는지조차 모른다.

### 이벤트 기반 통신 — Attribution → Commission

```kotlin
// commission/infrastructure/in/event/AttributionEventListener.kt
@Component
class AttributionEventListener(
    private val calculateCommissionUseCase: CalculateCommissionUseCase,
    private val handleRevocationUseCase: HandleRevocationUseCase,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun on(event: AttributionDecided) {
        calculateCommissionUseCase.execute(CalculateCommissionCommand(...))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun on(event: AttributionRevoked) {
        handleRevocationUseCase.execute(event.attributionDecisionId.value.toString())
    }
}
```

Listener는 `attribution.domain.event`만 참조한다. Attribution의 나머지 코드(aggregate, service 등)는 볼 수 없다.



---

## 테스트 구성

### 테스트 계층

```
architecture/     ArchUnit — 레이어 의존성, BC 경계, 네이밍 규칙
{bc}/domain/      도메인 단위 테스트 — 순수 Kotlin, 프레임워크 없음
{bc}/application/ UseCase 단위 테스트 — MockK 사용
integration/      통합 테스트 — @Tag("integration"), 실제 DB
```

Domain 테스트는 Spring 없이 실행된다. 프레임워크 독립성 덕분에 테스트가 가볍고 빠르다.

### 통합 테스트 시나리오

실제 비즈니스 흐름을 end-to-end로 검증한다.

| 시나리오 | 파일 |
|---------|------|
| 전환 수신 → 귀속 판정 → 커미션 산정 | `AttributionFlowTest` |
| 커미션 PENDING → CONFIRMED | `CommissionConfirmFlowTest` |
| 귀속 철회 → 커미션 PENDING → CANCELLED | `RevocationFlowTest` |
| 귀속 철회 → 커미션 CONFIRMED → Deduction 생성 | `RevokeDeductionFlowTest` |
| 동일 externalId 중복 전환 거부 | `DuplicateRejectionTest` |
| 취소 이벤트 전체 흐름 | `RevokeCancelFlowTest` |

통합 테스트는 `BaseIntegrationTest`, `FlowExecutor`(API 호출), `DbVerifier`(DB 검증) 프레임워크로 지원한다.

### Fixture 패턴

모든 파라미터에 기본값을 제공하고, 테스트에서 관심 있는 것만 오버라이드한다.

```kotlin
object CommissionFixture {
    fun create(
        tenantId: TenantId = TenantId(UUID.randomUUID()),
        partnerId: PartnerId = PartnerId(UUID.randomUUID()),
        amount: Money = Money(BigDecimal("5000")),
        rule: CommissionRule = CommissionRule(RuleType.PERCENTAGE, BigDecimal("10")),
    ): Commission = Commission.create(tenantId, partnerId, "attr-1", amount, rule)
}
```

---

## 기술 스택

| | |
|--|--|
| 언어 | Kotlin |
| 프레임워크 | Spring Boot 3 |
| 영속성 | Spring Data JPA (Hibernate) |
| DB | PostgreSQL (Docker Compose) |
| 테스트 | JUnit 5, MockK, ArchUnit |
| 빌드 | Gradle Kotlin DSL |

---

## 주요 파일 위치

### Attribution BC

| 역할 | 파일 |
|------|------|
| 귀속 판정 Aggregate | `attribution/domain/aggregate/AttributionDecision.kt` |
| 귀속 판정 Domain Service | `attribution/domain/service/AttributionJudges.kt` |
| 귀속 오케스트레이션 | `attribution/application/service/ReceiveConversionFacadeService.kt` |
| 귀속 전략 패턴 구현 | `attribution/application/service/processor/AttributeConversionProcessors.kt` |
| REST API | `attribution/infrastructure/in/web/AttributionController.kt` |
| ACL — Tenant 설정 조회 | `attribution/infrastructure/out/acl/TenantConfigAdapter.kt` |
| ACL — Tracking 데이터 조회 | `attribution/infrastructure/out/acl/TrackingDataAdapter.kt` |

### Commission BC

| 역할 | 파일 |
|------|------|
| 커미션 Aggregate | `commission/domain/aggregate/Commission.kt` |
| 차감 Aggregate | `commission/domain/aggregate/Deduction.kt` |
| 금액 계산 Domain Service | `commission/domain/service/CommissionCalculator.kt` |
| 취소 정책 Domain Service | `commission/domain/service/RevocationPolicy.kt` |
| 커미션 산정 UseCase | `commission/application/service/CalculateCommissionService.kt` |
| 취소 처리 UseCase | `commission/application/service/HandleRevocationService.kt` |
| Attribution 이벤트 리스너 | `commission/infrastructure/in/event/AttributionEventListener.kt` |

### Shared

| 역할 | 파일 |
|------|------|
| AggregateRoot | `shared/domain/AggregateRoot.kt` |
| Money | `shared/domain/value/Money.kt` |
| TenantId / PartnerId | `shared/domain/value/TenantId.kt`, `PartnerId.kt` |

### 아키텍처 테스트

| 역할 | 파일 |
|------|------|
| 레이어 의존 검증 | `architecture/HexagonalArchitectureTest.kt` |
| BC 경계 검증 | `architecture/ModuleBoundaryTest.kt` |
| 네이밍 규칙 검증 | `architecture/NamingConventionTest.kt` |
