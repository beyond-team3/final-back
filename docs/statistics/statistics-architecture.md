## [2026-03-06] 청구 매출 통계 조회 주체 스코프 전달

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsController.java`
- 클래스/메서드: `BillingRevenueStatisticsController#getMonthlyRevenue`, `getCategoryRevenue`, `getMonthlyCategoryRevenue`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryService.java`
- 클래스/메서드: `BillingRevenueStatisticsQueryService#getMonthlyRevenue`, `getCategoryRevenue`, `getMonthlyCategoryRevenue`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java`
- 클래스/메서드: `BillingRevenueStatisticsRepository#findMonthlyRevenue`, `findCategoryRevenue`, `findMonthlyCategoryRevenue`

### 변경 내용
통계 컨트롤러가 `@AuthenticationPrincipal CustomUserDetails`를 받아 조회 주체를 서비스로 전달하도록 변경했다.
서비스는 역할에 따라 조회 스코프를 해석해 `ADMIN`은 전체 조회, `SALES_REP`는 본인 `employeeId` 범위 조회만 허용한다.
리포지토리 메서드는 스코프용 `employeeId`를 추가로 받아 기존 공통 집계 조건에 담당 영업사원 조건만 선택적으로 결합한다.

### 변경 이유
권한 정책상 `SALES_REP`의 청구 매출 통계는 본인 담당 범위로 제한되어야 하기 때문이다.
