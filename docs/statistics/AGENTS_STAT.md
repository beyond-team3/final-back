# AGENTS.md — SeedFlow+ 통계 기능 전면 재구성 (공통 통계 API)

이 파일은 Codex가 통계(Statistics) 기능을 전면 재구성할 때 반드시 따라야 하는 규칙입니다.
**코드를 수정할 때마다** 아래 "작업 완료 후 루틴"을 실행해야 합니다.

---

## 배경 및 전제

기존 `billing statistics`(`/api/v1/statistics/billing/revenue/**`)는 청구 기준·월/품종 축에
묶여 있어 프론트 요구(사원/거래처/품종 + 추이/랭킹 + 개인/관리자)를 담을 수 없습니다.
새 통계 모듈을 `/api/v1/statistics/**` 기준으로 별도 구성하고,
기존 `billing statistics`는 새 API 안정화 후 별도 세션에서 정리합니다.

---

## 확정된 정책 결정 사항

| 항목 | 결정 |
|---|---|
| 매출 기준 | `Invoice.status = PAID` 단일 기준 |
| sales-rep API | 본인(SALES_REP) 1명의 매출 추이 |
| admin API | 전체 사원 합산 매출 추이 |
| SALES_REP 권한 범위 | by-client / by-variety / ranking — 본인 담당 데이터 범위로 조회 가능 |
| ADMIN 권한 범위 | by-employee / by-client / by-variety / ranking — 전체 범위 조회 가능 |
| period 포맷 | 월별: `YYYY-MM` (예: `2026-01`) / 분기별: `YYYY-QN` (예: `2026-Q1`) |
| 빈 구간 처리 | 매출 0인 월/분기도 응답에 포함 (Chart.js 라벨-데이터 정렬 보장) |
| 선택 필터 빈 요청 | `employeeIds` / `clientIds` / `varietyCodes` 최소 1개 필수. 빈 요청 → 400 거부 |
| 품종 식별자 | `ProductCategory code` 문자열 사용 (`varietyCodes` 파라미터명) |
| 드롭다운 API | 기존 API 재사용 (별도 alias endpoint 추가 없음) |

> 이 결정 사항은 이 세션 전체에서 변경하지 않습니다.
> 변경이 필요하다고 판단되면 작업을 멈추고 보고합니다.

---

## 구현 범위

### 컨트롤러 — 신규 엔드포인트

| 메서드 | 경로 | 허용 역할 | 설명 |
|---|---|---|---|
| GET | `/api/v1/statistics/sales-rep` | SALES_REP, ADMIN | 본인 매출 추이 |
| GET | `/api/v1/statistics/admin` | ADMIN | 전체 합산 매출 추이 |
| GET | `/api/v1/statistics/by-employee` | ADMIN | 사원별 매출 추이 |
| GET | `/api/v1/statistics/by-client` | SALES_REP, ADMIN | 거래처별 매출 추이 |
| GET | `/api/v1/statistics/by-variety` | SALES_REP, ADMIN | 품종별 매출 추이 |
| GET | `/api/v1/statistics/ranking` | SALES_REP, ADMIN | 축별 랭킹 |

> SALES_REP가 by-client / by-variety / ranking 조회 시
> 서비스 레이어에서 본인 담당 데이터 범위로 자동 필터링합니다.
> 컨트롤러에서 직접 필터링하지 않습니다.

### 공통 요청 파라미터

| 파라미터 | 타입 | 필수 여부 | 설명 |
|---|---|---|---|
| `from` | ISO date | 필수 | 조회 시작일 |
| `to` | ISO date | 필수 | 조회 종료일 |
| `period` | enum | 필수 | `MONTHLY` / `QUARTERLY` |
| `employeeIds` | Long[] | by-employee 전용 | 최소 1개 필수 |
| `clientIds` | Long[] | by-client 전용 | 최소 1개 필수 |
| `varietyCodes` | String[] | by-variety 전용 | 최소 1개 필수 (ProductCategory code) |
| `type` | enum | ranking 전용 | `EMPLOYEE` / `CLIENT` / `VARIETY` |
| `limit` | int | ranking 선택 | 기본값 10, 최대 50 |

### 공통 응답 모델

**시계열 응답** (`sales-rep`, `admin`, `by-employee`, `by-client`, `by-variety`)
```json
[
  {
    "targetId": "식별자 (사원/거래처 ID 또는 품종 code)",
    "targetName": "표시 이름",
    "data": [
      { "period": "2026-01", "sales": 0 },
      { "period": "2026-02", "sales": 1500000 }
    ]
  }
]
```

**랭킹 응답** (`ranking`)
```json
[
  { "rank": 1, "targetId": "식별자", "targetName": "이름", "sales": 5000000 },
  { "rank": 2, "targetId": "식별자", "targetName": "이름", "sales": 3200000 }
]
```

> 응답 최외곽은 `ApiResult.success(...)` 래핑을 유지합니다.

### 서비스

- 클래스명: `StatisticsQueryService`
- 내부 메서드:
  - `getMySalesTrend(principal, filter)`
  - `getAdminSalesTrend(filter)`
  - `getSalesTrendByEmployee(filter)` — ADMIN 전용
  - `getSalesTrendByClient(principal, filter)` — SALES_REP는 본인 담당 범위 자동 필터
  - `getSalesTrendByVariety(principal, filter)` — SALES_REP는 본인 담당 범위 자동 필터
  - `getRanking(principal, filter)` — SALES_REP는 본인 담당 범위 자동 필터

