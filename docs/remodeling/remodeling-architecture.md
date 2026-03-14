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
