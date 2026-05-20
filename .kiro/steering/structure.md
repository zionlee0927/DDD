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
│   │   │   ├── in/                    # Inbound Port (UseCase 인터페이스)
│   │   │   └── out/                   # Outbound Port (외부 시스템 인터페이스)
│   │   ├── service/                   # UseCase 구현체 (suffix: Service)
│   │   └── command/                   # Command DTOs
│   └── infrastructure/                # 인프라 레이어 (Adapter)
│       ├── in/web/                    # Inbound Adapter
│       │   ├── {BC}Controller.kt      # REST Controller
│       │   ├── request/               # Request DTOs
│       │   └── response/              # Response DTOs
│       └── out/persistence/           # Outbound Adapter
│           ├── {Root}JpaEntity.kt
│           ├── {Root}JpaRepository.kt
│           └── {Root}RepositoryImpl.kt
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

| 레이어 | 타 BC 허용 | 타 BC 금지 |
|--------|-----------|-----------|
| `domain/aggregate/`, `domain/value/` | 없음 | 전부 (shared만 사용 가능) |
| `domain/service/` | aggregate, value, event | repository, service, exception, application, infrastructure |
| `application/` | repository, event | aggregate, value, service, exception, application, infrastructure |
| `infrastructure/` | 없음 | 전부 (자기 BC만 참조) |

- Domain Service는 타 BC의 도메인 객체를 파라미터로 받을 수 있지만, Repository를 직접 가지지 않는다
- 타 BC 데이터 조회는 Application Service에서 수행하고, Domain Service에 전달한다
- 이벤트 리스너는 application/service/에 위치한다
- shared/domain/value/ 는 모든 BC에서 사용 가능

### Naming Conventions

| 대상 | 접미사/패턴 | 위치 |
|------|------------|------|
| Aggregate Root | 도메인 명사 | `{bc}/domain/aggregate/` |
| Value Object | 도메인 명사 (data class / value class) | `{bc}/domain/value/` |
| Domain Service | 도메인 명사 | `{bc}/domain/service/` |
| Repository 인터페이스 | `*Repository` | `{bc}/domain/repository/` |
| UseCase 인터페이스 | `*UseCase` | `{bc}/application/port/in/` |
| UseCase 구현체 | `*Service` | `{bc}/application/service/` |
| Controller | `*Controller` | `{bc}/infrastructure/in/web/` |
| JPA Entity | `*JpaEntity` | `{bc}/infrastructure/out/persistence/` |
| JPA Repository | `*JpaRepository` | `{bc}/infrastructure/out/persistence/` |
| Repository 구현체 | `*RepositoryImpl` | `{bc}/infrastructure/out/persistence/` |
| Domain Event | 과거분사 (`*Calculated`, `*Decided`) | `{bc}/domain/event/` |
| Domain Exception | `*Exception` (sealed class 하위) | `{bc}/domain/exception/` |
| Command | `*Command` | `{bc}/application/command/` |

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
├── {bc}/
│   ├── domain/              # Domain 단위 테스트
│   │   └── fixture/         # Test Fixture (object)
│   ├── application/         # Use Case 테스트
│   └── infrastructure/      # 통합 테스트
└── shared/
```