### 리포지토리

- QueryDSL 기반으로 기존 billing statistics와 일관화
- 클래스명: `StatisticsRepository`

---

## 작업 순서 (반드시 이 순서를 따를 것)

한 번에 전체를 수정하지 않고 아래 순서대로 한 단계씩 진행합니다.
각 단계 완료 후 컴파일 확인 → 완료 후 루틴 실행 → 다음 단계로 진행합니다.

```
1. 매출 기준 확인 (수정 없음)
   - Invoice, Statement, Order 엔티티에서
     Invoice.status = PAID 조건 단독으로 중복 집계 없이 집계 가능한지 확인
   - 조인 경로(Invoice → Order → Employee/Client, Invoice → Product 등) 파악
   - 중복 집계 위험이 있으면 즉시 멈추고 보고

2. 공통 DTO / Enum 작성
   - StatisticsFilter.java (요청 파라미터 + 검증 로직)
   - StatisticsPeriod.java (enum: MONTHLY, QUARTERLY)
   - StatisticsRankingType.java (enum: EMPLOYEE, CLIENT, VARIETY)
   - SalesTrendDto.java (시계열 응답: targetId, targetName, data[])
   - SalesTrendItemDto.java (period, sales)
   - SalesRankingDto.java (rank, targetId, targetName, sales)

3. StatisticsRepository 작성
   - QueryDSL 집계 메서드 구현
   - 빈 구간(0) 채우기 로직은 서비스 레이어에서 처리 (리포지토리는 순수 집계만)
   - SALES_REP 담당 범위 필터는 서비스에서 QueryDSL 조건으로 주입
     (리포지토리에 역할 판단 로직 넣지 않음)

4. StatisticsQueryService 작성
   - 필터 검증:
     - from / to 필수
     - from <= to
     - period 필수
     - 축별 선택 ID/code 최소 1개 필수 검증
     - limit 기본값 10, 최대 50 보정
   - 빈 구간 0 채우기:
     - from~to 범위의 전체 period 목록 생성
     - 리포지토리 결과와 병합, 누락된 period는 sales = 0으로 채움
   - SALES_REP 담당 범위 자동 필터링:
     - principal에서 employeeId 추출
     - 해당 사원의 담당 clientId 목록을 조회해 QueryDSL 조건으로 주입

5. StatisticsController 작성
   - 6개 엔드포인트 구현
   - principal → 서비스 전달
   - 파라미터 바인딩 및 ApiResult.success(...) 래핑

6. SecurityConfig 보안 매처 추가
   - 기존 /api/v1/statistics/billing/revenue/** 매처 유지
   - 신규 경로별 역할 매핑 추가 (위 컨트롤러 표 참조)
   - TestSecurityConfig에도 동일하게 반영

7. 테스트 작성
   - StatisticsControllerTest (WebMvcTest + TestSecurityConfig):
     - 경로별 정상 호출 200
     - 비허용 역할 403, 비인증 401
     - 필수 파라미터 누락 400
   - StatisticsQueryServiceTest:
     - from/to 역전, 선택 필터 빈 요청 예외 검증
     - 빈 구간 0 채우기 검증 (MONTHLY / QUARTERLY)
     - SALES_REP 담당 범위 필터 적용 검증
   - StatisticsRepositoryTest:
     - 집계 정확도 (PAID 기준 합산)
     - 월/분기 버킷 생성 정확도
     - 랭킹 limit / rank 순서
     - 축별 선택 필터(employeeIds / clientIds / varietyCodes) 적용 검증
```

---

## 집계 처리 공통 규칙

1. 매출 기준: `Invoice.status = PAID` 단일 조건
2. period 버킷 생성:
   - MONTHLY: `DATE_FORMAT(invoice.paidAt, '%Y-%m')`
   - QUARTERLY: `CONCAT(YEAR(invoice.paidAt), '-Q', QUARTER(invoice.paidAt))`
3. 빈 구간 채우기: 서비스에서 `from`~`to` 범위의 전체 period 목록을 생성 후
   리포지토리 결과와 병합. 누락된 period는 `sales = 0`으로 채움
4. 중복 집계 방지: invoice 단위를 집계 기준으로 고정.
   필요 시 `invoice + 축(employee/client/variety)` 단위 group 후 Java 재집계
5. SALES_REP 담당 범위 필터: 서비스에서 principal의 `employeeId`를 추출해
   해당 사원의 담당 client/order 범위로 QueryDSL 조건 주입.
   리포지토리는 주입된 조건만 실행, 역할 판단 금지

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

- Step 1에서 `Invoice.status = PAID` 단독 조건으로 중복 집계 위험이 발견되는 경우
- SALES_REP principal에서 담당 범위(clientId/employeeId)를 추출할 수 없는 케이스 발견
- `StatisticsQueryService` 외 다른 서비스에 집계 로직을 넣어야 하는 케이스 발견
- 확정된 정책 결정 사항(상단 표)과 충돌하는 요구사항 발견
- 컴파일 오류 발생
- 테스트 실패

---

## 범위 외 (이 세션에서 하지 말 것)

- 기존 `billing statistics` (`/api/v1/statistics/billing/revenue/**`) 수정 또는 삭제
- 드롭다운 전용 alias endpoint 신규 추가
- 프론트엔드 파일 수정
- 기존 테스트 코드 삭제
- `AGENTS.md` 자체 수정
