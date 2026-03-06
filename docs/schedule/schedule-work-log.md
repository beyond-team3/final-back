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
