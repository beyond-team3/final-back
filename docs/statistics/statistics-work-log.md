## [2026-03-06 10:33] 월별 청구 매출 category 필터 반영

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — 월별 청구 매출 조회에서도 category 파라미터가 존재할 때만 `EXISTS` 서브쿼리에 품종 조건이 적용되도록 수정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-13 14:28] 주문 승인 내부 이벤트 확정과 알림 보강

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` — ORD 승인 시 직접 주문 확정 대신 after-commit 이벤트 발행으로 전환
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/OrderApprovalConfirmedEvent.java` — 주문 승인 완료 내부 이벤트 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderApprovalConfirmedEventHandler.java` — 승인 완료 이벤트 수신 후 내부 전용 주문 확정 실행
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java` — 외부 confirm API 의존 제거, 내부 전용 `confirmOrderFromApproval` 메서드로 정리
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/controller/OrderController.java` — 외부 주문 확정 엔드포인트 제거
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java` — statement 발행 알림 수신자에 order employee와 deal owner를 모두 포함하고 missing user debug 로그 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationService.java` — 계약 시작/종료 알림 과거 예약 방지 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java` — 미사용 closeDeals 제거 및 close helper 의미 보정
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java` — 미사용 closeDeals 제거 및 close helper 의미 보정
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java` — RFQ 생성 시 단일 timestamp 재사용
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/**` — approval/order/statement/notification/request/quotation 관련 회귀 테스트 및 fixture 안정성 보강
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
없음

## [2026-03-13 15:16] 주문 승인 decision 로그 선반영 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` — ORD 승인 decision 결과가 실제 주문 확정보다 먼저 `CONFIRMED`를 기록하지 않도록 `PENDING/IN_PROGRESS` 유지로 보정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java` — ORD 승인 시 approval decision 로그가 주문 상태를 선반영하지 않는 회귀 테스트 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — approval decision 로그 보정 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
없음

## [2026-03-13 12:24] deal snapshot 복구와 만료 타임라인 보강

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java` — RFQ 삭제 시 deal snapshot을 DELETED RFQ로 동기화한 뒤 close 처리
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java` — QUO 삭제 후 RFQ snapshot 복구, QUO 만료 시 EXPIRE 로그/스냅샷/close 후처리 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java` — CNT 삭제 후 QUO snapshot 복구, CNT 만료 시 EXPIRE 로그/스냅샷/close 후처리 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationService.java` — 계약 종료 30일 전 알림 예약이 과거 시각이면 생성하지 않도록 보정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestServiceTest.java` — RFQ 삭제 snapshot 동기화 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationServiceTest.java` — QUO 삭제 후 RFQ snapshot 복구 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationSyncTest.java` — QUO 만료 EXPIRE 로그/스냅샷 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractServiceTest.java` — CNT 삭제 후 QUO snapshot 복구 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractSyncTest.java` — CNT 만료 EXPIRE 로그/스냅샷 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationServiceTest.java` — short contract의 past ending-soon 예약 skip 검증 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
없음

## [2026-03-12 11:19] 문서 생성 시 승인 요청 자동 생성 적용

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionService.java` — 문서 생성/수동 생성 승인 요청 전용 서비스 추가 및 자동 생성 정책 구현
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java` — 견적서 생성 직후 승인 요청 자동 생성 호출 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java` — 계약서 생성 직후 승인 요청 자동 생성 호출 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java` — 수동 승인 요청 생성 API를 ADMIN 전용으로 제한
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java` — 테스트 보안 설정에 승인 생성 권한 반영
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionServiceTest.java` — 자동 승인 생성, 상태 검증, 중복 차단 테스트 추가
- 수정 파일: `output.txt` — 프론트 반영용 API 변경 프롬프트 작성
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
프론트에서 견적/계약 생성 후 수동 승인 요청 API 호출 제거 및 관리자 승인 목록 재조회 흐름 점검

## [2026-03-12 12:01] 문서 삭제 시 승인 요청 취소 처리 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCancellationService.java` — 삭제된 문서에 연결된 진행 중 승인 요청을 취소하는 서비스 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/repository/ApprovalRequestRepository.java` — 문서와 상태 기준으로 승인 요청을 조회하는 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java` — 견적 삭제 시 진행 중 승인 요청 취소 호출 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java` — 계약 삭제 시 진행 중 승인 요청 취소 호출 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCancellationServiceTest.java` — 승인 취소 서비스 단위 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationServiceTest.java` — 견적 삭제 시 승인 요청 취소 검증 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractServiceTest.java` — 계약 삭제 시 승인 요청 취소 및 상위 문서 복구 검증 테스트 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
수동 승인 생성 API가 동일한 문서 상태 검증 정책을 타도록 승인 생성 경로 일원화 검토

## [2026-03-11 16:34] 공통 통계 API 신규 구현 및 보안 매핑 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/StatisticsFilter.java` — 공통 요청 파라미터 모델과 limit 정규화용 복사 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/StatisticsPeriod.java` — 월/분기 기간 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/StatisticsRankingType.java` — 랭킹 축 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/SalesTrendDto.java` — 시계열 응답 DTO 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/SalesTrendItemDto.java` — period/sales 항목 DTO 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/dto/SalesRankingDto.java` — 랭킹 응답 DTO 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/repository/StatisticsRepository.java` — QueryDSL 기반 추이/랭킹 집계 구현
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/service/StatisticsQueryService.java` — 필터 검증, 빈 구간 0 채우기, SALES_REP 담당 거래처 범위 필터링 구현
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/controller/StatisticsController.java` — `/api/v1/statistics/**` 6개 엔드포인트 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java` — 신규 통계 경로별 권한 매처 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java` — 테스트 보안 경로 매처 동기화
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/controller/StatisticsControllerTest.java` — 경로별 200/401/403/400 컨트롤러 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/service/StatisticsQueryServiceTest.java` — 검증, 0 채우기, SALES_REP 범위 적용 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/repository/StatisticsRepositoryTest.java` — PAID 기준 집계, 버킷, 랭킹 limit, 품종 필터 테스트 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 공통 통계 API 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
커밋

## [2026-03-11 16:43] 공통 통계 API 기능 문서 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/docs/statistics-api-guide.md` — 신규 공통 통계 기능의 집계 기준, 사용 엔티티, 권한, 엔드포인트, 요청/응답 모델을 정리한 문서 추가

### 컴파일 결과
- [ ] 오류 없음
- [ ] 오류 있음 → 문서 추가 작업으로 컴파일 미실행

### 다음 단계
커밋

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

## [2026-03-06 12:46] 통계 API 경로 정책 충돌 해소

### 작업 내용
- 수정 파일: `AGENTS.md` — 통계 API base path를 `/api/v1/statistics/billing/revenue` 기준으로 정정하고 구 경로 유지 금지를 범위 외 항목에 명시
- 수정 파일: `docs/statistics/statistics-work-log.md` — 경로 정책 변경 선행 작업을 별도 로그로 기록

### 컴파일 결과
- [ ] 오류 없음
- [ ] 오류 있음 → 코드 수정 전 정책 정리 단계로 컴파일 미실행

### 다음 단계
`BillingRevenueStatisticsController`와 `SecurityConfig`의 경로를 `/api/v1/statistics/billing/revenue`로 변경하고 컨트롤러 보안 회귀 테스트를 추가

## [2026-03-06 14:01] 컨트롤러 테스트 컨텍스트 복구

### 작업 내용
- 신규: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java` — WebMvcTest 슬라이스 전용 보안 설정 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsControllerTest.java` — `@Import` 대상을 테스트 전용 보안 설정으로 교체하고 `@WithMockUser` 기반 경로별 200/403/401 검증을 유지
- 수정 파일: `docs/statistics/statistics-architecture.md` — WebMvcTest 보안 설정 분리 구조를 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음 (이 작업으로 경로 정책 변경 세션 완료)

## [2026-03-06 14:02] JPA metamodel 오류 원인 진단 및 해결

### 작업 내용
- 진단 결과: 원인 A — `@EnableJpaAuditing`이 `MonSoonApplication`에 직접 선언되어 `@WebMvcTest` 슬라이스에도 유입됨
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/MonSoonApplication.java` — 메인 클래스에서 `@EnableJpaAuditing` 제거
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/config/JpaAuditingConfig.java` — JPA Auditing 전용 설정 클래스 신규 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — JPA Auditing 설정 분리 구조 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음 (경로 정책 변경 세션 완료)

## [2026-03-06 14:06] 통계 API 경로 정책 변경 및 테스트 회귀 정리

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsController.java` — 통계 API base path를 `/api/v1/statistics/billing/revenue`로 변경

- 수정 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java` — 통계 API 보안 매처를 새 base path로 동기화
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/api/BillingRevenueStatisticsControllerTest.java` — 신규 경로 반영, 구 경로 404 검증, 엔드포인트별 200/401/403 보안 회귀 테스트 추가 및 WebMvcTest 슬라이스 설정 정리
- 수정 파일: `build.gradle` — `spring-security-test` 의존성 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 경로 정책 변경 및 테스트 구조 변경 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음 (경로 정책 변경 세션 완료)

## [2026-03-06 14:08] WebMvcTest 테스트 규칙 문서화

### 작업 내용
- 수정 파일: `AGENTS.md` — `@WebMvcTest` 기반 컨트롤러 테스트에서 `TestSecurityConfig`를 사용하고 `JpaAuditingConfig` 분리 전제를 유지하도록 규칙 추가
- 수정 파일: `docs/statistics/statistics-work-log.md` — 테스트 규칙 문서화 작업 기록 추가

### 컴파일 결과
- [ ] 오류 없음
- [ ] 오류 있음 → 문서 수정 작업으로 컴파일 미실행

### 다음 단계
없음

## [2026-03-06 14:25] Git 추적 대상에서 AGENTS 파일 제외

### 작업 내용
- 수정 파일: `.gitignore` — `AGENTS.md`를 Git 추적 제외 대상으로 추가
- 수정 파일: `docs/statistics/statistics-work-log.md` — AGENTS 파일 추적 제외 작업 기록 추가

### 컴파일 결과
- [ ] 오류 없음
- [ ] 오류 있음 → 설정 파일 수정 작업으로 컴파일 미실행

### 다음 단계
`AGENTS.md`를 Git 인덱스에서만 제거하고 커밋

## [2026-03-06 14:41] 청구 매출 통계 컨트롤러 패키지명 변경

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/controller/BillingRevenueStatisticsController.java` — 패키지 선언을 `api`에서 `controller`로 변경하고 컨트롤러 파일 경로를 이동
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/controller/BillingRevenueStatisticsControllerTest.java` — `@WebMvcTest` 대상 테스트를 동일한 `controller` 패키지로 이동하고 선언 경로를 정리
- 수정 파일: `docs/statistics/statistics-architecture.md` — 컨트롤러 패키지 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-11 15:22] 계약 승인 일정 동기화 테스트 mock 대상 정합화

### 작업 내용
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ContractApprovalSchedulesSyncEventHandlerTest.java` — 핸들러 구현이 호출하는 `findByIdWithScheduleRelations(...)`에 맞춰 repository stub 대상을 교체

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-11 15:41] 승인 후속 문서 전이 및 일정 동기화 경로 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` — 계약 승인 후 견적서/RFQ 완료 처리를 전용 서비스 메서드 호출로 변경
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java` — 계약 승인 후 견적서 완료 전이 검증 및 DealLog 기록 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java` — 계약 승인 후 RFQ 완료 전이 검증 및 DealLog 기록 메서드 추가, 생성 로그 민감정보 축소
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/log/policy/DocStatusTransitionPolicy.java` — `WAITING_CONTRACT -> COMPLETED` 전이 규칙 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/log/docs/deal-log-integration-guide.md` — 견적서 완료 전이 가이드 업데이트
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ContractApprovalSchedulesSyncEventHandler.java` — 지정 executor 및 이벤트 전용 새 트랜잭션 경계 적용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncService.java` — 공용 upsert가 호출자 트랜잭션에 참여하도록 REQUIRES_NEW 제거
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java` — 후속 완료 서비스 호출 검증으로 테스트 보강
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-11 16:02] 계약 승인 일정 동기화 예외 전파 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ContractApprovalSchedulesSyncEventHandler.java` — 일정 동기화 중 예외 발생 시 로그 후 예외를 다시 던져 `REQUIRES_NEW` 트랜잭션이 정상적으로 롤백되도록 수정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ContractApprovalSchedulesSyncEventHandlerTest.java` — 일정 동기화 서비스 실패 시 예외가 삼켜지지 않고 호출자에게 전파되는 회귀 테스트 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-11 17:50] 승인 검색 정렬 alias 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/controller/ApprovalController.java` — 승인 검색 기본 정렬을 `id desc`로 지정
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` — `approvalId` 정렬 요청을 엔티티 `id`로 정규화하는 pageable 보정 로직 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java` — 프론트 정렬 alias 회귀 테스트 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 승인 검색 정렬 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-12 17:02] 주문 승인 절차 및 자동 승인 요청 연계 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/controller/OrderController.java` — 주문 생성 컨트롤러가 principal 포함 `createOrder(...)`만 호출하도록 정리
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java` — 주문 생성 principal 검증 및 생성 직후 ORD 승인 요청 자동 제출 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalFlowPolicy.java` — 문서 타입별 승인 단계 정의와 마지막 단계 판정 공통화
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionService.java` — ORD 제출 지원, 주문 상태/소유권 검증, 1단계 SALES_REP 알림 대상 계산 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` — ORD 승인/반려 처리, 주문 확정 서비스 연계, 마지막 단계 일반화, ORD 알림 대상 계산 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java` — ORD 승인 요청 submit 로그가 주문 PENDING 상태를 유지하도록 보정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderServiceTest.java` — 주문 생성 자동 승인 요청 호출 및 null principal 예외 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionServiceTest.java` — ORD 승인 요청 생성/상태/소유권 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java` — ORD 승인/반려/권한/최종 승인 상태 테스트 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-12 19:26] 주문 승인 decision 로그 상태 검증 분리

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java` — ORD 승인 decision은 주문 확정과 명세서 발행으로 deal snapshot이 선행 이동해도 snapshot 재동기화 없이 승인 로그만 기록하도록 분기 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriterTest.java` — ORD 승인 시 deal stage가 `ISSUED`로 바뀐 뒤에도 decision 로그가 예외 없이 기록되는 회귀 테스트 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
프론트 `/approval` 경로에서 ORD 승인 재검증

## [2026-03-12 20:11] 계약 거래처 승인 후 견적 CONVERT actor 보정

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` — 계약서 거래처 승인 후 자동 완료되는 견적서 CONVERT 로그는 사용자 직접 행위가 아니라 후속 동기화이므로 `SYSTEM` actor로 호출하도록 변경
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java` — 계약 거래처 승인 시 견적 완료 후처리가 `SYSTEM/null actorId`로 호출되는지 기대값 보정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
프론트 계약 거래처 승인 재검증

## [2026-03-13 10:46] deal log 정책 변경사항 작업 메모 정리

### 작업 내용
- 수정 파일: `output.txt` — RFQ/QUO/CNT/ORD/STMT/INV 기준 deal log, approval 정리, deal 생성/종결 정책을 현재 코드 기준으로 정리하고 수정 우선순위, 파일별 TODO, 단계별 검증 항목을 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음 (문서성 작업으로 파일 내용 검토 수행)

### 다음 단계
- output.txt 기준으로 실제 코드 수정 착수

## [2026-03-13 11:56] deal lifecycle 삭제/만료 정책 반영

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java` — RFQ 생성 시 새 deal bootstrap 강제, RFQ 삭제 시 삭제 로그와 deal close 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationService.java` — QUO 삭제 로그 추가, 만료 동기화 후 관련 deal close 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java` — CNT 삭제 로그 추가, 만료 동기화 후 관련 deal close 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java` — ORD 취소 시 pending approval cancel 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java` — STMT cancel API가 삭제 정책 의미임을 주석으로 명시
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/core/entity/SalesDeal.java` — close idempotent 가드 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestServiceTest.java` — RFQ 새 deal 생성/삭제 로그/close 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationServiceTest.java` — QUO 삭제 로그 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/service/QuotationSyncTest.java` — QUO 만료 시 deal close 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractServiceTest.java` — CNT 삭제 로그 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractSyncTest.java` — CNT 만료 시 deal close 검증 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderServiceTest.java` — ORD 취소 approval cancel 회귀 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCancellationServiceTest.java` — ORD approval cancel 단위 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/deal/core/entity/SalesDealTest.java` — deal close idempotent 테스트 추가
- 수정 파일: `docs/statistics/statistics-architecture.md` — 구조 변경 이력 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
없음
