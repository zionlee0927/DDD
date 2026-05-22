# Project Structure & Architecture

## Hexagonal Architecture Layers

```
Infrastructure(Adapter) → Application → Domain
```

Domain은 핵심이며 외부 레이어에 대한 의존성이 없다.

## Package Structure

```
com.partnercommission/
├── {bc}/                              # BC별 패키지 (attribution, commission, tracking 등)
│   ├── domain/                        # 도메인 레이어 (순수 Kotlin, 프레임워크 의존 없음)
│   │   ├── aggregate/                 # Aggregate Root
│   │   ├── entity/                    # 내부 Entity (필요 시)
│   │   ├── value/                     # Value Objects
│   │   ├── repository/                # Repository 인터페이스 (Out Port)
│   │   ├── service/                   # Domain Service
│   │   ├── event/                     # Domain Events
│   │   └── exception/                 # Domain Exceptions
│   ├── application/                   # 애플리케이션 레이어
│   │   ├── port/
│   │   │   ├── in/                    # Inbound Port (UseCase 인터페이스 + Command)
│   │   │   └── out/                   # Outbound Port (외부 시스템 인터페이스)
│   │   ├── service/                   # UseCase 구현체 (suffix: Service)
│   │   │   └── processor/             # 전략 패턴 구현체 (suffix: Processor)
│   │   └── listener/                  # 이벤트 리스너 (suffix: Listener)
│   └── infrastructure/                # 인프라 레이어 (Adapter)
│       ├── in/web/                    # Inbound Adapter
│       │   ├── {BC}Controller.kt      # REST Controller
│       │   ├── request/               # Request DTOs
│       │   └── response/              # Response DTOs
│       └── out/
│           ├── persistence/           # 자기 BC 영속성
│           │   ├── {Root}JpaEntity.kt
│           │   ├── {Root}JpaRepository.kt
│           │   └── {Root}RepositoryImpl.kt
│           └── acl/                   # 타 BC 번역 어댑터
│               └── {Target}Adapter.kt
└── shared/
    └── domain/
        └── value/                     # 공통 VO (Money, TenantId, PartnerId)
```

## Bounded Contexts

각 BC는 동일한 구조를 따른다:

- **attribution**: 전환 이벤트 수신, 귀속 판정/철회 (Core)
- **commission**: 커미션 산정/확정/취소/차감 (Core)
- **tracking**: 추적 링크, 추천 코드, 클릭 기록 (Supporting)
- **partner**: 파트너 계정, 소속, 티어 (Supporting)
- **statement**: 정산 주기 마감, 명세서 생성 (Supporting)
- **tenant**: 고객사 관리, API 키, 프로그램 설정 (Supporting)
- **notification**: 웹훅 발송 (Generic)

## Architectural Rules

### Layer Dependencies

- Domain은 Application이나 Infrastructure에 의존하지 않는다
- Domain은 Spring, JPA 등 프레임워크에 의존하지 않는다
- Application은 Domain에만 의존한다
- Infrastructure는 Application과 Domain에 의존한다

### Port & Adapter

- Inbound Port: UseCase 인터페이스 (`application/port/in/`)
- Outbound Port: 외부 시스템 인터페이스 (`application/port/out/`)
- Inbound Adapter: Controller (`infrastructure/in/web/`)
- Outbound Adapter: Repository 구현, 외부 API 클라이언트 (`infrastructure/out/`)
- Controller → Inbound Port(UseCase) → Domain ← Outbound Port ← Outbound Adapter

### Aggregate Rules

- Aggregate 간 직접 참조 금지 (ID value object로만 참조)
- 하나의 트랜잭션에서 하나의 Aggregate만 수정
- Aggregate 내부는 Root를 통해서만 접근

### BC 간 참조 규칙

| 레이어 | 타 BC 참조 |
|--------|-----------|
| `domain/` | **전면 금지** (shared만 허용) |
| `application/` | **전면 금지** (port/out으로 격리) |
| `infrastructure/out/acl/` | C-S 관계 BC의 value + ReadRepository만 허용 |

- Domain과 Application은 타 BC를 일절 참조하지 않는다
- 타 BC 데이터가 필요하면 `application/port/out/` 인터페이스를 정의하고, `infrastructure/out/acl/` Adapter에서 구현한다
- ACL Adapter가 타 BC ReadRepository를 호출하고, 자기 BC의 VO로 번역하여 반환한다
- ReadRepository는 Aggregate가 아닌 View(VO)를 반환한다
- 이벤트 리스너는 application/listener/에 위치한다
- 이벤트 리스너는 타 BC의 domain/event만 참조 가능 (Events 관계)
- shared/domain/value/ 는 모든 BC에서 사용 가능

### Naming Conventions

| 대상 | 접미사/패턴 | 위치 |
|------|------------|------|
| Aggregate Root | 도메인 명사 | `{bc}/domain/aggregate/` |
| Value Object | 도메인 명사 (data class / value class) | `{bc}/domain/value/` |
| Domain Service | 도메인 명사 | `{bc}/domain/service/` |
| Repository 인터페이스 | `*Repository` | `{bc}/domain/repository/` |
| UseCase 인터페이스 | `*UseCase` | `{bc}/application/port/in/` |
| UseCase 구현체 | `*FacadeService` 또는 `*Service` | `{bc}/application/service/` |
| Processor | `*Processor` | `{bc}/application/service/processor/` |
| Listener | `*Listener` | `{bc}/application/listener/` |
| Command | `*Command` | `{bc}/application/port/in/` (UseCase와 함께) |
| Controller | `*Controller` | `{bc}/infrastructure/in/web/` |
| JPA Entity | `*JpaEntity` | `{bc}/infrastructure/out/persistence/` |
| JPA Repository | `*JpaRepository` | `{bc}/infrastructure/out/persistence/` |
| Repository 구현체 | `*RepositoryImpl` | `{bc}/infrastructure/out/persistence/` |
| Domain Event | 과거분사 (`*Calculated`, `*Decided`) | `{bc}/domain/event/` |
| Domain Exception | `*Exception` (sealed class 하위) | `{bc}/domain/exception/` |

### JPA Entity Rules

- JPA Entity는 infrastructure 레이어에만 존재
- Domain 객체 ↔ JPA Entity 변환은 RepositoryImpl에서 수행
- JPA Entity에 비즈니스 로직 금지

## Multitenancy

- 모든 Entity에 `tenantId` 포함
- 쿼리 시 tenantId 필터 필수
- API 요청에서 인증된 tenantId를 추출하여 전달

## Test Structure

```
src/test/kotlin/com/partnercommission/
├── architecture/            # ArchUnit 아키텍처 테스트
├── integration/             # 통합 테스트 (@Tag("integration"))
│   ├── framework/           # BaseIntegrationTest, FlowExecutor, DbVerifier
│   └── scenario/            # 축 기반 시나리오 테스트
├── {bc}/
│   ├── domain/              # Domain 단위 테스트
│   └── application/         # Use Case 단위 테스트 (MockK)
└── shared/
```

## Domain Event

- `DomainEvent` 인터페이스: `eventId: UUID`, `occurredAt: LocalDateTime`
- Aggregate 내부에서 `registerEvent()` 호출
- RepositoryImpl에서 save 후 `getAndClearDomainEvents()` → `ApplicationEventPublisher` 발행
- 이벤트 리스너: `@Async @TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`
