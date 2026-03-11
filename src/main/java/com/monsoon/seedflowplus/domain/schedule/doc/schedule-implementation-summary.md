# Schedule 기능 구현 정리

## 1. 개요
- 도메인 경로: `src/main/java/com/monsoon/seedflowplus/domain/schedule`
- 목적:
  - 개인 일정(Personal) 생성/조회/수정/삭제
  - 거래 일정(Deal) 조회 + 외부 이벤트 기반 upsert 동기화
  - 통합 조회에서 PERSONAL/DEAL 일정을 병합 반환

## 2. 패키지 구조
- `controller`
  - `ScheduleController`
- `command`
  - `PersonalScheduleCommandService`
- `query`
  - `ScheduleQueryService`
  - `ScheduleSearchCondition`
- `sync`
  - `DealScheduleSyncService`
  - `DealScheduleReferenceReader`, `DealScheduleReferenceReaderImpl`
- `repository`
  - `PersonalScheduleRepository`
  - `DealScheduleRepository`
- `entity`
  - `PersonalSchedule`, `DealSchedule`
  - enum: `ScheduleStatus`, `ScheduleVisibility`, `ScheduleSource`, `DealScheduleEventType`, `DealDocType`
- `dto/request`
  - `PersonalScheduleCreateRequest`, `PersonalScheduleUpdateRequest`
- `dto/command`
  - `DealScheduleUpsertCommand`
- `dto/response`
  - `ScheduleItemDto`, `ScheduleItemType`
- `validation`
  - `ValidTimeRange`, `ValidTimeRangeValidator`

## 3. 엔티티/테이블

### 3.1 PersonalSchedule
- 테이블: `tbl_pers_sked`
- PK: `personal_schedule_id` (`BaseModifyEntity.id` override)
- 주요 컬럼:
  - `owner_id` (User FK)
  - `title`(200), `description`(TEXT)
  - `start_at`, `end_at`, `all_day`
  - `status`(enum), `visibility`(enum)
  - `is_deleted`
- 삭제 정책:
  - `cancel()` 호출 시 `status=CANCELED`, `is_deleted=true`
  - `@SQLRestriction("is_deleted = false")`로 기본 조회에서 soft delete 제외
- 인덱스:
  - `idx_pers_sked_owner_start_at(owner_id, start_at)`
  - `idx_pers_sked_owner_end_at(owner_id, end_at)`

### 3.2 DealSchedule
- 테이블: `tbl_deal_sked`
- PK: `deal_sked_id` (`BaseModifyEntity.id` override)
- 주요 컬럼:
  - `deal_id` (SalesDeal FK), `client_id` (Client FK), `assignee_user_id` (User FK)
  - `title`(200), `description`(TEXT)
  - `start_at`, `end_at`
  - `event_type`, `doc_type`
  - `ref_doc_id`, `ref_deal_log_id`
  - `source`, `external_key`, `last_synced_at`
- 제약/인덱스:
  - UNIQUE: `uk_deal_sked_external_key(external_key)`
  - `idx_deal_sked_assignee_start_end(assignee_user_id, start_at, end_at)`
  - `idx_deal_sked_client_start_end(client_id, start_at, end_at)`
  - `idx_deal_sked_deal_start_end(deal_id, start_at, end_at)`
  - `idx_deal_sked_start_end(start_at, end_at)`

## 4. API

### 4.1 통합 조회
- `GET /api/v1/schedules`
- `GET /api/schedules`
- query params:
  - 필수: `from`, `to` (ISO LocalDateTime)
  - 선택: `assigneeUserId`, `clientId`, `dealId`
  - 선택: `includePersonal`(기본 true), `includeDeal`(기본 true)
- 응답: `ApiResult<List<ScheduleItemDto>>`
- 동작:
  - 개인 일정 + 거래 일정을 합쳐 `startAt ASC, id ASC` 정렬

### 4.2 개인 일정 CRUD
- `POST /api/v1/schedules/personal`, `POST /api/schedules/personal` -> 생성 ID 반환
- `GET /api/v1/schedules/personal/{id}`, `GET /api/schedules/personal/{id}` -> 본인 일정 단건
- `PUT /api/v1/schedules/personal/{id}`, `PUT /api/schedules/personal/{id}` -> 본인 일정 수정
- `DELETE /api/v1/schedules/personal/{id}`, `DELETE /api/schedules/personal/{id}` -> `cancel()` 수행

