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
