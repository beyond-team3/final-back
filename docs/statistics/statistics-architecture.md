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

## [2026-03-06] 통계 API base path 변경

### 변경 대상
- 파일: `BillingRevenueStatisticsController.java`, `SecurityConfig.java`
- 클래스/메서드: `@RequestMapping`, `requestMatchers`

### 변경 내용
통계 API base path를 `/statistics/billing/revenue`에서
`/api/v1/statistics/billing/revenue`로 변경.
프로젝트 전체 API prefix `/api/v1` 통일 정책에 맞춤.
SecurityConfig의 보안 매처도 동일하게 변경.

### 변경 이유
프로젝트 전체 API prefix 통일 정책

## [2026-03-06] WebMvcTest 슬라이스 보안 설정 분리

### 변경 대상
- 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java`
- 클래스/메서드: `TestSecurityConfig#filterChain`
- 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsControllerTest.java`
- 클래스/메서드: `BillingRevenueStatisticsControllerTest`

### 변경 내용
`SecurityConfig`의 JPA Auditing 의존으로 인해 `@WebMvcTest` 슬라이스에서
컨텍스트 로딩 실패가 발생했다. 테스트 전용 `TestSecurityConfig`를 `src/test/` 하위에
신규 작성하고, 컨트롤러 테스트의 `@Import` 대상을 교체했다.
이후 `@WebMvcTest` 기반 컨트롤러 테스트는 이 테스트 보안 구성을 표준으로 재사용할 수 있다.

### 변경 이유
WebMvcTest 슬라이스에서 JPA 컨텍스트 분리 필요

## [2026-03-06] JPA Auditing 설정 분리

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/config/JpaAuditingConfig.java`
- 클래스/메서드: `JpaAuditingConfig`
- 파일: `src/main/java/com/monsoon/seedflowplus/MonSoonApplication.java`
- 클래스/메서드: `MonSoonApplication`

### 변경 내용
`@EnableJpaAuditing`을 메인 클래스에서 분리하여 `JpaAuditingConfig`로 이동했다.
`@WebMvcTest` 슬라이스 컨텍스트에서 JPA Auditing 빈이 유입되던 문제를 해소하도록
애플리케이션 부팅 설정과 JPA 설정의 경계를 분리했다.

### 변경 이유
WebMvcTest 슬라이스 격리 요건
