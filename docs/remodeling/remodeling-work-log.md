# Remodeling Work Log

## [2026-03-15 03:40] AGENTS v2 버전 계층 계획 추가 및 문구 보정

### 작업 내용
- 수정 파일: `AGENTS.md` — `/api/v2/**` 신규 버전 계층 추가 계획, 권장 패키지 구조, 1차/보류 대상, 구현 단계, 주의사항을 추가
- 수정 파일: `AGENTS.md` — 범위 외 항목의 `AGENTS.md 자체 수정` 문구를 사용자 명시 요청 없는 임의 수정 금지로 보정
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 리모델링 전용 작업 로그 파일 신규 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
`/api/v2/**` 1차 대상 도메인별 컨트롤러/서비스 패키지 초안 정리

## [2026-03-15 03:48] AGENTS 엔티티/Enum 보존 규칙 추가

### 작업 내용
- 수정 파일: `AGENTS.md` — 엔티티/enum 변경 시 기존 내용을 바로 삭제하지 말고 우선 주석 또는 대체 보존 방식으로 남기도록 지시사항 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
사용자 요청 시 AGENTS 문서 변경분 stage 및 커밋

## [2026-03-15 04:05] 리모델링 진행 체크리스트 문서 추가

### 작업 내용
- 수정 파일: `PROGRESS.md` — 권장 구현 순서를 기준으로 단계별 체크리스트와 현재 분석 진행 상태를 추가
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 진행 체크리스트 추가 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
1단계 현재 구조 파악 세부 분석 계속 진행

## [2026-03-15 04:17] 현재 구조 파악 1단계 분석 완료

### 작업 내용
- 수정 파일: `PROGRESS.md` — 문서 상태 결합, deal 자동 연결, snapshot 갱신, 일정/알림/통계 귀속 기준에 대한 1단계 분석 결과를 반영하고 다음 단계 포커스를 갱신
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 1단계 분석 완료 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
2단계 v2 정책 반영용 설계 뼈대 작성

## [2026-03-15 04:26] v2 공통 모델 초안 추가

### 작업 내용
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — `v2` 공통 상태 enum과 deal 중심 DTO 추가 구조를 기록
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentLifecycleStatus.java` — 문서 생명주기 상태 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentApprovalStatus.java` — 문서 승인 상태 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentRole.java` — 문서 대표성 역할 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/RevisionInfoDto.java` — 재작성 계보 DTO 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealSnapshotDto.java` — deal snapshot DTO 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealSummaryDto.java` — deal 목록 DTO 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDetailDto.java` — deal 상세 DTO 초안 추가
- 수정 파일: `PROGRESS.md` — 2단계, 3단계 완료 처리 및 4단계 포커스 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 컴파일 확인 전

### 다음 단계
4단계 deal 중심 조회 계층 작성

## [2026-03-15 04:39] v2 deal 조회 계층 초안 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java` — `/api/v2/deals` 목록/상세/문서목록 조회 엔드포인트 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2QueryService.java` — 기존 deal/document 리포지토리 재사용 기반의 `v2` 조회 서비스 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDocumentSummaryDto.java` — deal 문서 목록 응답 DTO 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — `v2` deal 조회 계층 구조를 기록
- 수정 파일: `PROGRESS.md` — 4단계 완료 처리 및 5단계 포커스 반영
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — `v2` deal 조회 계층 추가 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 최초 compileJava 실패(`Iterable.stream()`, `updatedAt` getter) 후 수정하여 재컴파일 성공

### 다음 단계
5단계 문서 생성/재작성/승인/취소 흐름 개편

## [2026-03-15 05:06] v2 QUO/CNT 생성 및 재작성 초안 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/entity/QuotationHeader.java` — 견적서 재작성 계보 필드와 설정 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/entity/ContractHeader.java` — 계약서 재작성 계보 필드와 설정 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/repository/QuotationRepository.java` — revision group 기준 최신 revision 조회 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/repository/ContractRepository.java` — revision group 기준 최신 revision 조회 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/**` — 견적서 v2 생성/재작성 DTO, 서비스, 컨트롤러 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/**` — 계약서 v2 생성/재작성 DTO, 서비스, 컨트롤러 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDocumentCommandResultDto.java` — 생성/재작성 결과 DTO 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 5단계 명령 계층 초안 구조 기록
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 5단계 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → `ContractV2CommandService` 에 `ClientRepository`, `EmployeeRepository` import 누락으로 최초 실패 후 수정하여 재컴파일 성공

