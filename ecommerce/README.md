# ecommerce

인플루언서 중심 쇼핑몰 — 파트너가 추적 링크로 상품을 추천하고, 구매 귀속 판정 후 커미션을 정산받는 구조.

## 설계 과정

아래 순서로 전략적 설계를 진행했다:

| # | 단계 | 산출물 |
|---|------|--------|
| 0 | [서브도메인 분석](./docs/00-subdomain.md) | 문제 공간 분해, Core/Supporting/Generic 식별 |
| 1 | [유비쿼터스 언어](./docs/01-ubiquitous-language.md) | BC별 용어집 (한국어 ↔ 코드 매핑) |
| 2 | [Event Storming](./docs/02-event-storming.md) | 32개 이벤트, 커맨드, 정책 도출 |
| 3 | [Bounded Context](./docs/03-bounded-context.md) | 10개 BC 경계 및 분리 근거 |
| 4 | [Entity / VO](./docs/04-entity-vo.md) | Aggregate Root, 내부 Entity, Value Object 분류 |
| 5 | [불변식](./docs/05-invariants.md) | Aggregate 내 불변 규칙, 상태 전이도 |
| 6 | [Aggregate](./docs/06-aggregate.md) | 13개 Aggregate 설계 |
| 7 | [Context Mapping](./docs/07-context-mapping.md) | BC 간 통신 패턴 (OHS, C-S, Events, ACL) |

## 도메인 개요

```
인플루언서 → 추적 링크로 상품 추천
소비자 → 추적 링크 클릭 → 쇼핑몰에서 구매
쇼핑몰 → 귀속 판정 → 인플루언서에게 커미션 정산
```

### Core Domain (경쟁 우위)

| BC | 역할 |
|----|------|
| Partner | 파트너 등록, 티어 관리, 추적 링크 발급 |
| Tracking | 클릭 기록, 귀속 판정 (라스트클릭, 30일 윈도우) |
| Commission | 커미션 산정/확정, 정산 주기별 지급 |

### Supporting / Generic

| BC | 역할 | 유형 |
|----|------|------|
| Order | 주문 생성, 상태 관리, 취소/환불 | Supporting |
| Cart | 장바구니 | Supporting |
| Catalog | 상품 정보, 카테고리 | Supporting |
| Inventory | 재고 선점/차감/복원 | Supporting |
| Shipping | 배송 상태 추적 | Supporting |
| Promotion | 쿠폰, 할인 규칙 | Supporting |
| Payment | PG 연동, 결제/환불 | Generic |

### 핵심 흐름

```
추적 링크 클릭 → 장바구니 → 주문 생성
→ 재고 선점 → 결제 → 주문 확정
→ 귀속 판정(PENDING) → 커미션 산정
→ 배송 완료 → 귀속 확정 → 커미션 확정
→ 정산 주기 도래 → 정산 완료 → 티어 재평가
```

### Context Mapping

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           External Systems                               │
│  ┌──────────┐  ┌──────────┐                                            │
│  │   PG사   │  │ 외부 물류사│                                            │
│  │(토스,페이팔)│  │          │                                            │
│  └────┬─────┘  └────┬─────┘                                            │
│       │ACL          │ACL                                                │
└───────┼─────────────┼───────────────────────────────────────────────────┘
        │             │
        ▼             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Generic / Supporting                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Payment  │  │ Shipping │  │ Catalog  │  │Promotion │               │
│  │ Context  │  │ Context  │  │ Context  │  │ Context  │               │
│  │          │  │          │  │  [OHS]   │  │          │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────────┘
        │             │             │             │
        │Partnership  │Events       │OHS          │C-S
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Order Domain                                   │
│                                                                          │
│  ┌──────────┐     C-S      ┌──────────┐     C-S      ┌──────────┐     │
│  │   Cart   │─────────────→│  Order   │─────────────→│Inventory │     │
│  │ Context  │              │ Context  │              │ Context  │     │
│  └──────────┘              └────┬─────┘              └──────────┘     │
│       ↑                         │                         ↑            │
│       │ Events                  │ Events                  │ Events     │
│       └─────────────────────────┤                         │            │
│                                 │                    ┌────┴─────┐      │
│                                 │                    │ Shipping │      │
│                                 │                    └──────────┘      │
└─────────────────────────────────┼───────────────────────────────────────┘
                                  │
                                  │ Events
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Core Domain                                    │
│                                                                          │
│  ┌──────────┐     C-S      ┌──────────┐    Events    ┌──────────┐     │
│  │ Partner  │─────────────→│ Tracking │─────────────→│Commission│     │
│  │ Context  │              │ Context  │              │ Context  │     │
│  └──────────┘              └──────────┘              └────┬─────┘     │
│       ↑                                                   │            │
│       │ Events                                            │            │
│       └───────────────────────────────────────────────────┘            │
│                          (정산 완료 → 티어 재평가)                        │
└─────────────────────────────────────────────────────────────────────────┘
```

## 기술 스택

- Kotlin 1.9 / Java 21
- Spring Boot 3.3
- Spring Data JPA + H2 (개발용)

## 실행

```bash
./gradlew bootRun
```

## 사전 준비

Java 21 필요. `gradle.properties`에 JAVA_HOME 경로 설정:
```properties
org.gradle.java.home=/path/to/java-21
```
