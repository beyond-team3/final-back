## [2026-03-06 10:33] 월별 청구 매출 category 필터 반영

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — 월별 청구 매출 조회에서도 category 파라미터가 존재할 때만 `EXISTS` 서브쿼리에 품종 조건이 적용되도록 수정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 10:41] 서비스 검증 정책 테스트 고정

### 작업 내용
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryServiceTest.java` — 24개월 허용/초과, 역전 날짜, fromDate/toDate null 검증 케이스를 추가해 `BillingRevenueStatisticsQueryService` 검증 정책을 고정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 11:08] 월별 집계 category 필터 리포지토리 테스트 추가

### 작업 내용
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepositoryTest.java` — `invoice -> statement -> orderDetail -> contractDetail.productCategory` 연결 fixture를 구성해 월별 집계의 category 필터 및 공통 필터 유지 여부를 검증하는 `@DataJpaTest` 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/H2DateFormatFunctions.java` — H2 테스트 환경에서 `DATE_FORMAT` QueryDSL 표현식을 실행할 수 있도록 함수 alias용 헬퍼 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 11:36] 월별 category 과대 집계 회귀 수정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — 월별 매출 조회에서 category 지정 시 `invoice.totalAmount` 전액 합산을 제거하고, 매칭된 `contractDetail.amount`만 `invoice + month` 단위로 집계한 뒤 Java에서 월별 재집계하도록 수정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepositoryTest.java` — 혼합 품종 invoice fixture와 회귀 테스트를 추가하고 기존 월별 기대값을 실제 fixture 기준으로 보정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 11:49] 청구 매출 통계 품종 집계 및 기간 경계 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — 품종별/월별+품종별 집계와 월별 category 분기에서 `invoice.totalAmount` 및 `contractDetail.amount` 대신 `unitPrice * quantity` 기준 품목 금액을 `invoice + category` 또는 `invoice + month + category` 단위로 합산하도록 수정
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryService.java` — 조회 기간 검증을 포함 구간 기준 월 수로 계산해 정책상 최대 24개월까지만 허용하도록 보정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsControllerTest.java` — `by-category`, `monthly-by-category` 엔드포인트의 응답 래퍼 및 필터 바인딩 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryServiceTest.java` — 24개월/25개월 경계값 테스트 기대값을 정책 기준으로 수정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepositoryTest.java` — 혼합 품종 invoice의 품종별/월별+품종별 중복 집계 회귀 테스트와 실제 청구 산식 기반 fixture 보정 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 12:02] 통계 API 접근 제어 및 월별 총매출 기준 정합성 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java` — `/statistics/billing/revenue/**` 경로를 `SALES_REP`, `ADMIN` 역할로 제한해 다른 인증 사용자의 통계 조회를 차단
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — category 미지정 월별 집계도 `included=true`, `statement.status=ISSUED`인 statement 라인 금액을 `invoice + month` 단위로 합산한 뒤 Java에서 재집계하도록 보정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepositoryTest.java` — invoice 총액과 statement 라인 합계가 어긋나는 fixture를 추가해 월별 총매출이 품종 집계와 같은 기준을 사용하는지 검증

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 12:22] 청구 매출 통계 SALES_REP 조회 범위 제한

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsController.java` — 인증 사용자 principal을 받아 통계 조회 서비스로 전달하도록 수정
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryService.java` — 역할별 조회 스코프를 해석해 `ADMIN`은 전체 조회, `SALES_REP`는 `employeeId` 기준 조회만 허용하도록 수정
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — 기존 공통 집계 조건에 선택적 `employeeId` 필터를 추가해 담당 영업사원 범위만 조회하도록 수정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsControllerTest.java` — principal 전달 및 기존 바인딩 검증을 서비스 시그니처에 맞게 보강
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryServiceTest.java` — `ADMIN`/`SALES_REP`/비허용 역할/직원 미연결 케이스를 검증하는 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepositoryTest.java` — `employeeId` 스코프가 월별/품종별/월별+품종별 집계에 공통 적용되는지 검증 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 통계 조회 주체 전달 구조 변경을 기록하기 위해 문서 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
