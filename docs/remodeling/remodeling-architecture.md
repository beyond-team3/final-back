# Remodeling Architecture

## [2026-03-15] v2 공통 모델 초안 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentLifecycleStatus.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentApprovalStatus.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentRole.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/RevisionInfoDto.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealSnapshotDto.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealSummaryDto.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDetailDto.java`

### 변경 내용
`/api/v2/**` 리모델링의 기준 타입으로 사용할 3축 문서 상태 enum과 deal 중심 조회 DTO 초안을 추가합니다.
기존 `v1`의 `DocumentStatus` 문자열 결합을 건드리지 않고, `v2` 전용 타입을 별도 패키지에 분리합니다.
`DealSnapshotDto` 는 대표 문서, 현재 단계, 마지막 활동 시각을 한 곳에 모으고, `RevisionInfoDto` 는 재작성 계보 필드를 담도록 정의합니다.

### 변경 이유
정책 결정 사항의 상태 3축 분리, 재작성 계보 보존, deal 중심 조회 전환을 `v2` 타입 레벨에서 먼저 고정하기 위함입니다.

## [2026-03-15] v2 deal 조회 계층 초안 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2QueryService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDocumentSummaryDto.java`

### 변경 내용
`GET /api/v2/deals`, `GET /api/v2/deals/{dealId}`, `GET /api/v2/deals/{dealId}/documents` 초안을 추가했습니다.
기존 `SalesDealRepository`, `DocumentSummaryRepository` 를 재사용하되, 응답은 `v2` 전용 DTO로 매핑합니다.
현재 snapshot의 3축 상태 값과 `dealCode`, `dealTitle` 은 아직 저장 필드가 없어 `null` 로 유지하고, 이후 엔티티 확장 단계에서 채우도록 경계를 분리했습니다.

### 변경 이유
정책상 메인 조회 기준이 deal 중심으로 전환되어야 하므로, 생성/재작성 개편 전에 조회 계약을 먼저 고정하기 위함입니다.

## [2026-03-15] v2 QUO/CNT 생성 및 재작성 초안 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/controller/QuotationV2Controller.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/controller/ContractV2Controller.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/service/ContractV2CommandService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/entity/QuotationHeader.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/entity/ContractHeader.java`

### 변경 내용
`/api/v2/quotations`, `/api/v2/contracts`, `/api/v2/quotations/{id}/revise`, `/api/v2/contracts/{id}/revise` 초안을 추가했습니다.
`v2` 생성 로직은 상위 문서가 있으면 deal 계승, 없으면 요청의 `dealId`, 둘 다 없으면 새 deal 생성 규칙만 허용하며, 기존 열린 deal 자동 연결은 사용하지 않습니다.
재작성은 원본 문서를 수정하지 않고 새 문서를 생성하며, `sourceDocumentId`, `revisionGroupKey`, `revisionNo` 계보 필드를 QUO/CNT 엔티티에 추가합니다.

### 변경 이유
정책 결정 사항의 핵심인 자동 deal 연결 금지와 재작성의 새 문서 생성 원칙을 `v2` 서비스에 먼저 고정하기 위함입니다.

## [2026-03-15] v2 cancel 및 snapshot 동기화 초안 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2SnapshotSyncService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/service/ContractV2CommandService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/controller/QuotationV2Controller.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/controller/ContractV2Controller.java`

### 변경 내용
`PATCH /api/v2/quotations/{id}/cancel`, `PATCH /api/v2/contracts/{id}/cancel` 초안을 추가했습니다.
`v2` 취소는 현재 스키마 제약상 저장 레벨에서는 기존 `DELETED` 상태를 사용하지만, API 의미는 명시적 `cancel`로 분리하고 승인 요청 취소를 함께 수행합니다.
취소 후에는 `DocumentSummary` 전체 문서를 기준으로 대표 문서를 다시 계산해 `SalesDeal` snapshot을 재동기화합니다.

### 변경 이유
5단계의 남은 과제인 취소 흐름 분리와 approval/snapshot 후처리 연결을 `v2`에서 먼저 명시화하기 위함입니다.

## [2026-03-15] v2 snapshot 재계산 경로 일반화

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2SnapshotSyncService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/service/ContractV2CommandService.java`

### 변경 내용
`DealV2SnapshotSyncService` 에 대표 문서 선정 우선순위를 한 곳에 모으고, 생성/재작성/취소 후 모두 `recalculateAfterMutation(...)` 경로를 타도록 정리했습니다.
현재 스키마에는 `documentRole` 필드가 없어서 representative 우선순위는 상태 기반 힌트로 근사하며, 뒤 단계 우선 → representative 후보 상태 우선 → 상태 우선순위 → `createdAt` 최신 순으로 계산합니다.

### 변경 이유
6단계 목표인 snapshot 재계산 로직의 공통화와 수동 갱신 경로 축소를 먼저 `v2` 계층에서 달성하기 위함입니다.

## [2026-03-15] v2 deal 문맥 알림/일정 조회 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2ContextQueryService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java`

