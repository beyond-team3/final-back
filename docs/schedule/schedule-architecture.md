## [2026-03-06] Schedule 엔티티 Phase 2 명세 정렬

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java
- 클래스/메서드: PersonalSchedule#cancel
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealSchedule.java
- 클래스/메서드: DealSchedule (@Table indexes)
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleStatus.java
- 클래스/메서드: ScheduleStatus
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleVisibility.java
- 클래스/메서드: ScheduleVisibility

### 변경 내용
PersonalSchedule에 soft delete 전용 `cancel()` 메서드를 추가해 상태 변경 책임을 엔티티에 명시했다.
DealSchedule 인덱스를 `assignee/client/deal + start_at + end_at` 조합으로 보강해 기간 겹침 조회에 맞췄다.
일정 상태 enum은 `ACTIVE/CANCELED`로, 공개 범위 enum은 `PRIVATE/PUBLIC`로 정렬했다.

### 변경 이유
Phase 2 정책(엔티티 필드/soft delete/인덱스 규칙) 일치

## [2026-03-06] Phase 3 패키지 정렬 및 개인 일정 삭제 도메인화

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/command/PersonalScheduleCommandService.java
- 클래스/메서드: PersonalScheduleCommandService#delete
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java
- 클래스/메서드: ScheduleController
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/PersonalScheduleCreateRequest.java
- 클래스/메서드: PersonalScheduleCreateRequest
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/PersonalScheduleUpdateRequest.java
- 클래스/메서드: PersonalScheduleUpdateRequest
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/validation/ValidTimeRange.java
- 클래스/메서드: ValidTimeRange
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/validation/ValidTimeRangeValidator.java
- 클래스/메서드: ValidTimeRangeValidator

### 변경 내용
`PersonalScheduleCommandService`를 `service` 패키지에서 `command` 패키지로 이동해 Phase 구조를 맞췄다.
`ValidTimeRange`/`Validator`를 `dto.validation`에서 `validation` 패키지로 이동하고 요청 DTO import를 정리했다.
개인 일정 삭제는 서비스가 직접 필드를 덮어쓰지 않고 `PersonalSchedule.cancel()`을 호출하도록 변경했다.
컨트롤러의 서비스 import도 새 패키지 경로로 동기화했다.

### 변경 이유
Phase 3 정책(패키지 구조 및 개인 일정 soft delete 도메인 메서드 사용) 일치
