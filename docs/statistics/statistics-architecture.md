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

## [2026-03-06] 청구 매출 통계 컨트롤러 패키지명 정리

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/controller/BillingRevenueStatisticsController.java`
- 클래스/메서드: `BillingRevenueStatisticsController`
- 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/controller/BillingRevenueStatisticsControllerTest.java`
- 클래스/메서드: `BillingRevenueStatisticsControllerTest`

### 변경 내용
청구 매출 통계 웹 계층 패키지를 `api`에서 `controller`로 이동해
프로젝트 내 Spring MVC 명명 규칙과 일치시켰다.
패키지 이동에 맞춰 컨트롤러와 `@WebMvcTest` 대상 테스트의 선언 경로를 함께 정리했다.

### 변경 이유
웹 어댑터 패키지 명명 규칙 일관성 유지

## [2026-03-11] 승인 후속 상태 동기화 서비스 경로 분리

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java`
- 클래스/메서드: `ApprovalCommandService#syncUpstreamDocumentsAfterContractDecision`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java`
- 클래스/메서드: `QuotationService#completeAfterContractApproval`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java`
- 클래스/메서드: `QuotationRequestService#completeAfterContractApproval`

### 변경 내용
계약 최종 승인 시 상위 문서 상태를 직접 `updateStatus(...)`로 바꾸던 경로를
견적서/견적요청서 전용 완료 메서드 호출로 분리했다.
새 서비스 메서드는 상태 전이 검증과 DealLog 기록을 함께 수행해 recentLogs 생성 경로를 표준화한다.

### 변경 이유
후속 문서 상태 변경에도 전이 검증과 감사 로그를 일관되게 적용하기 위함