### 변경 내용
`GET /api/v2/deals/{dealId}/notifications`, `GET /api/v2/deals/{dealId}/schedules` 를 추가해 deal 문맥으로 알림과 일정을 조회할 수 있게 했습니다.
알림은 deal 자체 알림과 deal에 속한 문서 알림을 함께 모아서 반환하고, 일정은 기존 `ScheduleQueryService`를 재사용해 deal 단위로 집계 조회합니다.
견적서 v2 생성 시에는 기존 만료 일정 upsert를 재사용해 문서 단위 일정 생성 원칙을 맞췄습니다.

### 변경 이유
7단계 목표인 알림/일정의 deal 중심 조회와 문서 단위 일정 연계를 `v2`에서 먼저 제공하기 위함입니다.

제약 사항:
현재 `DealSchedule` 엔티티에는 상태 컬럼이 없어, 정책의 "삭제 대신 inactive/cancelled 상태 전환"은 스키마 변경 없이 완전 반영할 수 없습니다.
이번 단계에서는 deal 문맥 조회와 생성 시 일정 upsert만 반영하고, 취소 상태 전환은 후속 단계로 남깁니다.

## [2026-03-15] v2 deal KPI 조회 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealKpiDto.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2KpiQueryService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java`

### 변경 내용
`GET /api/v2/deals/kpis` 를 추가해 deal 수, open/closed 수, 성공률, 평균 리드타임, QUO→CNT 전환율, 재작성률을 조회할 수 있게 했습니다.
성공 판단은 현재 정책에 맞춰 `PAY COMPLETED` 문서 존재 여부로 계산하고, 리드타임은 deal 생성 시각부터 최초 `PAY COMPLETED` 시각까지의 일수 평균으로 계산합니다.
기존 v1 통계 API는 그대로 유지하고, v2에서는 deal 중심 KPI만 별도로 분리했습니다.

### 변경 이유
7단계 목표인 deal KPI 추가를 v2 조회 계층에서 먼저 제공하기 위함입니다.

