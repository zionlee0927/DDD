# Project Structure & Architecture

## Hexagonal Architecture Layers

```
Infrastructure(Adapter) → Application → Domain
```

Domain은 핵심이며 외부 레이어에 대한 의존성이 없다.

## Package Structure

```
com.partnercommission/
├── {bc}/                        # BC별 패키지 (attribution, commission, tracking 등)
│   ├── domain/                  # 도메인 레이어 (순수 Kotlin, 프레임워크 의존 없음)
│   │   ├── {Aggregate}.kt      # Aggregate Root
│   │   ├── {VO}.kt             # Value Objects
│   │   ├── {Enum}.kt           # Domain Enums
│   │   ├── {BC}Repository.kt   # Repository 인터페이스 (Port)
│   │   ├── event/              # Domain Events
│   │   └── exception/          # Domain Exceptions
│   ├── application/             # 애플리케이션 레이어 (Use Case)
│   │   ├── {UseCase}.kt        # Application Service
│   │   └── command/            # Command DTOs
│   └── infrastructure/          # 인프라 레이어 (Adapter)
│       ├── persistence/         # Repository 구현 (JPA)
│       │   ├── {Root}JpaEntity.kt
│       │   ├── {Root}JpaRepository.kt
│       │   └── {Root}RepositoryImpl.kt
│       └── web/                 # REST API (Inbound Adapter)
│           ├── {BC}Controller.kt
│           ├── request/
│           └── response/
└── shared/                      # Shared Kernel
    └── domain/
        ├── Money.kt
        ├── TenantId.kt
        └── PartnerId.kt
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

### Aggregate Rules

- Aggregate 간 직접 참조 금지 (ID value object로만 참조)
- 하나의 트랜잭션에서 하나의 Aggregate만 수정
- Aggregate 내부는 Root를 통해서만 접근

### Naming Conventions

| 대상 | 접미사/패턴 | 위치 |
|------|------------|------|
| Aggregate Root | 도메인 명사 | `{bc}.domain` |
| Value Object | 도메인 명사 (data class / value class) | `{bc}.domain` |
| Repository 인터페이스 | `*Repository` | `{bc}.domain` |
| Application Service | `*UseCase` | `{bc}.application` |
| Controller | `*Controller` | `{bc}.infrastructure.web` |
| JPA Entity | `*JpaEntity` | `{bc}.infrastructure.persistence` |
| JPA Repository | `*JpaRepository` | `{bc}.infrastructure.persistence` |
| Repository 구현 | `*RepositoryImpl` | `{bc}.infrastructure.persistence` |
| Domain Event | 과거분사 (`*Calculated`, `*Decided`) | `{bc}.domain.event` |
| Domain Exception | `*Exception` (sealed class 하위) | `{bc}.domain.exception` |
| Command | `*Command` | `{bc}.application.command` |

### JPA Entity Rules

- JPA Entity는 infrastructure 레이어에만 존재
- Domain 객체 ↔ JPA Entity 변환은 RepositoryImpl에서 수행
- JPA Entity에 비즈니스 로직 금지

## Query Service Pattern

### 규칙

- 조회 전용 서비스는 `*QueryService`로 명명
- `@Transactional(readOnly = true)` 필수
- Domain 객체 대신 DTO 프로젝션 반환 가능
- CUD 작업 금지

## Multitenancy

- 모든 Entity에 `tenantId` 포함
- 쿼리 시 tenantId 필터 필수
- API 요청에서 인증된 tenantId를 추출하여 전달

## Test Structure

```
src/test/kotlin/com/partnercommission/
├── {bc}/
│   ├── domain/              # Domain 단위 테스트
│   │   └── fixture/         # Test Fixture (object)
│   ├── application/         # Use Case 테스트
│   └── infrastructure/      # 통합 테스트
└── shared/
```
