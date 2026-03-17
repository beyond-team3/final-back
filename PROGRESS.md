## [2026-03-16 17:59] Contract Immediate Activation

### 현재 목표
- 거래처 승인 시 시작일이 승인일과 같거나 이전이면 즉시 ACTIVE_CONTRACT 전이

### 사전 점검
- AGENTS.md: 확인 완료. 코드 수정 시마다 remodeling architecture/work log 갱신과 커밋 준비가 필요함.
- REMODELING_POLICY.md: 직접 충돌 없음. v2의 상태 3축 분리 방향과는 별개로 현재 v1 `ContractStatus` 의미 조정 작업이지만, `COMPLETED` 의미 축소가 deal snapshot/조회 우회/배치에 미치는 영향 확인 필요.
- 영향 도메인: Approval, Contract, Deal/DealLog, Notification, Schedule, Invoice, v2 snapshot, contract 활성 조회, 상태 전이 정책 테스트가 영향권.

### 확인한 사실
- `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java` `applyContractDecision(...)`: 거래처 승인 시 현재 무조건 `WAITING_CLIENT -> COMPLETED` 전이 후 upstream 문서를 동기화함.
- `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractService.java` `syncContractStatuses()`: 현재 배치가 `COMPLETED -> ACTIVE_CONTRACT`, `ACTIVE_CONTRACT -> EXPIRED` 둘 다 담당함.
- `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/repository/ContractRepository.java` `findActiveContractsByClient(...)`: `ACTIVE_CONTRACT` 외에 `startDate <= today <= endDate` 인 `COMPLETED`도 활성 계약으로 간주하는 우회 조회가 존재함.
- `src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/scheduler/InvoiceScheduler.java`: 자동 청구서 생성 대상 계약을 `ContractStatus.COMPLETED`만 조회함.
- `src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java`: 수동 청구서 생성은 `COMPLETED` 또는 `ACTIVE_CONTRACT` 모두 허용함.
- `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java`: 승인 decision 로그는 `ApprovalCommandService`가 넘긴 `fromStatus/toStatus`, `fromStage/toStage`를 그대로 기록하므로 승인 즉시 활성화되면 log stage도 `APPROVED -> CONFIRMED`로 바뀜.
- `src/main/java/com/monsoon/seedflowplus/domain/approval/service/ContractApprovalSchedulesSyncEventHandler.java`: 거래처 계약 승인 after-commit 이벤트로 계약 시작/종료 일정과 예약 알림을 생성함. 상태값 자체는 직접 참조하지 않음.
- `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2SnapshotSyncService.java`: CNT `COMPLETED`는 `APPROVED`, `ACTIVE_CONTRACT`는 `CONFIRMED`로 매핑되어 있어 즉시 활성화 시 v2 snapshot 조회 결과도 달라짐.
- `src/main/java/com/monsoon/seedflowplus/domain/deal/log/policy/DocStatusTransitionPolicy.java`: CNT 승인 전이는 `WAITING_CLIENT -> COMPLETED`만 허용하고 `ACTIVE_CONTRACT`는 승인 전이 규칙에 없음.
- `src/test/java/com/monsoon/seedflowplus/domain/sales/contract/service/ContractSyncIntegrationTest.java`: 현재 정책을 고정하는 “승인 시점에는 COMPLETED 유지” 테스트가 존재함.

### 영향 도메인 분류
- ApprovalCommandService 및 승인 이벤트 흐름: 코드 수정 필요
- ContractService / ContractRepository / Scheduler: 코드 수정 필요
- Contract 관련 Notification 발행 조건: 테스트만 수정
- Contract 관련 Schedule 동기화 조건: 테스트만 수정
- Deal currentStage/currentStatus 갱신 규칙: 코드 수정 필요
- DealLog 기록값 (`fromStatus`, `toStatus`, `fromStage`, `toStage`): 코드 수정 필요
- Dashboard / Statistics 에서 `COMPLETED`, `ACTIVE_CONTRACT` 의미 사용 여부: 무영향으로 보이나 회귀 확인 필요
- Invoice 생성 가능 조건: 코드 수정 필요
- Order / Payment / Quotation / RFQ 연쇄 영향: 테스트만 수정
- `/api/v2/**` snapshot 또는 representative 계산 로직 영향: 무영향 (상태 매핑은 기존 분기가 이미 `COMPLETED`/`ACTIVE_CONTRACT`를 구분하고 있어 상태 원천값 변경만 반영됨)
- “COMPLETED 상태인 계약도 활성처럼 조회”하는 우회 로직의 필요성 재평가: 코드 수정 필요

