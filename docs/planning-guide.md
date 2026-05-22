# 플래닝 가이드라인

## 계층 구조

```
Epic (BC/기능 단위)
└── Story (유즈케이스 단위)
    └── Task (구현 단위: 설계/구현/테스트)
```

## 단위 기준

| 계층 | 크기 | 예시 |
|------|------|------|
| Epic | BC 하나 또는 기술 개선 하나 | Statement BC, 멀티 모듈 분리 |
| Story | 유즈케이스 하나 (AC 포함) | 정산 항목 추가, 주기 마감 |
| Task | 커밋 1~3개로 끝나는 작업 | 도메인 설계, API 구현, 테스트 |

## 폴더 구조

```
{project}/docs/planning/
├── README.md                    ← 로드맵 + 에픽 상태
├── epic-01-statement.md
├── epic-02-multi-module.md
└── epic-03-outbox.md
```

## 상태

| 기호 | 의미 |
|------|------|
| ⬜ | 미착수 |
| 🔲 | 진행중 |
| ✅ | 완료 |

## 커밋 연결

```
feat(statement): Statement aggregate [E01-S01]
```

## 진행 원칙

1. 에픽 순서대로 (병렬 금지)
2. 스토리 완료 = AC 통과
3. 에픽 완료 시 README 업데이트
