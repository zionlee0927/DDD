# DDD 패턴 및 구현 가이드

이 문서는 Domain-Driven Design 패턴을 Kotlin으로 구현할 때 참고하는 규칙입니다.

## Aggregate Root 구현

### 기본 구조

```kotlin
class Commission private constructor(
    val id: CommissionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val amount: Money,
    val rule: CommissionRule,
    private var status: CommissionStatus = CommissionStatus.PENDING,
    val calculatedAt: LocalDateTime = LocalDateTime.now(),
) {
    // Factory method
    companion object {
        fun create(
            id: CommissionId,
            tenantId: TenantId,
            partnerId: PartnerId,
            amount: Money,
            rule: CommissionRule,
        ): Commission {
            require(amount.amount > BigDecimal.ZERO) { "커미션 금액은 0보다 커야 한다" }
            return Commission(id, tenantId, partnerId, amount, rule)
        }

        // 영속성 계층에서 복원
        fun reconstitute(
            id: CommissionId,
            tenantId: TenantId,
            partnerId: PartnerId,
            amount: Money,
            rule: CommissionRule,
            status: CommissionStatus,
            calculatedAt: LocalDateTime,
        ): Commission = Commission(id, tenantId, partnerId, amount, rule, status, calculatedAt)
    }

    // 비즈니스 로직
    fun confirm() {
        check(status == CommissionStatus.PENDING) { "PENDING 상태에서만 확정 가능" }
        status = CommissionStatus.CONFIRMED
    }

    fun cancel() {
        check(status == CommissionStatus.PENDING) { "PENDING 상태에서만 취소 가능" }
        status = CommissionStatus.CANCELLED
    }

    fun currentStatus(): CommissionStatus = status
}
```

### 규칙

1. **생성자는 private**: companion object의 factory method 사용 강제
2. **Factory methods**:
   - `create()`: 새로운 aggregate 생성 (불변식 검증 포함)
   - `reconstitute()`: 영속성 계층에서 복원 (검증 생략)
3. **내부 상태 보호**:
   - 변경 가능한 필드는 `private var`
   - 외부 노출은 메서드로 (`currentStatus()`)
4. **비즈니스 로직 캡슐화**: 모든 상태 변경은 aggregate 메서드를 통해서만

## Value Object 구현

### data class 사용

```kotlin
data class Money(val amount: BigDecimal, val currency: String = "KRW") {
    init {
        require(amount >= BigDecimal.ZERO) { "금액은 0 이상이어야 한다" }
    }

    fun add(other: Money): Money {
        require(currency == other.currency) { "통화가 다릅니다" }
        return Money(amount.add(other.amount), currency)
    }
}

// ID Value Object
@JvmInline
value class CommissionId(val value: String) {
    init {
        require(value.isNotBlank()) { "CommissionId는 비어있을 수 없다" }
    }
}

@JvmInline
value class TenantId(val value: String)

@JvmInline
value class PartnerId(val value: String)
```

### 규칙

1. **불변성**: data class (equals/hashCode 자동 생성)
2. **Validation**: init 블록에서 검증
3. **ID는 inline value class**: 타입 안전성 + 런타임 오버헤드 없음
4. **연산 메서드**: 새 객체를 반환 (불변 유지)

## Domain Service 구현

### 언제 사용하는가?

- 여러 aggregate에 걸친 비즈니스 로직
- Aggregate에 속하지 않는 도메인 개념

### 구현 패턴

```kotlin
class AttributionJudge {
    fun judge(
        clicks: List<Click>,
        referralCodes: List<ReferralCode>,
        evidence: AttributionEvidence,
        config: ProgramConfig,
    ): AttributionResult {
        // 귀속 판정 로직
    }
}
```

### 규칙

1. **Stateless**: 상태를 가지지 않음
2. **Pure domain logic**: 프레임워크 의존성 없음
3. **명확한 책임**: 단일 도메인 개념에 집중

## Repository Interface (Domain Port)

```kotlin
interface CommissionRepository {
    fun findById(id: CommissionId): Commission?
    fun findByTenantAndAttribution(tenantId: TenantId, attributionDecisionId: String): Commission?
    fun save(commission: Commission): Commission
}
```

### 규칙

1. **Domain 패키지에 위치**: `{bc}.domain`
2. **Domain 언어 사용**: 기술적 용어 대신 비즈니스 용어
3. **Aggregate 단위**: Repository는 aggregate root 단위로만
4. **nullable 반환**: 없을 수 있는 경우 `?` 사용 (Optional 대신)

## Domain Event

```kotlin
data class CommissionCalculated(
    val commissionId: CommissionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val amount: Money,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class AttributionDecided(
    val attributionDecisionId: String,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: String,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
```

### 규칙

1. **과거형 명명**: `CommissionCalculated`, `AttributionDecided`
2. **불변 객체**: data class 사용
3. **발생 시점 포함**: `occurredAt` 필드 (기본값 now)
4. **최소 정보**: 이벤트 처리에 필요한 최소한의 정보만

## Cross-Aggregate 참조

```kotlin
// ✅ ID로 참조
class Commission(
    val partnerId: PartnerId,           // ID value object
    val attributionDecisionId: String,  // 다른 BC의 ID
)

// ❌ 직접 참조 금지
class Commission(
    val partner: Partner,               // 다른 aggregate 직접 참조
)
```

## Exception 처리

```kotlin
// BC별 base exception
sealed class CommissionException(message: String) : RuntimeException(message)

class CommissionNotFoundException(id: CommissionId) :
    CommissionException("Commission not found: ${id.value}")

class InvalidCommissionStateException(current: CommissionStatus, expected: CommissionStatus) :
    CommissionException("Invalid state: current=$current, expected=$expected")
```

### 규칙

1. **sealed class**: BC별 base exception
2. **RuntimeException 상속**: Unchecked exception 사용
3. **명확한 이름**: 비즈니스 의미가 드러나는 이름

## 테스트 작성

### Fixture 패턴

```kotlin
object CommissionFixture {
    fun create(
        id: CommissionId = CommissionId("comm-1"),
        tenantId: TenantId = TenantId("tenant-1"),
        partnerId: PartnerId = PartnerId("partner-1"),
        amount: Money = Money(BigDecimal("5000")),
        rule: CommissionRule = CommissionRule(RuleType.PERCENTAGE, BigDecimal("10")),
    ): Commission = Commission.create(id, tenantId, partnerId, amount, rule)
}
```

### 규칙

1. **object 사용**: Fixture는 싱글톤
2. **기본값 제공**: 모든 파라미터에 기본값 → 테스트에서 관심 있는 것만 오버라이드
3. **명확한 이름**: `create()`, `createConfirmed()`, `createCancelled()`