## [2026-03-15] v2 보안 매처 및 컨트롤러 테스트 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java`
- 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java`
- 파일: `src/test/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryControllerTest.java`
- 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/controller/QuotationV2ControllerTest.java`
- 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/v2/controller/ContractV2ControllerTest.java`

### 변경 내용
`/api/v2/quotations`, `/api/v2/contracts` 와 각 `revise`, `cancel` 경로에 `SALES_REP` 권한 매처를 추가하고 테스트 보안 설정에도 동일 규칙을 반영했습니다.
`DealV2QueryController`, `QuotationV2Controller`, `ContractV2Controller` 에 대해 인증 성공, 비인증 `401`, 권한 부족 `403` 시나리오를 검증하는 `WebMvcTest` 를 추가했습니다.
검증은 신규 `v2` 컨트롤러 테스트와 함께 기존 `DocumentSummaryQueryControllerTest`, `ScheduleControllerTest`, `StatisticsControllerTest` 를 같이 실행해 `v1` 경로 회귀 여부를 확인했습니다.

### 변경 이유
8단계 목표인 `/api/v2/**` 접근 제어 반영과 `v1` 유지 조건 하의 테스트 확장을 분리된 보안 계층에서 마무리하기 위함입니다.

## [2026-03-15] 일정 soft-cancel 및 v2 billing revenue 래퍼 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealSchedule.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealScheduleStatus.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/repository/DealScheduleRepository.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/v2/controller/BillingRevenueStatisticsV2Controller.java`

### 변경 내용
`DealSchedule` 에 `DealScheduleStatus` 를 추가하고, 기존 `deleteByExternalKey(...)` 호출은 물리 삭제 대신 `CANCELLED` 상태 전환으로 처리하도록 바꿨습니다.
거래 일정 조회는 `CANCELLED` 상태를 제외하도록 repository/query 계층을 조정했고, deal 일정 응답에도 상태 문자열을 포함하도록 맞췄습니다.
청구 매출 통계는 기존 `BillingRevenueStatisticsQueryService` 를 그대로 재사용하는 `/api/v2/statistics/billing/revenue/**` 래퍼 컨트롤러를 추가해 `PAY COMPLETED` 기준을 유지한 채 v2 경로를 분리했습니다.

### 변경 이유
7단계 목표인 일정의 삭제 금지/상태 전환 정책과 매출 통계의 v2 전용 경로 분리를 동시에 마무리하기 위함입니다.

## [2026-03-15] v2 문서 인벤토리 및 전환 가이드 정리

### 변경 대상
- 파일: `docs/api/domain-api-list.csv`
- 파일: `api-test/http/deal/deal-v2.http`
- 파일: `api-test/http/deal/document-v2.http`
- 파일: `api-test/http/statistics/billing-revenue-v2.http`
- 파일: `docs/remodeling/v1-v2-transition-guide.md`

### 변경 내용
`domain-api-list.csv` 에 현재까지 추가된 v2 deal/quotation/contract/statistics 엔드포인트를 등록했습니다.
IntelliJ HTTP Client 기준으로 deal 조회, QUO/CNT 명령, billing revenue v2 호출 예제를 별도 파일로 분리했습니다.
`v1-v2-transition-guide.md` 에 현재 v2 적용 범위, 정책 차이, QA 체크포인트, 남은 전환 작업을 초안으로 정리했습니다.

### 변경 이유
9단계 목표인 API 인벤토리, 수동 검증 컬렉션, 전환 문서 초안을 병행 관리하기 위함입니다.

## [2026-03-15] 최종 점검 및 전환 체크리스트 보강

### 변경 대상
- 파일: `PROGRESS.md`
- 파일: `docs/remodeling/v1-v2-transition-guide.md`

### 변경 내용
최종 검증 대상으로 v2 컨트롤러 테스트와 v1 회귀 테스트 묶음을 다시 실행하고, 진행표의 10단계를 완료 처리했습니다.
전환 가이드에는 운영 전환 체크리스트와 현재 남아 있는 모델링/범위 리스크를 추가해 실제 배포 전 확인 항목을 문서화했습니다.

### 변경 이유
10단계 목표인 최종 점검과 전환 가이드 초안의 운영 관점 보강을 마무리하기 위함입니다.

## [2026-03-15] 명세서 상세 응답 보강 및 관리자 수동 청구서 초안 생성 추가

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/statement/dto/response/StatementResponse.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/controller/InvoiceController.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java`

### 변경 내용
명세서 상세 응답에 계약/딜/거래처/작성자/배송/품목 정보를 추가해 프론트가 다른 문서 상세 모달과 유사한 형태로 렌더링할 수 있게 했습니다.
관리자 전용 `POST /api/v1/invoices/contracts/{contractId}/manual-draft` 엔드포인트를 추가해 billing cycle 스케줄을 기다리지 않고 특정 계약 기준 청구서 초안을 즉시 생성할 수 있게 했습니다.
기존 자동 초안 생성 로직은 내부 공통 메서드로 재사용하고, 보안 매처는 ADMIN 전용으로 분리했습니다.

### 변경 이유
프론트 명세서 상세 빈 화면 문제를 DTO 수준에서 해소하고, billing cycle 기반 청구서 플로우를 운영 전 테스트할 수 있는 수동 트리거를 제공하기 위함입니다.

## [2026-03-15] 수동 청구서 초안 담당자 귀속 및 발행 권한 분리

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/repository/InvoiceRepository.java`
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java`

### 변경 내용
관리자 수동 생성 청구서 초안에도 계약의 deal owner, 거래처 담당자, 계약 작성자를 순서대로 해석해 `Invoice.employee`를 채우도록 조정했습니다.
영업사원 청구서 목록은 기존 `employee_id` 단일 기준 대신 invoice 담당자, 거래처 담당자, deal owner 범위를 모두 포함하는 조회로 확장했습니다.
청구서 발행은 서비스에서 `SALES_REP` 전용으로 강제하고, 조회 권한 검증을 재사용해 담당 범위를 벗어난 발행을 차단했습니다.

### 변경 이유
관리자가 초안을 생성한 뒤 담당 영업사원이 동일 청구서를 조회·발행할 수 있어야 하고, 반대로 관리자의 발행이나 비담당 영업사원의 발행은 허용하지 않기 때문입니다.

## [2026-03-15] SSE 연결 종료 로그 노이즈 완화

### 변경 대상
- 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java`
- 파일: `src/test/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseServiceTest.java`

### 변경 내용
SSE 전송 실패 시 `Broken pipe`, `AsyncRequestNotUsableException`, Tomcat `ClientAbortException` 계열을 정상적인 클라이언트 연결 종료로 분류해 stacktrace 없는 `debug` 로그만 남기도록 조정했습니다.
그 외 예외는 기존처럼 `warn`으로 유지하고, 실패 emitter 제거 동작은 그대로 유지합니다.
로그 레벨 분기와 emitter 제거가 의도대로 동작하는 단위 테스트를 추가했습니다.

### 변경 이유
같은 브라우저에서 계정 전환·새로고침 시 반복적으로 발생하는 SSE 종료 로그가 운영 콘솔을 오염시키지 않게 하면서, 실제 알림 전송 이상은 계속 식별할 수 있게 하기 위함입니다.
