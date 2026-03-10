# Billing Statistics Backend 정리

## 1. 기능 개요
`billing` 통계 기능은 청구(Invoice) 기반 매출을 조회하는 읽기 전용 통계 API다.
기간(`from`, `to`)과 선택 품종(`category`)을 입력으로 받아 다음 3가지 뷰를 제공한다.

- 월별 청구 매출: `GET /api/v1/statistics/billing/revenue/monthly`
- 품종별 청구 매출: `GET /api/v1/statistics/billing/revenue/by-category`
- 월별/품종별 청구 매출: `GET /api/v1/statistics/billing/revenue/monthly-by-category`

## 2. 프론트 연동용 API 명세

### 2.1 공통 요청 규칙
- Base Path: `/api/v1/statistics/billing/revenue`
- Method: 모두 `GET`
- 인증: 로그인 사용자 토큰 필수 (`@AuthenticationPrincipal CustomUserDetails`)
- Query
  - `from` (필수, `yyyy-MM-dd`)
  - `to` (필수, `yyyy-MM-dd`)
  - `category` (선택, 정확 일치 문자열)

요청 예시:
```http
GET /api/v1/statistics/billing/revenue/monthly?from=2026-01-01&to=2026-12-31&category=수박
Authorization: Bearer {accessToken}
```

### 2.2 응답 래퍼 형식
모든 응답은 `ApiResult<T>` 형태를 따른다.

- 성공 시
```json
{
  "result": "SUCCESS",
  "data": [ ... ],
  "error": null
}
```

- 실패 시
```json
{
  "result": "ERROR",
  "data": null,
  "error": {
    "code": "...",
    "message": "..."
  }
}
```

### 2.3 엔드포인트별 `data` 스키마

1. `GET /monthly`
```json
[
  { "month": "2026-01", "billedRevenue": 1250000 },
  { "month": "2026-02", "billedRevenue": 980000 }
]
```

2. `GET /by-category`
```json
[
  { "category": "수박", "billedRevenue": 3120000 },
  { "category": "참외", "billedRevenue": 1980000 }
]
```

3. `GET /monthly-by-category`
```json
[
  { "month": "2026-01", "category": "수박", "billedRevenue": 800000 },
  { "month": "2026-01", "category": "참외", "billedRevenue": 450000 }
]
```

### 2.4 액터별 접근 권한/조회 범위
| 액터(Role) | API 접근 | 조회 범위 | 비고 |
|---|---|---|---|
| `ADMIN` | 허용 | 전체 데이터 | `employeeId = null`로 조회 |
| `SALES_REP` | 허용 | 본인 담당 데이터만 | `employeeId = principal.employeeId` 적용 |
| `CLIENT` | 거부 | 없음 | 서비스에서 `ACCESS_DENIED` 예외 |
| 인증 없음 | 거부 | 없음 | `UNAUTHORIZED` 예외 |

프론트 처리 권장:
- `CLIENT` 계정에서는 통계 화면 진입을 막거나 권한 안내 문구 노출
- `ACCESS_DENIED`, `UNAUTHORIZED` 수신 시 공통 에러 핸들러에서 라우팅 처리

## 3. 계층별 역할

### Controller
`BillingRevenueStatisticsController`
- `from`, `to`, `category`를 요청 파라미터로 수신
- `BillingRevenueStatisticsFilter`로 변환 후 서비스에 전달
- 인증 사용자(`CustomUserDetails`)를 함께 전달
- 응답은 `ApiResult.success(...)`로 래핑

### Service
`BillingRevenueStatisticsQueryService`
- 읽기 전용 트랜잭션(`@Transactional(readOnly = true)`)에서 동작
- 공통 사전 검증 수행
- 사용자 역할에 따라 조회 범위(`employeeId`)를 제한
- 리포지토리 조회 메서드 호출

핵심 검증 규칙:
- `fromDate`, `toDate` 필수
- `fromDate <= toDate`
- 조회 최대 기간 24개월(포함)

권한/스코프 규칙:
- `ADMIN`: 전체 조회(`employeeId = null`)
- `SALES_REP`: 본인 직원 ID 기준 조회
- 기타 역할: 접근 거부(`ACCESS_DENIED`)

### Repository
`BillingRevenueStatisticsRepository`
- QueryDSL 기반 다중 조인으로 통계 조회 수행
- 집계 기준 데이터:
  - `Invoice.status IN (PUBLISHED, PAID)`
  - `Invoice.invoiceDate BETWEEN fromDate AND toDate`
  - `InvoiceStatement.included = true`
  - `Statement.status = ISSUED`
  - (필요 시) `productCategory = category`
  - (SALES_REP) `orderHeader.employee.id = employeeId`

## 4. 집계 구현 포인트

### 4.1 월 키 생성
- `DATE_FORMAT(invoiceDate, '%Y-%m')`로 월 문자열(`yyyy-MM`) 생성
- MySQL 및 H2(MySQL mode) 호환 기준

### 4.2 금액 계산식
- 라인 금액: `coalesce(unitPrice, 0) * coalesce(quantity, 0)`
- 집계 시 `sum(...).coalesce(0)` 적용

### 4.3 중복 집계 방지 전략
조인으로 인해 동일 청구서가 중복 합산될 수 있어, 1차 그룹핑 후 애플리케이션 레벨 재집계를 사용한다.

- 월별: `groupBy(month, invoice.id)` 후 month 단위 재합산
- 품종별: `groupBy(category, invoice.id)` 후 category 단위 재합산
- 월별/품종별: `groupBy(month, category, invoice.id)` 후 (month, category) 단위 재합산

## 5. DTO/입출력 모델

### 요청 필터
`BillingRevenueStatisticsFilter`
- `fromDate: LocalDate` (필수)
- `toDate: LocalDate` (필수)
- `category: String` (선택)

### 응답 DTO
- `MonthlyBilledRevenueDto(month, billedRevenue)`
- `CategoryBilledRevenueDto(category, billedRevenue)`
- `MonthlyCategoryBilledRevenueDto(month, category, billedRevenue)`

## 6. 정렬 정책
- 월별 계열 응답: `month` 오름차순
- 품종별 응답: `billedRevenue` 내림차순

## 7. 예외 처리 포인트
서비스 계층에서 `CoreException`으로 예외를 명시적으로 처리한다.

- 입력 오류: `INVALID_INPUT_VALUE`
- 인증 정보 없음: `UNAUTHORIZED`
- 영업 담당자-직원 매핑 없음: `EMPLOYEE_NOT_LINKED`
- 권한 없음: `ACCESS_DENIED`
