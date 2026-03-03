# Schedule 기능 구현 정리

## 1. 개요
- 도메인 경로: `src/main/java/com/monsoon/seedflowplus/domain/schedule`
- 목적:
  - 개인 일정(Personal) CRUD
  - 거래 일정(Deal) 읽기 + 동기화 upsert
  - 통합 조회에서 PERSONAL/DEAL 병합 반환

## 2. 패키지 구조
- `controller`
  - `ScheduleController`
- `service`
  - `ScheduleQueryService`
  - `PersonalScheduleCommandService`
  - `DealScheduleSyncService`
- `repository`
  - `PersonalScheduleRepository`
  - `DealScheduleRepository`
- `entity`
  - `PersonalSchedule`, `DealSchedule`
  - enum: `ScheduleStatus`, `ScheduleVisibility`, `ScheduleSource`, `DealScheduleEventType`, `DealDocType`
- `dto/request`
  - `ScheduleSearchCondition`
  - `PersonalScheduleCreateRequest`, `PersonalScheduleUpdateRequest`
  - `DealScheduleUpsertCommand`
- `dto/response`
  - `ScheduleItemDto`, `ScheduleItemType`
- `dto/validation`
  - `ValidTimeRange`, `ValidTimeRangeValidator`

## 3. 엔티티/테이블

### 3.1 PersonalSchedule
- 테이블: `tbl_pers_sked`
- PK: `psked_id` (`BaseEntity.id` override)
- 주요 컬럼:
  - `owner_id` (User FK)
  - `title`(200), `description`(TEXT)
  - `start_at`, `end_at`, `all_day`
  - `status`(enum), `visibility`(enum)
- 인덱스:
  - `idx_pers_sked_owner_start_at(owner_id, start_at)`
  - `idx_pers_sked_owner_end_at(owner_id, end_at)`

### 3.2 DealSchedule
- 테이블: `tbl_deal_sked`
- PK: `deal_sked_id` (`BaseEntity.id` override)
- 주요 컬럼:
  - `deal_id` (SalesDeal FK), `client_id` (Client FK), `assignee_user_id` (User FK)
  - `title`(200), `description`(TEXT)
  - `start_at`, `end_at`
  - `event_type`, `doc_type`
  - `ref_doc_id`, `ref_deal_log_id`
  - `source`, `external_key`, `last_synced_at`
- 제약/인덱스:
  - UNIQUE: `uk_deal_sked_external_key(external_key)`
  - `idx_deal_sked_assignee_start_at(assignee_user_id, start_at)`
  - `idx_deal_sked_client_start_at(client_id, start_at)`
  - `idx_deal_sked_deal_start_at(deal_id, start_at)`
  - `idx_deal_sked_start_end(start_at, end_at)`

## 4. API

### 4.1 통합 조회
- `GET /api/schedules`
- query params:
  - 필수: `from`, `to` (ISO LocalDateTime)
  - 선택: `assigneeUserId`, `clientId`, `dealId`
  - 선택: `includePersonal`(기본 true), `includeDeal`(기본 true)
- 응답: `ApiResult<List<ScheduleItemDto>>`
- 동작:
  - 개인 일정 + 거래 일정을 합쳐 `startAt ASC, id ASC` 정렬

### 4.2 개인 일정 CRUD
- `POST /api/schedules/personal` -> 생성 ID 반환
- `GET /api/schedules/personal/{id}` -> 본인 일정 단건
- `PUT /api/schedules/personal/{id}` -> 본인 일정 수정
- `DELETE /api/schedules/personal/{id}` -> 상태 `CANCELED` 처리(soft delete 성격)

### 4.3 거래 일정 API 정책
- 거래 일정 전용 CRUD API는 제공하지 않음
- 조회는 통합 조회에서만 노출
- 생성/수정은 `DealScheduleSyncService.upsertFromEvent(...)`로만 수행

## 5. 서비스 규칙

### 5.1 PersonalScheduleCommandService
- actor는 `CustomUserDetails.userId` 기준
- owner 본인만 조회/수정/삭제 가능
- 삭제는 물리 삭제가 아니라 `status=CANCELED`
- 검증:
  - request 시간 범위 방어 (`startAt < endAt`)
  - owner+status 조건으로 조회 (`CANCELED` 제외)

### 5.2 ScheduleQueryService
- 공통 검증:
  - actorUserId 필수
  - `includePersonal/includeDeal` 둘 다 false면 오류
  - `rangeStart < rangeEnd`
- PERSONAL 조회:
  - actor 본인 owner 강제, `CANCELED` 제외
- DEAL 조회 권한:
  - `ADMIN`: assignee/client/deal 필터 조합 허용
  - `SALES_REP`: 담당 거래처 목록(`ClientRepository.findAllByManagerEmployeeId`) 범위 내 조회
  - `CLIENT`: 본인 client 범위만 허용, 추가 필터 금지

### 5.3 DealScheduleSyncService
- `externalKey` 기반 upsert
- 유니크 충돌 시(`DataIntegrityViolationException`) 재조회 후 update 재시도
- 검증:
  - 필수 필드/시간 범위
  - `deal.client`와 입력 `client` 일치 검증

## 6. DTO/검증
- `PersonalScheduleCreateRequest`
  - `title` 필수 + 최대 200
  - `startAt/endAt/status/visibility` 필수
  - `@ValidTimeRange`로 `endAt > startAt`
- `PersonalScheduleUpdateRequest`
  - `title` 필수 + 최대 200
  - `startAt/endAt` 필수
  - `status/visibility`는 nullable(미전달 시 기존 값 유지)
  - `@ValidTimeRange` 적용
- `DealScheduleUpsertCommand`
  - canonical constructor에서 필수/양수/시간 범위 검증

## 7. Repository 포인트
- 기간 겹침(overlap) 조건 사용:
  - `startAt < rangeEnd AND endAt > rangeStart`
- 모든 조회 메서드는 `startAt ASC, id ASC`
- N+1 완화:
  - `@EntityGraph`로 `owner` 또는 `assigneeUser/client/deal` preload

## 8. 예외 처리
- 프로젝트 공통 `CoreException(ErrorType)` 사용
- 주요 오류 유형:
  - `UNAUTHORIZED`, `ACCESS_DENIED`, `INVALID_INPUT_VALUE`
  - `PERSONAL_SCHEDULE_NOT_FOUND`, `DEAL_NOT_FOUND`, `CLIENT_NOT_FOUND`, `USER_NOT_FOUND`

## 9. 향후 개선 후보
- 통합 조회 전용 projection 쿼리로 read-model 최적화
- `DealScheduleSyncService`에 이벤트 source별 감사정보 확장
- 통합 조회 페이징 옵션 추가(필요 시)
