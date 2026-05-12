# DDD 이론 05: Context Mapping 심화

> "Context Map은 프로젝트에 관련된 Bounded Context들과 그들 사이의 관계를 보여주는 문서다. 이것은 현재 상태를 있는 그대로 보여줘야 한다. 이상적인 상태가 아니라." — Eric Evans

## 1. Context Map의 본질

단순한 아키텍처 다이어그램이 아니라, **팀 간의 관계, 권력 구조, 통합 전략**을 명시적으로 드러내는 전략적 설계 도구.

### Context Map이 보여주는 것

**기술적 측면:**
- Bounded Context 목록
- Context 간 데이터 흐름 방향
- 통합 방식 (API, 이벤트, 공유 DB)
- 의존성 방향과 강도

**조직적 측면:**
- 팀 간 협력 관계
- 권력과 협상력의 분포
- 의사결정 권한
- 커뮤니케이션 패턴

> "Context Map은 기술적 다이어그램이 아니다. 그것은 조직의 정치적 지형도다." — Alberto Brandolini

### Context Map vs 다른 다이어그램

| 다이어그램 | 초점 | Context Map과의 차이 |
|---|---|---|
| 시스템 아키텍처 | 기술 컴포넌트, 인프라 | 팀 관계, 통합 패턴 미포함 |
| ERD | 데이터 구조 | 비즈니스 경계, 소유권 미포함 |
| 서비스 맵 | 서비스 간 호출 | 협력 패턴, 권력 관계 미포함 |
| Context Map | 비즈니스 경계 + 팀 관계 + 통합 전략 | 전략적 관점의 통합 뷰 |

---

## 2. 협력 패턴 심화

### Partnership 심화

두 팀이 **공동 목표**를 위해 긴밀 협력. 함께 성공하거나 함께 실패.

**성공 조건:**
- 같은 관리자 아래 또는 긴밀한 관계
- 공동 스프린트 계획, 동기화된 릴리스
- 정기적 합동 회의 (주 1회 이상)
- 공유된 성공 지표 (KPI)

**실패 신호:**
- 한 팀이 다른 팀을 기다리며 블로킹
- 인터페이스 변경 시 갈등
- 릴리스 일정 불일치

**⚠️ 주의:** Partnership은 유지 비용이 높음. 정말 필요한 경우에만 선택.

### Shared Kernel 심화

두 Context가 도메인 모델 일부를 공유. 변경 시 양쪽 합의 필요.

**공유해도 좋은 것:**
- 범용 Value Object (Money, Address)
- 기본 이벤트 인터페이스
- 공통 유틸리티 타입
- 변경 빈도가 낮은 것

**공유하면 안 되는 것:**
- Entity (식별성이 Context마다 다름)
- 비즈니스 규칙
- Context 특화 로직
- 자주 변경되는 것

```
shared-kernel/
├── domain/
│   ├── Money.kt           // 금액 VO
│   ├── Address.kt         // 주소 VO
│   └── DateRange.kt       // 기간 VO
├── events/
│   └── DomainEvent.kt     // 기본 이벤트 인터페이스
└── types/
    ├── Result.kt          // 결과 타입
    └── EntityId.kt        // ID 기본 타입
```

> "Shared Kernel은 가능한 작게 유지하라. 공유하는 것이 많아질수록 두 팀의 자율성은 줄어든다." — Eric Evans

---

## 3. 상류/하류 패턴 심화

### Customer-Supplier: Consumer-Driven Contract

Downstream(고객)이 기대하는 계약을 먼저 정의하고, Upstream(공급자)이 이를 만족시키는 방식.

```
협력 프로세스:
1. Downstream이 API 요구사항 전달
2. 양 팀이 우선순위 협상
3. Downstream이 계약(Contract) 정의
4. Upstream이 구현, 계약 테스트로 검증
5. Upstream 먼저 배포 (하위 호환 유지)
```

### Conformist vs ACL 선택 기준

| 기준 | Conformist | ACL |
|---|---|---|
| 외부 모델 품질 | 좋음, 우리 도메인과 유사 | 나쁨, 우리 도메인과 다름 |
| 변경 빈도 | 낮음, 안정적 | 높음, 자주 변경 |
| 구현 비용 | 낮음 | 높음 (번역 계층 필요) |
| 유지보수 비용 | 외부 변경에 취약 | 외부 변경 격리 |
| 도메인 순수성 | 오염 가능 | 보호됨 |

**실무 가이드:**
- 표준 프로토콜 (OAuth, OpenID) → Conformist
- 잘 설계된 외부 API (Stripe) → Conformist 또는 얇은 ACL
- 레거시 시스템 → ACL 필수
- Core Domain과 통합 → ACL로 보호

---

## 4. Open Host Service + Published Language

### Open Host Service (OHS)

Upstream이 **여러 Downstream을 위한 표준화된 서비스** 제공.

