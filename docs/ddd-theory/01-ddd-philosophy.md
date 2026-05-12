# DDD 이론 01: DDD 철학과 복잡성

## 핵심 개념

### Essential vs Accidental Complexity (Fred Brooks)
- **Essential Complexity**: 도메인 자체의 본질적 복잡성 (DDD가 집중하는 대상)
- **Accidental Complexity**: 기술적 선택으로 인한 우발적 복잡성

### 복잡성 관리 전략
- **분할 (Divide)**: 큰 문제를 작은 문제로 나눔
- **추상화 (Abstract)**: 핵심만 남기고 세부사항 숨김
- **캡슐화 (Encapsulate)**: 변경 영향 범위 제한

## Anemic vs Rich Domain Model

| Anemic Domain Model | Rich Domain Model |
|---|---|
| 데이터만 있는 빈약한 모델 | 행위를 가진 풍부한 모델 |
| 로직이 Service에 분산 | 로직이 도메인 객체에 응집 |
| 절차적 프로그래밍 | 객체지향 프로그래밍 |

## Knowledge Crunching

도메인 전문가의 암묵적 지식을 명시적 모델로 변환하는 반복적 협력 과정.

**Breakthrough**: Knowledge Crunching 중 새로운 개념이 기존 모델을 명쾌하게 정리하는 순간

## 전략적 설계 vs 전술적 설계

| 전략적 설계 | 전술적 설계 |
|---|---|
| Subdomain | Entity |
| Bounded Context | Value Object |
| Context Map | Aggregate |
| Ubiquitous Language | Repository, Factory, Domain Service, Domain Event |

## DDD 6대 원칙

1. 핵심 도메인 집중
2. 유비쿼터스 언어 구축
3. 모델-구현 연결
4. 지속적 정제
5. 명확한 경계
6. 도메인 전문가 협력

## DDD 적용 판단

- **적합**: 복잡한 비즈니스 규칙, 장기 유지보수, 도메인 전문가 접근 가능
- **과도**: 단순 CRUD, 빠른 프로토타입, 도메인 전문가 부재

## 점진적 도입

유비쿼터스 언어 구축 → 경계 식별 → Core Domain 적용 → 확장