### 4.3 거래 일정 API 정책
- 거래 일정 전용 CRUD API는 제공하지 않음
- 조회는 통합 조회에서만 노출
- 생성/수정은 `DealScheduleSyncService.upsertFromEvent(...)`로만 수행

## 5. 서비스 규칙

### 5.1 PersonalScheduleCommandService
- actor는 `CustomUserDetails.userId` 기준
- owner 본인만 수정/삭제 가능
- 생성 시 owner는 actor user 조회 결과로 고정
- 삭제는 `status=CANCELED` + `isDeleted=true` 처리
- 검증:
  - request 시간 범위 방어 (`startAt < endAt`)
  - owner+status 조건으로 조회 (`CANCELED` 제외)
  - 생성/수정 시 `ScheduleStatus.DONE` 금지
  - 생성/수정 시 `ScheduleVisibility.TEAM` 금지
- 기본값:
  - 생성 시 `status` 미전달이면 `ACTIVE`
  - 생성 시 `visibility` 미전달이면 `PRIVATE`
  - 수정 시 `status/visibility` 미전달이면 기존 값 유지

### 5.2 ScheduleQueryService
- 공통 검증:
  - `condition.actorUserId` 필수
  - `includePersonal/includeDeal` 둘 다 false면 오류
  - `rangeStart < rangeEnd`
  - 요청 actor 실사용자 조회 후 role 일치 여부 검증
- PERSONAL 조회:
  - actor 본인 owner만 허용
  - `CANCELED` 제외
- DEAL 조회 권한:
  - `ADMIN`: assignee/client/deal 필터 조합 허용
  - `SALES_REP`: `assigneeUserId` 필터 금지, 본인 담당 거래처 범위만 허용
  - `CLIENT`: `assigneeUserId/clientId/dealId` 직접 지정 금지, 본인 client 범위만 조회
  - 그 외 role: `ACCESS_DENIED`

### 5.3 DealScheduleSyncService
- `externalKey` 기반 upsert
- 참조 엔티티 조회는 `DealScheduleReferenceReader`로 위임
- 저장 source는 항상 `ScheduleSource.AUTO_SYNC`
- 유니크 충돌 시(`DataIntegrityViolationException`) 재조회 후 update 재시도
- 검증:
  - command null 여부
  - 필수 필드/시간 범위
  - `deal.client`와 입력 `client` 일치 검증

## 6. DTO/검증
- `PersonalScheduleCreateRequest`
  - `title` 필수 + 최대 200
  - `startAt/endAt` 필수
  - `status/visibility`는 nullable
  - `@ValidTimeRange`로 `endAt > startAt`
- `PersonalScheduleUpdateRequest`
  - `title` 필수 + 최대 200
  - `startAt/endAt` 필수
  - `status/visibility`는 nullable(미전달 시 기존 값 유지)
  - `@ValidTimeRange` 적용
- `DealScheduleUpsertCommand`
  - canonical constructor에서 trim + 필수/양수/길이/시간 범위 검증

## 7. Repository 포인트
- 기간 겹침(overlap) 조건 사용:
  - `startAt < rangeEnd AND endAt > rangeStart`
- 모든 조회 메서드는 `startAt ASC, id ASC`
- N+1 완화:
  - `@EntityGraph`로 `owner` 또는 `assigneeUser/client/deal` preload
- SALES_REP 전용 조회:
  - `client.managerEmployeeId` 기준 조회 메서드 제공

## 8. 예외 처리
- 프로젝트 공통 `CoreException(ErrorType)` 사용
- 주요 오류 유형:
  - `UNAUTHORIZED`, `ACCESS_DENIED`, `INVALID_INPUT_VALUE`
  - `PERSONAL_SCHEDULE_NOT_FOUND`, `DEAL_NOT_FOUND`, `CLIENT_NOT_FOUND`, `USER_NOT_FOUND`
  - `EMPLOYEE_NOT_LINKED`
- 참고:
  - `DealSchedule` 엔티티 내부 validate는 `IllegalArgumentException`을 사용

## 9. 비고
- 통합 응답에서 PERSONAL은 `status`, `ownerUserId`, `allDay`를 채운다
- 통합 응답에서 DEAL은 `assigneeUserId`, `clientId`, `dealId`, `eventType`, `docType`를 채우고 `allDay=false`, `status=null`로 반환한다