```
┌─────────────────┐      ┌─────────────────┐
│ Product Context │─────→│  Order Context  │
│   [Open Host]   │─────→│ Search Context  │
│                 │─────→│Analytics Context│
│  REST API v2    │─────→│  Mobile App     │
└─────────────────┘      └─────────────────┘
```

구현 옵션:
- REST API (가장 범용)
- GraphQL (유연한 쿼리)
- gRPC (고성능)
- Event Stream (비동기)

### Published Language (PL)

Context 간 통신을 위한 **잘 문서화된 공유 스키마**.

| 도구 | 용도 |
|---|---|
| OpenAPI (Swagger) | REST API 스키마 |
| AsyncAPI | 이벤트/메시지 스키마 |
| Protocol Buffers | gRPC 스키마 |
| JSON Schema | 데이터 검증 |
| Avro | 이벤트 스트리밍 |

### 스키마 진화 전략

| 변경 유형 | 안전성 | 대응 |
|---|---|---|
| 필드 추가 (optional) | ✓ 안전 | 그냥 추가 |
| 필드 삭제 | ✗ 위험 | deprecated 마킹 → 다음 메이저 버전에서 제거 |
| 필드 타입 변경 | ✗ 위험 | 새 필드 추가 + 기존 유지 |
| 새 버전 발행 | ✓ 안전 | v1 유지 + v2 추가 |

**권장:** Schema Registry 사용 (Confluent, AWS Glue, Apicurio)

---

## 5. Team Topologies와 Context Mapping

### Conway의 법칙

> "시스템을 설계하는 조직은 그 조직의 커뮤니케이션 구조를 복제한 설계를 만들어낸다."

**역 Conway 기동:** 원하는 시스템 구조에 맞게 조직을 설계한다.

### 4가지 팀 유형

| 팀 유형 | 역할 | 예시 |
|---|---|---|
| Stream-aligned | 비즈니스 가치 흐름에 정렬 | 주문팀, 결제팀 |
| Platform | 내부 서비스 제공 | 인프라팀, DevOps |
| Enabling | 다른 팀 역량 향상 지원 | 아키텍처팀, SRE |
| Complicated Subsystem | 전문 지식 필요 영역 | ML팀, 보안팀 |

### 팀 상호작용 → Context Mapping 패턴

| 상호작용 모드 | Context Mapping 패턴 | 설명 |
|---|---|---|
| Collaboration | Partnership / Shared Kernel | 긴밀 협력, 공동 소유 |
| X-as-a-Service | OHS + Published Language | 표준 API 제공/소비 |
| Facilitating | ACL 지원 | 전환 지원, 임시적 |

---

## 6. Context Map 진화와 마이그레이션

### 패턴 전환 시나리오

**Conformist → ACL:**
- 외부 모델이 도메인을 오염시키기 시작할 때
- 외부 변경이 잦아져 내부 안정성이 필요할 때

**Partnership → Customer-Supplier:**
- 팀 간 동기화 비용이 너무 클 때
- 한 팀이 더 빠르게 움직여야 할 때

**Big Ball of Mud → 여러 Context:**
- ACL로 격리 → 점진적으로 Context 추출
- Strangler Fig 패턴 적용

### 마이그레이션 원칙

1. 점진적으로 (Big Bang 금지)
2. 롤백 가능하게
3. 비즈니스 연속성 유지
4. 한 번에 하나의 관계만 변경

---

## 7. Context Map 문서화

### 실무 가이드라인

- Context Map을 **Git에 저장** (코드와 함께 버전 관리)
- **분기별 리뷰** 세션 진행
- 새로운 Context 추가 시 관계 패턴 명시적으로 결정
- 팀 변경 시 소유권과 관계 재검토
- **ADR(Architecture Decision Record)**과 연결

### Context Map 표기법

```
┌─────────────┐  Customer-Supplier  ┌─────────────┐
│   Product   │────────────────────→│    Order    │
│  (Upstream) │                     │(Downstream) │
└─────────────┘                     └─────────────┘

┌─────────────┐       ACL          ┌─────────────┐
│   Legacy    │─────────────────────│  New System │
│  (Upstream) │                     │(Downstream) │
└─────────────┘                     └─────────────┘

┌─────────────┐    Partnership     ┌─────────────┐
│    Order    │◄──────────────────►│   Payment   │
└─────────────┘                    └─────────────┘
```

---

## 핵심 정리

1. Context Map = 기술 + 조직 + 전략의 통합 뷰
2. **현재 상태**를 있는 그대로 보여야 함 (이상적 상태 아님)
3. Partnership은 유지 비용 높음 → 정말 필요한 경우에만
4. Shared Kernel은 가능한 작게 (범용 VO, 이벤트 인터페이스만)
5. Conformist vs ACL: 외부 모델 품질과 도메인 보호 필요성으로 판단
6. OHS + Published Language: 여러 소비자를 위한 표준 API + 스키마
7. Team Topologies와 연계하여 조직 구조 = 시스템 구조
8. Context Map은 살아있는 문서 — Git 저장, 분기별 리뷰
