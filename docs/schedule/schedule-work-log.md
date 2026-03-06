## [2026-03-06 22:40] Phase 2 엔티티/리포지토리 기준 정렬

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java — soft delete 전용 `cancel()` 메서드 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealSchedule.java — assignee/client/deal 기준 start-end 복합 인덱스로 보강
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleStatus.java — 상태를 `ACTIVE/CANCELED`로 정렬
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleVisibility.java — 공개 범위를 `PRIVATE/PUBLIC`로 정렬
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 3 개인 일정 명령 API 구현

## [2026-03-06 17:28] Phase 3 개인 일정 명령 계층 정렬

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/service/PersonalScheduleCommandService.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/command/PersonalScheduleCommandService.java — command 패키지로 이동, delete에서 `schedule.cancel()` 사용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/validation/ValidTimeRange.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/validation/ValidTimeRange.java — validation 패키지로 이동
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/validation/ValidTimeRangeValidator.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/validation/ValidTimeRangeValidator.java — validation 패키지로 이동
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/PersonalScheduleCreateRequest.java — `ValidTimeRange` import 경로 정리
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/PersonalScheduleUpdateRequest.java — `ValidTimeRange` import 경로 정리
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java — `PersonalScheduleCommandService` import 경로 정리
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 4 통합 조회 API 정합성 점검 및 보정

## [2026-03-06 18:07] Phase 4 통합 조회 서비스 패키지 정렬

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/service/ScheduleQueryService.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryService.java — query 패키지로 이동
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java — `ScheduleQueryService` import 경로 동기화
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 5 거래 일정 동기화 로직 정합성 점검 및 보정

## [2026-03-06 18:11] Phase 5 거래 일정 동기화 계층 정렬

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/DealScheduleUpsertCommand.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/command/DealScheduleUpsertCommand.java — 명령 DTO 패키지 이동
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/service/DealScheduleSyncService.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncService.java — sync 패키지 이동 및 command import 경로 정리
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 6 테스트 구현

## [2026-03-06 18:22] Phase 6 일정 도메인 테스트 구현

### 작업 내용
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleControllerTest.java — 개인 일정 CRUD 및 통합 조회(ADMIN/SALES_REP/CLIENT) WebMvc 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/command/PersonalScheduleCommandServiceTest.java — create/update/delete 및 USER_NOT_FOUND, PERSONAL_SCHEDULE_NOT_FOUND, 시간 검증 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryServiceTest.java — 역할별 접근 제어, include flag, ACCESS_DENIED, EMPLOYEE_NOT_LINKED 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncServiceTest.java — 신규/upsert/충돌 재시도/deal-client 불일치 테스트 추가
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일/테스트 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 18:33] 일정 예외 정책 및 API prefix 이슈 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/command/DealScheduleUpsertCommand.java — canonical constructor 예외를 `CoreException(INVALID_INPUT_VALUE)`로 통일
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealSchedule.java — 엔티티 validate 예외를 `CoreException(INVALID_INPUT_VALUE)`로 통일
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java — 엔티티 validate 예외를 `CoreException(INVALID_INPUT_VALUE)`로 통일
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/PersonalScheduleCreateRequest.java — `status/visibility`를 nullable로 조정해 서비스 기본값 분기와 정책 일치
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java — 생성 API 201 적용 및 base path `/api/v1/schedules`로 정렬
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleControllerTest.java — 컨트롤러 경로/생성 상태코드 기대값 정렬
- 수정 파일: docs/schedule/schedule-architecture.md — 구조/정책 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일/테스트 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 18:46] 일정 하위호환성 및 title 선검증 이슈 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleStatus.java — `DONE` 레거시 enum 값을 `@Deprecated`로 복구해 기존 DB 문자열 로딩 호환성 보강
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleVisibility.java — `TEAM` 레거시 enum 값을 `@Deprecated`로 복구해 기존 DB 문자열 로딩 호환성 보강
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java — `validate()`에 제목 길이(최대 200자) 선검증 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java — `/api/schedules` 구경로 alias 추가(기존 `/api/v1/schedules` 유지)
- 수정 파일: docs/schedule/schedule-architecture.md — 구조/호환성 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 19:16] PersonalSchedule soft delete 정책/PK 컬럼명 이슈 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java — `@AttributeOverride`를 `personal_schedule_id`로 정렬, `@SQLDelete/@SQLRestriction` 및 `is_deleted` 필드 추가
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 19:31] Phase 2 DTO/검증 리뷰 이슈 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/response/ScheduleItemDto.java — 거래 일정 응답에서 `externalKey`, `refDocId`, `refDealLogId` 제거
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/command/DealScheduleUpsertCommand.java — canonical constructor에 `title`/`externalKey` 길이·공백 선검증 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/ScheduleSearchCondition.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleSearchCondition.java — 패키지/경로 정렬
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java — ScheduleSearchCondition import 경로 동기화
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryService.java — ScheduleSearchCondition import 제거(동일 패키지)
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleControllerTest.java — ScheduleSearchCondition import 경로 동기화
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryServiceTest.java — ScheduleSearchCondition import 제거(동일 패키지)
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 검증 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 19:48] DealScheduleSyncService 도메인 의존 경계 분리

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncService.java — 타 도메인 Repository 직접 주입 제거, 조회 포트 의존으로 교체
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleReferenceReader.java — deal/client/assignee 조회 포트 인터페이스 및 결과 record 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleReferenceReaderImpl.java — 기존 repository 조회/예외 매핑을 구현체로 캡슐화
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncServiceTest.java — 포트 mock 기반으로 테스트 의존 구조 정렬
- 수정 파일: docs/schedule/schedule-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 검증 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-07 01:22] Phase 6 테스트 커버리지 누락 보강

### 작업 내용
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleControllerTest.java — `@ValidTimeRange` 경계값(`endAt == startAt`)에 대한 POST/PUT 400 검증 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/command/PersonalScheduleCommandServiceTest.java — `PERSONAL_SCHEDULE_NOT_FOUND` 예외를 get/update/delete 경로별 독립 테스트로 분리
- 수정 파일: docs/schedule/schedule-architecture.md — 테스트 보강 이력 기록 추가
- 수정 파일: docs/schedule/schedule-work-log.md — 작업 및 검증 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
