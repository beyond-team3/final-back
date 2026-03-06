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
