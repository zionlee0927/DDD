# DDD 설계 & 구현 프로젝트

도메인 주도 설계(DDD)를 전략적 설계부터 전술적 구현까지 직접 수행하는 프로젝트.
하나의 도메인을 선정해 서브도메인 분석 → Event Storming → Bounded Context → Aggregate 설계 → 코드 구현까지 전 과정을 진행한다.

## 진행 중인 도메인

| 도메인 | 설명 | 설계 | 구현 |
|--------|------|------|------|
| [ecommerce](./ecommerce/README.md) | 인플루언서 중심 쇼핑몰 — 파트너가 추적 링크로 상품을 추천하고, 구매 귀속 판정 후 커미션을 정산받는 구조 | ✅ 완료 | 🚧 진행 중 |

## 설계 프로세스

각 도메인은 아래 순서로 설계한다:

```
0. 서브도메인 분석 (문제 공간 분해)
1. 유비쿼터스 언어 정의
2. Event Storming (이벤트 → 커맨드 → 정책)
3. Bounded Context 경계 식별
4. Entity / Value Object 도출
5. 불변식(Invariant) 식별
6. Aggregate 설계
7. Context Mapping (BC 간 관계)
```

## 구조

```
ddd/
├── docs/ddd-theory/    ← DDD 이론 정리 (12개 주제)
└── ecommerce/          ← 첫 번째 도메인
    ├── docs/           ← 전략적 설계 산출물 (00~07)
    └── src/            ← Kotlin + Spring Boot 구현
```

## 기술 스택

- Kotlin + Spring Boot 3.3
- Spring Data JPA
- H2 (개발) / PostgreSQL (추후)
- Hexagonal Architecture (Port & Adapter)
- 도메인 이벤트 기반 BC 간 통신
