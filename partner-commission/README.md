# partner-commission

파트너 커미션 SaaS — 추천 귀속 판정과 커미션 계산을 API로 제공하는 Headless 서비스.

## 핵심 가치

> "파트너한테 돈 잘못 줄 일이 없다"

```
✅ 우리가 해주는 것: 추적 → 귀속 판정 → 커미션 계산 → 명세서
❌ 우리가 안 하는 것: 실제 송금, 원천징수, 세금계산서
```

## 연동 흐름

```
1. 고객사 → [추적 링크 발급 API] → 파트너에게 전달
2. 소비자 → 링크 클릭 → 우리 서버(클릭 기록) → 고객사로 리다이렉트 (?s_id=click_id)
3. 고객사 → [전환 이벤트 API] (s_id 포함) → 귀속 판정 + 커미션 산정
4. 고객사 → [취소 이벤트 API] → 커미션 자동 회수
5. 정산일 → [명세서 조회 API] → 고객사가 확인 후 직접 지급
```

## 설계 원칙

- 전환 이벤트 타입은 고객사가 자유롭게 정의 (`PAYMENT`, `VISIT` 등)
- 소비자 혜택(쿠폰, QR 등)은 고객사 몫
- 중복 전환 제한은 고객사가 설정하는 규칙

## MVP 범위

**포함:** 테넌트 관리, 파트너 관리, 추적 링크/클릭, 귀속 판정, 커미션 규칙/산정/확정/취소, 명세서, 웹훅

**제외:** 실제 송금, 세금, 오프라인 귀속(OCR/POS), 대시보드 UI, 멀티 통화

## 이커머스 프로젝트와의 관계

```
[ecommerce] ─── 첫 번째 고객사 ───→ [partner-commission]
```

## 설계 과정

| # | 단계 | 산출물 |
|---|------|--------|
| 0 | [서브도메인 분석](./docs/design/00-subdomain.md) | 문제 공간 분해 |
| 1 | [유비쿼터스 언어](./docs/design/01-ubiquitous-language.md) | BC별 용어집 |
| 2 | [Event Storming](./docs/design/02-event-storming.md) | 이벤트, 커맨드, 정책 |
| 3 | [Bounded Context](./docs/design/03-bounded-context.md) | BC 경계 |
| 4 | [Entity / VO](./docs/design/04-entity-vo.md) | Entity, Value Object |
| 5 | [불변식](./docs/design/05-invariants.md) | 불변 규칙 |
| 6 | [Aggregate](./docs/design/06-aggregate.md) | Aggregate 설계 |
| 7 | [Context Mapping](./docs/design/07-context-mapping.md) | BC 간 통신 |

## 아키텍처

### Hexagonal Architecture

```
Infrastructure(Adapter) → Application → Domain
```
ㄹ
### BC 간 참조 규칙

```
domain/        → 타 BC 전면 금지 (shared만 허용)
application/   → 타 BC 전면 금지 (port/out으로 격리)
infrastructure/out/acl/ → C-S 관계 BC의 value + ReadRepository만 허용
```

타 BC 데이터가 필요하면:
1. `application/port/out/`에 인터페이스 정의
2. `infrastructure/out/acl/`에서 타 BC ReadRepository 호출 + 자기 BC VO로 번역

### ArchUnit 아키텍처 테스트

| 테스트 | 검증 내용 |
|--------|-----------|
| HexagonalArchitectureTest | Domain → Application → Infrastructure 레이어 의존 방향 |
| LayerDependencyTest | Domain이 Spring/JPA에 의존하지 않음 |
| ModuleBoundaryTest | BC 간 참조 규칙 (domain/application 타 BC 금지, infrastructure ACL만 허용) |
| NamingConventionTest | UseCase, Service, Controller, JpaEntity 등 네이밍 규칙 |

### Attribution BC 구조

```
attribution/
├── domain/                     # 순수 (타 BC 의존 0)
│   ├── aggregate/              # AttributionDecision, ConversionEvent
│   ├── value/                  # Evidence, Config, Result, EvidenceData, Ids
│   ├── service/                # AttributionJudges, AttributionDecisionFactory
│   ├── event/                  # AttributionDecided, AttributionFailed, AttributionRevoked
│   ├── exception/              # sealed class AttributionException
│   └── repository/             # ConversionEventRepository, AttributionDecisionRepository
├── application/                # 오케스트레이션 (타 BC 의존 0)
│   ├── port/in/                # ReceiveConversionUseCase + Command
│   ├── port/out/               # LoadTenantConfigPort, LoadClickPort, LoadReferralCodePort
│   └── service/
│       ├── ReceiveConversionFacadeService.kt
│       └── processor/          # ClickAttributionProcessor, ReferralCodeAttributionProcessor
└── infrastructure/
    ├── in/web/                 # REST Controller
    └── out/
        ├── acl/                # ACL 어댑터 (타 BC 번역)
        │   ├── TenantConfigAdapter.kt
        │   └── TrackingDataAdapter.kt
        └── persistence/        # JPA Entity, RepositoryImpl, 이벤트 발행
```