## [2026-03-11] 공통 통계 API 신규 구조 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/controller/StatisticsController.java`
- 클래스/메서드: `StatisticsController`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/service/StatisticsQueryService.java`
- 클래스/메서드: `StatisticsQueryService#getMySalesTrend`, `getAdminSalesTrend`, `getSalesTrendByEmployee`, `getSalesTrendByClient`, `getSalesTrendByVariety`, `getRanking`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/repository/StatisticsRepository.java`
- 클래스/메서드: `StatisticsRepository`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/*.java`
- 클래스/메서드: `StatisticsFilter`, `StatisticsPeriod`, `StatisticsRankingType`, `SalesTrendDto`, `SalesTrendItemDto`, `SalesRankingDto`
- 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java`
- 클래스/메서드: `SecurityConfig#filterChain`
- 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java`
- 클래스/메서드: `TestSecurityConfig#filterChain`

### 변경 내용
기존 billing 전용 통계와 별도로 `/api/v1/statistics/**` 공통 통계 API 계층을 신규 추가했다.
컨트롤러는 6개 엔드포인트를 제공하고, 서비스는 기간 검증·0 채우기·영업사원 담당 거래처 범위 주입을 담당하며,
리포지토리는 QueryDSL로 사원/거래처/품종/랭킹 집계를 수행하도록 역할을 분리했다.

### 변경 이유
정책 결정 사항의 공통 통계 API 구조와 역할별 조회 범위를 반영하기 위함

## [2026-03-11] 승인 검색 정렬 alias 정규화

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/controller/ApprovalController.java`
- 클래스/메서드: `ApprovalController#search`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java`
- 클래스/메서드: `ApprovalCommandService#search`, `normalizeApprovalSearchPageable`

### 변경 내용
승인 검색 API의 기본 페이지 정렬을 `id desc`로 고정했다.
서비스는 프론트가 보내는 `approvalId` 정렬 요청을 엔티티 필드명인 `id`로 정규화한 뒤 저장소 검색에 전달한다.
이로써 Spring Data가 존재하지 않는 `ar.approvalId` 정렬 절을 JPQL에 추가하지 않도록 보정했다.

### 변경 이유
승인 요청 엔티티 식별자 필드와 프론트 정렬 파라미터 이름이 달라 조회가 실패했기 때문이다.

## [2026-03-12] 문서 생성 승인 자동화 구조 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionService.java`
- 클래스/메서드: `ApprovalSubmissionService#createApprovalRequest`, `submitFromDocumentCreation`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java`
- 클래스/메서드: `QuotationService#createQuotation`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java`
- 클래스/메서드: `ContractService#createContract`
- 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java`
- 클래스/메서드: `SecurityConfig#filterChain`
- 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java`
- 클래스/메서드: `TestSecurityConfig#filterChain`

### 변경 내용
문서 생성 직후 승인 요청을 자동 생성하는 전용 서비스 `ApprovalSubmissionService`를 추가했다.
견적서/계약서 생성 서비스는 문서 저장과 딜 로그 기록 후 이 서비스를 호출해 승인 요청을 생성하며,
수동 `POST /api/v1/approvals` 경로는 ADMIN만 접근하도록 보안 매처를 강화했다.

### 변경 이유
견적서·계약서 생성 이후 프론트의 별도 승인 요청 호출 없이 관리자 승인 흐름이 이어지도록 승인 생성 경로를 백엔드로 일원화하기 위함

## [2026-03-12] 문서 삭제 시 승인 요청 취소 처리 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCancellationService.java`
- 클래스/메서드: `ApprovalCancellationService#cancelPendingRequest`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/repository/ApprovalRequestRepository.java`
- 클래스/메서드: `ApprovalRequestRepository#findByDealTypeAndTargetIdAndStatus`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java`
- 클래스/메서드: `QuotationService#deleteQuotation`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java`
- 클래스/메서드: `ContractService#deleteContract`

### 변경 내용
문서 삭제 시 연결된 `PENDING` 승인 요청을 찾아 `CANCELED`로 종료하는 전용 서비스를 추가했다.
견적서/계약서 삭제 로직은 문서 상태를 삭제 처리한 직후 이 서비스를 호출해 더 이상 유효하지 않은 승인 요청이 관리자 목록에 남지 않도록 보정했다.

### 변경 이유
문서가 삭제된 뒤에도 진행 중 승인 요청이 남아 관리자 승인 목록과 실제 문서 상태가 어긋나는 문제를 방지하기 위함

## [2026-03-12] 주문 승인 단계 정책 및 주문 생성 연계 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java`
- 클래스/메서드: `OrderService#createOrder`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalFlowPolicy.java`
- 클래스/메서드: `ApprovalFlowPolicy#createSteps`, `isLastStep`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionService.java`
- 클래스/메서드: `submitFromDocumentCreation`, `publishApprovalRequestedForFirstApprovers`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java`
- 클래스/메서드: `createApprovalRequest`, `decideStep`, `updateRequestStatus`

### 변경 내용
주문 생성 서비스 시그니처를 principal 포함 형태로 단일화하고, 주문 저장 및 CREATE 로그 기록 직후 자동 승인 요청을 생성하도록 연결했다.
승인 단계 정의를 `ApprovalFlowPolicy`로 분리해 QUO/CNT는 `ADMIN -> CLIENT`, ORD는 `SALES_REP` 단일 단계로 공통 생성한다.
승인 명령 서비스는 마지막 단계 판정, ORD 승인 시 `OrderService.confirmOrder(...)` 재사용, ORD 전용 알림 대상 계산을 공통 흐름 안에서 처리하도록 일반화했다.

### 변경 이유
주문 문서에도 기존 승인 모듈을 재사용해 자동 승인 요청과 주문 확정/명세서 발급 흐름을 일관되게 연결하기 위함

## [2026-03-13] deal lifecycle 삭제/만료 종결 규칙 반영

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java`
- 클래스/메서드: `QuotationRequestService#createQuotationRequest`, `QuotationRequestService#deleteQuotationRequest`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java`
- 클래스/메서드: `QuotationService#deleteQuotation`, `QuotationService#syncQuotationStatuses`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java`
- 클래스/메서드: `ContractService#deleteContract`, `ContractService#syncContractStatuses`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java`
- 클래스/메서드: `OrderService#cancelOrder`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/core/entity/SalesDeal.java`
- 클래스/메서드: `SalesDeal#close`

### 변경 내용
RFQ 생성은 기존 open deal 재사용을 중단하고 항상 새 deal bootstrap을 생성하도록 바꿨다.
RFQ/QUO/CNT 삭제는 문서 상태 변경 후 삭제/cancel deal log를 직접 기록하고, RFQ 삭제와 QUO/CNT 만료 시에는 서비스 내부 helper를 통해 deal 종료를 공통 처리하도록 정리했다.
ORD 취소는 진행 중 approval 정리를 추가했고, `SalesDeal.close()`는 중복 호출 시 최초 `closedAt`을 유지하는 idempotent 동작으로 보강했다.

### 변경 이유
2026-03-13 deal lifecycle 정책상 RFQ만 deal 생성/삭제로 직접 종결하고, QUO/CNT 만료를 비삭제 종결 이벤트로 취급해야 하기 때문

## [2026-03-13] deal snapshot 복구 및 만료 타임라인 보강

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java`
- 클래스/메서드: `QuotationRequestService#deleteQuotationRequest`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java`
- 클래스/메서드: `QuotationService#deleteQuotation`, `QuotationService#syncQuotationStatuses`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java`
- 클래스/메서드: `ContractService#deleteContract`, `ContractService#syncContractStatuses`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationService.java`
- 클래스/메서드: `ScheduledNotificationService#scheduleContractLifecycleNotifications`

### 변경 내용
RFQ/QUO/CNT 삭제 후 deal snapshot이 실제 현재 문서(RFQ 복구, QUO 복구, 최종 삭제 문서)를 가리키도록 서비스 helper에서 `SalesDeal.updateSnapshot(...)`을 명시적으로 동기화한다.
QUO/CNT 만료 배치에는 `EXPIRE` deal log와 snapshot 갱신을 추가해 타임라인과 deal 목록이 모두 만료 상태를 반영하도록 보강했다.
계약 종료 30일 전 알림은 예약 시각이 현재보다 과거면 생성하지 않도록 방어 로직을 추가했다.

### 변경 이유
deal 조회와 타임라인이 실제 영업 진행 상태와 어긋나면 삭제/만료 이후 추적성이 깨지므로 snapshot과 만료 이벤트를 함께 보정해야 하기 때문

## [2026-03-13] 주문 승인 후 내부 이벤트 확정 및 알림 수신자 보강

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java`
- 클래스/메서드: `ApprovalCommandService#applyOrderDecision`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/OrderApprovalConfirmedEvent.java`
- 클래스/메서드: `OrderApprovalConfirmedEvent`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderApprovalConfirmedEventHandler.java`
- 클래스/메서드: `OrderApprovalConfirmedEventHandler#handle`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java`
- 클래스/메서드: `OrderService#confirmOrderFromApproval`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java`
- 클래스/메서드: `StatementService#resolveStatementRecipientUserIds`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationService.java`
- 클래스/메서드: `ScheduledNotificationService#scheduleContractLifecycleNotifications`

### 변경 내용
ORD 승인 완료 시 즉시 주문 확정을 호출하던 구조를 제거하고, approval 트랜잭션 커밋 후 `OrderApprovalConfirmedEvent`를 받아 내부 전용 확정 메서드를 실행하도록 분리했다.
외부 주문 확정 API는 제거해 승인 완료 경로만 주문 확정을 유발하게 했고, statement 발행 알림은 주문 담당자와 deal owner를 각각 독립적으로 수신자에 포함하도록 보강했다.
계약 시작/종료 알림도 종료 30일 전과 동일하게 과거 예약 시각을 건너뛰도록 통일했다.

### 변경 이유
주문 확정이 승인 완료의 후속 이벤트로만 실행되어야 approval 우회가 막히고, 계약/명세서 알림도 실제 수신 대상과 현재 시각 기준으로 정확히 처리되기 때문
