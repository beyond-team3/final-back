# SeedFlow+ Common Statistics API

## 개요

신규 통계 기능은 기존 billing statistics와 분리된 공통 통계 API로 제공된다.
기준 경로는 `/api/v1/statistics/**` 이며, 매출 집계 기준은 `Invoice.status = PAID` 단일 조건이다.

응답은 모두 `ApiResult.success(...)` 래핑 구조를 사용한다.

---

## 집계 기준

### 매출 기준
- `Invoice.status = PAID`
- 집계 금액: `Invoice.totalAmount`
- 기간 기준 컬럼: `Invoice.invoiceDate`

### period 포맷
- `MONTHLY`: `YYYY-MM`
- `QUARTERLY`: `YYYY-QN`

### 빈 구간 처리
- 조회 범위 내 매출이 없는 월/분기도 응답에 포함된다.
- 누락 구간은 `sales = 0`으로 채워진다.

### SALES_REP 범위 제한
- `by-client`, `by-variety`, `ranking` 조회 시 `SALES_REP`는 본인 담당 거래처 범위로 자동 제한된다.
- 담당 거래처는 `Client.managerEmployee` 기준으로 판별된다.

---

## 사용 엔티티

### 공통 추이 / 랭킹
- `Invoice`
- `Client`
- `Employee`

### 품종 추이 / 품종 랭킹
- `Invoice`
- `InvoiceStatement`
- `Statement`
- `OrderDetail`
- `ContractDetail`

품종 집계는 아래 경로를 사용한다.

`Invoice -> InvoiceStatement -> Statement -> OrderDetail -> ContractDetail.productCategory`

---

## 권한 정책

| API | SALES_REP | ADMIN |
|---|---|---|
| `/sales-rep` | 가능 | 가능 |
| `/admin` | 불가 | 가능 |
| `/by-employee` | 불가 | 가능 |
| `/by-client` | 가능 | 가능 |
| `/by-variety` | 가능 | 가능 |
| `/ranking` | 가능 | 가능 |

---

## 공통 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | ISO Date | Y | 조회 시작일 |
| `to` | ISO Date | Y | 조회 종료일 |
| `period` | Enum | Y | `MONTHLY`, `QUARTERLY` |

추가 정책:
- `from <= to` 여야 한다.
- `employeeIds`, `clientIds`, `varietyCodes`는 해당 API에서 최소 1개 이상 필요하다.
- `limit`은 기본값 10, 최대 50으로 보정된다.

---

## 응답 모델

### 시계열 응답

```json
[
  {
    "targetId": "1",
    "targetName": "홍길동",
    "data": [
      { "period": "2026-01", "sales": 0 },
      { "period": "2026-02", "sales": 1500000 }
    ]
  }
]
```

### 랭킹 응답

```json
[
  { "rank": 1, "targetId": "10", "targetName": "거래처A", "sales": 5000000 },
  { "rank": 2, "targetId": "11", "targetName": "거래처B", "sales": 3200000 }
]
```

---

## API 상세

### 1. 본인 매출 추이

- Method: `GET`
- Path: `/api/v1/statistics/sales-rep`
- Roles: `SALES_REP`, `ADMIN`

#### Query
- `from`
- `to`
- `period`

#### 설명
- `SALES_REP`는 본인 1명의 매출 추이를 조회한다.
- `ADMIN`도 동일 엔드포인트 호출은 가능하지만, principal의 employeeId 기준으로 처리된다.

---

### 2. 전체 매출 추이

- Method: `GET`
- Path: `/api/v1/statistics/admin`
- Roles: `ADMIN`

#### Query
- `from`
- `to`
- `period`

#### 설명
- 전체 사원 합산 매출 추이를 조회한다.
- 단일 시리즈로 반환된다.

---

### 3. 사원별 매출 추이

- Method: `GET`
- Path: `/api/v1/statistics/by-employee`
- Roles: `ADMIN`

#### Query
- `from`
- `to`
- `period`
- `employeeIds`

#### 예시

```text
/api/v1/statistics/by-employee?from=2026-01-01&to=2026-03-31&period=MONTHLY&employeeIds=1&employeeIds=2
```

#### 설명
- 선택한 사원별 시계열 데이터를 반환한다.

---

### 4. 거래처별 매출 추이

- Method: `GET`
- Path: `/api/v1/statistics/by-client`
- Roles: `SALES_REP`, `ADMIN`

#### Query
- `from`
- `to`
- `period`
- `clientIds`

#### 예시

```text
/api/v1/statistics/by-client?from=2026-01-01&to=2026-03-31&period=MONTHLY&clientIds=10&clientIds=11
```

#### 설명
- 선택한 거래처별 시계열 데이터를 반환한다.
- `SALES_REP`는 본인 담당 거래처만 조회 가능하다.

---

### 5. 품종별 매출 추이

- Method: `GET`
- Path: `/api/v1/statistics/by-variety`
- Roles: `SALES_REP`, `ADMIN`

#### Query
- `from`
- `to`
- `period`
- `varietyCodes`

#### 예시

```text
/api/v1/statistics/by-variety?from=2026-01-01&to=2026-03-31&period=MONTHLY&varietyCodes=TOMATO&varietyCodes=CABBAGE
```

#### 설명
- 선택한 품종별 시계열 데이터를 반환한다.
- `targetId`는 품종 식별자 문자열이다.

---

### 6. 랭킹 조회

- Method: `GET`
- Path: `/api/v1/statistics/ranking`
- Roles: `SALES_REP`, `ADMIN`

#### Query
- `from`
- `to`
- `period`
- `type`: `EMPLOYEE`, `CLIENT`, `VARIETY`
- `limit`: optional
- `employeeIds`: `type=EMPLOYEE`일 때 필수
- `clientIds`: `type=CLIENT`일 때 필수
- `varietyCodes`: `type=VARIETY`일 때 필수

#### 예시

```text
/api/v1/statistics/ranking?from=2026-01-01&to=2026-03-31&period=MONTHLY&type=CLIENT&clientIds=10&clientIds=11&limit=5
```

#### 설명
- 축별 총매출 랭킹을 반환한다.
- `rank`는 서비스 레이어에서 1부터 순차 부여된다.

---

## 프론트 연동 시 참고

- 차트 라벨은 `data[].period`를 그대로 사용하면 된다.
- 차트 값은 `data[].sales`를 사용한다.
- 금액 포맷은 프론트에서 처리한다.
- 드롭다운 API는 기존 사원/거래처/품종 조회 API를 재사용한다.
- 별도 alias endpoint는 없다.

---

## 구현 클래스

- Controller: `StatisticsController`
- Service: `StatisticsQueryService`
- Repository: `StatisticsRepository`
- DTO:
  - `StatisticsFilter`
  - `StatisticsPeriod`
  - `StatisticsRankingType`
  - `SalesTrendDto`
  - `SalesTrendItemDto`
  - `SalesRankingDto`