### 다음 단계
5단계 컴파일 확인 및 cancel/supersede 보완

## [2026-03-15 05:22] v2 cancel 및 snapshot 재동기화 초안 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2SnapshotSyncService.java` — document summary 기반 deal snapshot 재동기화 서비스 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java` — v2 견적서 취소, approval 취소, snapshot 재동기화 후처리 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/service/ContractV2CommandService.java` — v2 계약서 취소, approval 취소, snapshot 재동기화 후처리 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/controller/QuotationV2Controller.java` — 견적서 cancel API 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/controller/ContractV2Controller.java` — 계약서 cancel API 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — cancel/snapshot 후처리 구조 기록
- 수정 파일: `PROGRESS.md` — 5단계 완료 처리 및 6단계 포커스 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
6단계 snapshot 재계산 로직 일반화

## [2026-03-15 05:33] v2 snapshot 재계산 경로 일반화

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2SnapshotSyncService.java` — 대표 문서 우선순위 비교와 공통 재계산 진입점을 정리
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java` — 견적서 생성/재작성/취소 후 공통 snapshot 재계산 경로 사용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/service/ContractV2CommandService.java` — 계약서 생성/재작성/취소 후 공통 snapshot 재계산 경로 사용
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 6단계 snapshot 일반화 구조 기록
- 수정 파일: `PROGRESS.md` — 6단계 완료 처리 및 7단계 포커스 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 추가 검증
- `./gradlew compileJava` 성공
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepositoryTest' --tests 'com.monsoon.seedflowplus.domain.deal.core.controller.DocumentSummaryQueryControllerTest' --tests 'com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationSyncIntegrationTest' --tests 'com.monsoon.seedflowplus.domain.sales.contract.service.ContractSyncIntegrationTest'` 성공

### 다음 단계
7단계 알림/일정/통계 연계 수정 및 v1 회귀 확인

## [2026-03-15 05:47] v2 deal 문맥 알림/일정 조회 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2ContextQueryService.java` — deal 문맥 알림/일정 조회 서비스 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java` — deal notifications/schedules 조회 API 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java` — deal 문맥 알림 조회용 repository query 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java` — 견적서 v2 생성 시 만료 일정 upsert 연계 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 7단계 deal 문맥 조회 구조 기록
- 수정 파일: `PROGRESS.md` — 7단계 진행 현황 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 추가 검증
- `./gradlew compileJava` 성공
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepositoryTest' --tests 'com.monsoon.seedflowplus.domain.deal.core.controller.DocumentSummaryQueryControllerTest' --tests 'com.monsoon.seedflowplus.domain.schedule.controller.ScheduleControllerTest' --tests 'com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationSyncIntegrationTest' --tests 'com.monsoon.seedflowplus.domain.sales.contract.service.ContractSyncIntegrationTest'` 성공

### 다음 단계
7단계 통계/KPI v2 반영 범위 정리 후 8단계 보안/테스트 확장

## [2026-03-15 06:21] v2 deal KPI 조회 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealKpiDto.java` — deal KPI 응답 DTO 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2KpiQueryService.java` — deal KPI 집계 서비스 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java` — `/api/v2/deals/kpis` 조회 API 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — deal KPI 조회 구조 기록
- 수정 파일: `PROGRESS.md` — 7단계 진행 현황에 KPI 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 추가 검증
- `./gradlew compileJava` 성공
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepositoryTest' --tests 'com.monsoon.seedflowplus.domain.deal.core.controller.DocumentSummaryQueryControllerTest' --tests 'com.monsoon.seedflowplus.domain.schedule.controller.ScheduleControllerTest' --tests 'com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationSyncIntegrationTest' --tests 'com.monsoon.seedflowplus.domain.sales.contract.service.ContractSyncIntegrationTest' --tests 'com.monsoon.seedflowplus.domain.statistics.controller.StatisticsControllerTest'` 성공

### 다음 단계
7단계 잔여 이슈 정리 후 8단계 보안 매처와 v2 테스트 확장

