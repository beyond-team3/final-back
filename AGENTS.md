AGENTS.md — SeedFlow+ 통계 기능 백엔드 구현 작업 규칙

이 파일은 Codex가 통계(Statistics) 백엔드 기능을 구현할 때 반드시 따라야 하는 규칙입니다.
**코드를 수정할 때마다** 아래 "작업 완료 후 루틴"을 실행해야 합니다.

---

## 구현 범위 및 전제

### 대상 도메인
- `billing/revenue`: 청구 매출 통계 (월별 / 품종별 / 월별+품종별) — **이미 구현됨, 보완 대상**
- 추후 추가될 도메인은 별도 AGENTS.md 세션에서 처리

### 현재 구현된 API (base path: `/api/v1/statistics/billing/revenue`)

| 엔드포인트 | 설명 | 상태 |
|---|---|---|
| `GET /monthly` | 월별 청구 매출 | ✅ 구현 완료 |
| `GET /by-category` | 품종별 청구 매출 | ✅ 구현 완료 |
| `GET /monthly-by-category` | 월별/품종별 청구 매출 | ✅ 구현 완료 |


### 공통 쿼리 파라미터
- `from` (required): ISO date (예: `2024-01-01`)
- `to` (required): ISO date
- `category` (optional): 품종 필터

### 응답 포맷
```json
{
  "success": true,
  "data": [ ... ]
}
```
→ `ApiResult.success(...)` 래핑 방식 유지

### 인보이스 필터 조건 (모든 집계에 공통 적용)
- 인보이스 상태: `PUBLISHED`, `PAID` 만 포함
- `included = true`
- `statement.status = ISSUED`

### 서비스 검증 규칙
- `fromDate`, `toDate` 필수
- `fromDate <= toDate`
- 조회 범위 최대 24개월 (`MAX_RANGE_MONTHS = 24`)
- 위반 시 `CoreException(ErrorType.INVALID_INPUT_VALUE)` 발생

### 중복 집계 방지 패턴 (QueryDSL, 반드시 유지)
- **품종별**: `invoice + category` 단위 group → Java에서 재집계
- **월별+품종별**: `invoice + month + category` 단위 group → Java에서 재집계

---

## 작업 순서 (반드시 이 순서를 따를 것)

한 번에 전체를 수정하지 않고 아래 순서대로 한 단계씩 진행합니다.
각 단계 완료 후 컴파일 확인 → 완료 후 루틴 실행 → 다음 단계로 진행합니다.

```
1. BillingRevenueStatisticsRepository 읽기 (기존 QueryDSL 집계 구조 파악, 수정 없음)
2. BillingRevenueStatisticsFilter 검증 로직 확인 (24개월 제한, from/to 필수 여부)
3. BillingRevenueStatisticsQueryService 읽기 (필터 위임 구조 파악, 수정 없음)
4. BillingRevenueStatisticsController 읽기 (파라미터 바인딩, 응답 포맷 확인)
5. 보완 작업: Filter / Service / Repository 수정이 필요한 경우 처리
6. 컨트롤러 테스트 보완 (BillingRevenueStatisticsControllerTest)
7. 서비스 테스트 보완 (BillingRevenueStatisticsQueryServiceTest)
8. 리포지토리 집계 테스트 작성 (신규)
```

---

## 집계 처리 공통 순서 (Repository 내부)

모든 집계 메서드는 아래 순서를 반드시 따릅니다.

1. 공통 조건 적용 (인보이스 상태 / 날짜 범위 / `included` / `statement.status`)
2. `category` 파라미터가 존재할 경우에만 조건 추가
3. QueryDSL 집계 실행
4. Java 재집계로 중복 집계 방지 처리
5. DTO 변환 후 반환

> ⚠️ 4번(Java 재집계) 생략 금지 — QueryDSL 단일 group by 결과를 그대로 반환하면 중복 집계 발생.

---

## 작업 완료 후 루틴 (코드 수정 시 매번 실행)

### Step 1. 아키텍처 문서 업데이트 (`docs/statistics/statistics-architecture.md`)

구조가 바뀐 경우에만 업데이트합니다.
구조 변경의 기준: 메서드 시그니처 변경, 새 클래스 추가, 의존 방향 변경.

파일 하단에 아래 블록을 추가합니다:

```markdown
## [YYYY-MM-DD] <변경 제목>

### 변경 대상
- 파일: <경로>
- 클래스/메서드: <이름>

### 변경 내용
<무엇이 어떻게 바뀌었는지 2~5줄>

### 변경 이유
<정책 번호 또는 이유 한 줄>
```

### Step 2. 작업 로그 기록 (`docs/statistics/statistics-work-log.md`)

모든 코드 수정 후 예외 없이 기록합니다.
파일 하단에 아래 블록을 추가합니다:

```markdown
## [YYYY-MM-DD HH:MM] <작업 제목>

### 작업 내용
- 수정 파일: <경로> — <한 줄 설명>

### 컴파일 결과
- [ ] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
<이어서 할 작업 또는 "없음">
```

### Step 3. 커밋 (`COMMIT_PROMPT.md` 규칙에 따라 실행)

Step 1~2 완료 후 커밋합니다. `COMMIT_PROMPT.md`를 읽고 그 형식을 따릅니다.

---

## 멈춤 조건

아래 상황이 발생하면 작업을 멈추고 보고합니다. 임의로 해결하지 않습니다.

- 인보이스 상태값(`PUBLISHED`, `PAID`) 외 다른 상태를 포함해야 하는 케이스 발견
- 중복 집계 방지 없이 집계 결과가 정확히 나오는 것처럼 보이는 케이스 발견
- `BillingRevenueStatisticsQueryService` 외 다른 서비스에 집계 로직이 이미 구현되어 있는 경우
- 컴파일 오류 발생
- 테스트 실패

---

## 범위 외 (이 세션에서 하지 말 것)

- 프론트엔드 연동 작업 (`SalesRepStatsView.vue`, `statistics.js`, Pinia 스토어)
- `personal`, `by-client`, `by-employee` 등 신규 통계 도메인 구현
- `/statistics/billing/revenue` 구 경로 유지 또는 fallback 처리
- 기존 테스트 코드 삭제
- 승인(Approval) 관련 로직 수정
- `AGENTS.md` 자체 수정