### 결정 사항
- 거래처 승인 기준 시각은 우선 `ApprovalCommandService`의 `actionAt`을 기준으로 하고, 날짜 비교는 `actionAt.toLocalDate()`와 `contract.startDate` 비교로 구현하는 것이 가장 자연스럽다.
- 승인 직후 활성화되는 경우 배치의 activation 책임은 제거하고, 배치는 미래 시작 계약의 시작일 도래 처리와 기존 만료 처리만 남겨야 한다.
- 활성 계약 조회 우회 로직은 새 정책 이후 불필요할 가능성이 높아 제거 또는 축소 대상이다.
- 인보이스 스케줄러가 `COMPLETED`만 조회하는 현재 구현은 새 정책과 어긋날 가능성이 높다. 승인 당일 즉시 활성 계약도 자동 청구 대상에 포함되는지 확인이 필요하지만, 기간 체크가 이미 별도로 있어 `ACTIVE_CONTRACT` 기준 조회로 정리 가능성이 높다.

### 구현 진행
- [x] 영향 범위 조사
- [x] 승인 로직 수정
- [x] 배치 로직 정리
- [x] 테스트 수정/추가
- [x] 문서 업데이트
- [x] 컴파일/테스트 확인

### 구현 결과
- `ApprovalCommandService`가 계약 거래처 승인 시 `actionAt.toLocalDate()`와 `contract.startDate`를 비교해 `ACTIVE_CONTRACT` 또는 `COMPLETED`를 결정하도록 변경했다.
- `DocStatusTransitionPolicy`에 CNT 승인 결과로 `ACTIVE_CONTRACT`를 추가해 검증기와 새 승인 분기가 충돌하지 않도록 맞췄다.
- `ContractRepository.findActiveContractsByClient(...)`는 더 이상 `COMPLETED + 기간내` 우회를 사용하지 않고 `ACTIVE_CONTRACT`만 반환한다.
- `InvoiceService.createDraftInvoiceByAdmin(...)`는 `ACTIVE_CONTRACT`만 허용하도록 좁혔고, `InvoiceScheduler`도 자동 생성 대상을 `ACTIVE_CONTRACT`로 변경했다.
- 계약 상태 동기화 배치는 기존 벌크 전이를 유지하되, 주석/의미를 “미래 시작 승인 계약의 시작일 도래 처리”로 정리했다.
- v2 snapshot 코드는 수정하지 않았다. 기존 매핑이 `COMPLETED -> APPROVED`, `ACTIVE_CONTRACT -> CONFIRMED`를 이미 분리하고 있어 원천 상태값 변화만 반영된다.

### 리스크
- `ApprovalCommandService`가 현재 `docStatusTransitionValidator`를 통해 단일 `toStatus`를 검증하므로, 승인 시점 조건 분기가 추가되면 정책 테이블도 함께 수정해야 함.
- 인보이스 자동 생성이 의도상 “계약 체결 후 즉시 가능”인지 “계약 시작 후 가능”인지 코드만으로 완전히 명시되진 않는다. 다만 현재 스케줄러는 기간 체크를 같이 하고 있어 활성 계약 기준으로 정리하는 편이 자연스럽다.
- v2 snapshot은 v1 상태를 읽는 래퍼 성격이라, v1 상태 의미 변경이 조회 결과까지 바뀐다는 점을 감안해야 함.

### 검증 결과
- `./gradlew test --tests 'com.monsoon.seedflowplus.domain.approval.service.ApprovalCommandServiceTest' --tests 'com.monsoon.seedflowplus.domain.deal.log.policy.DocStatusTransitionPolicyTest' --tests 'com.monsoon.seedflowplus.domain.sales.contract.service.ContractSyncTest' --tests 'com.monsoon.seedflowplus.domain.sales.contract.service.ContractSyncIntegrationTest' --tests 'com.monsoon.seedflowplus.domain.billing.invoice.service.InvoiceServiceTest' --tests 'com.monsoon.seedflowplus.domain.billing.invoice.scheduler.InvoiceSchedulerTest'` 성공

### 다음 액션
- 정책 위반 여부와 예상 수정 범위를 먼저 보고한 뒤, 이상 없으면 승인 전이/배치/우회 조회/인보이스 조회 및 관련 테스트를 함께 수정한다.