## [2026-03-15 06:45] v2 보안 매처 및 컨트롤러 테스트 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java` — `/api/v2/quotations`, `/api/v2/contracts` 의 생성/재작성/취소 권한 매처 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java` — 테스트 환경용 v2 보안 매처 반영
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryControllerTest.java` — deal 조회/KPI 인증 경계 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/controller/QuotationV2ControllerTest.java` — 견적 v2 생성/취소 권한 테스트 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/v2/controller/ContractV2ControllerTest.java` — 계약 v2 생성/취소 권한 테스트 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 보안 매처 및 컨트롤러 테스트 구조 기록
- 수정 파일: `PROGRESS.md` — 8단계 완료 및 잔여 이슈 상태 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → `DealV2QueryControllerTest` 의 `getAuthorities()` mock 제네릭 반환 타입 문제를 수정한 뒤 재검증 성공

### 추가 검증
- `./gradlew compileJava` 성공
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.deal.v2.controller.DealV2QueryControllerTest' --tests 'com.monsoon.seedflowplus.domain.sales.quotation.v2.controller.QuotationV2ControllerTest' --tests 'com.monsoon.seedflowplus.domain.sales.contract.v2.controller.ContractV2ControllerTest' --tests 'com.monsoon.seedflowplus.domain.deal.core.controller.DocumentSummaryQueryControllerTest' --tests 'com.monsoon.seedflowplus.domain.schedule.controller.ScheduleControllerTest' --tests 'com.monsoon.seedflowplus.domain.statistics.controller.StatisticsControllerTest'` 성공

### 다음 단계
7단계 잔여 이슈 정리 및 9단계 문서 업데이트 정리

## [2026-03-15 09:55] 일정 soft-cancel 및 v2 billing revenue 래퍼 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealSchedule.java` — 거래 일정 상태 필드와 cancel 전환 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealScheduleStatus.java` — 거래 일정 상태 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/repository/DealScheduleRepository.java` — 취소 일정 제외 조회 메서드로 전환
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryService.java` — deal 일정 조회에서 `CANCELLED` 제외 처리 반영
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncService.java` — `deleteByExternalKey` 를 soft-cancel 동작으로 변경
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/response/ScheduleItemDto.java` — deal 일정 응답 status 포함
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/v2/controller/BillingRevenueStatisticsV2Controller.java` — v2 billing revenue 래퍼 API 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/infra/security/SecurityConfig.java` — v2 billing revenue 보안 매처 추가
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/config/TestSecurityConfig.java` — 테스트 보안 매처 반영
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/schedule/**` — 일정 상태 도입에 맞춘 entity/query/repository/sync 테스트 수정
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/v2/controller/BillingRevenueStatisticsV2ControllerTest.java` — v2 billing revenue 권한/응답 테스트 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 일정 soft-cancel 및 v2 billing revenue 구조 기록
- 수정 파일: `PROGRESS.md` — 7단계 완료 및 현재 포커스 갱신

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → `ScheduleQueryServiceTest` 의 repository 메서드명 정합성 수정 후 재검증 성공

### 추가 검증
- `./gradlew compileJava` 성공
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncServiceTest' --tests 'com.monsoon.seedflowplus.domain.schedule.query.ScheduleQueryServiceTest' --tests 'com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleTest' --tests 'com.monsoon.seedflowplus.domain.schedule.repository.DealScheduleRepositoryTest' --tests 'com.monsoon.seedflowplus.domain.schedule.controller.ScheduleControllerTest' --tests 'com.monsoon.seedflowplus.domain.statistics.billing.controller.BillingRevenueStatisticsControllerTest' --tests 'com.monsoon.seedflowplus.domain.statistics.billing.v2.controller.BillingRevenueStatisticsV2ControllerTest' --tests 'com.monsoon.seedflowplus.domain.statistics.controller.StatisticsControllerTest'` 성공
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.deal.core.controller.DocumentSummaryQueryControllerTest'` 성공

### 다음 단계
9단계 문서 업데이트 정리 및 10단계 최종 점검

## [2026-03-15 10:08] v2 문서 인벤토리 및 전환 가이드 정리

### 작업 내용
- 수정 파일: `docs/api/domain-api-list.csv` — v2 deal/quotation/contract/statistics API 목록 추가
- 수정 파일: `api-test/http/deal/deal-v2.http` — v2 deal 조회/KPI/manual test 요청 컬렉션 추가
- 수정 파일: `api-test/http/deal/document-v2.http` — v2 quotation/contract 생성·재작성·취소 요청 컬렉션 추가
- 수정 파일: `api-test/http/statistics/billing-revenue-v2.http` — v2 billing revenue 요청 컬렉션 추가
- 수정 파일: `docs/remodeling/v1-v2-transition-guide.md` — v1→v2 전환 가이드 초안 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 9단계 문서 정리 구조 기록
- 수정 파일: `PROGRESS.md` — 9단계 완료 및 10단계 포커스 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 코드 변경 없음

### 다음 단계
10단계 최종 점검 및 전환 가이드 보완
