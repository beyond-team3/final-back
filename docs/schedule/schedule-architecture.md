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

## [2026-03-06] Phase 4 통합 조회 계층 패키지 정렬

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/service/ScheduleQueryService.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryService.java
- 클래스/메서드: ScheduleQueryService
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java
- 클래스/메서드: ScheduleController#getSchedules

### 변경 내용
통합 조회 서비스 `ScheduleQueryService`를 `service` 패키지에서 `query` 패키지로 이동해
CQRS 성격의 조회 계층 구조를 명시했다.
컨트롤러는 새 패키지 경로를 참조하도록 import를 동기화했고,
기존 `GET /api/schedules` 동작 및 조건 생성 로직은 유지했다.

### 변경 이유
Phase 4 정책(통합 조회 서비스의 query 계층 분리) 일치

## [2026-03-06] Phase 5 거래 일정 동기화 패키지 정렬

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/request/DealScheduleUpsertCommand.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/command/DealScheduleUpsertCommand.java
- 클래스/메서드: DealScheduleUpsertCommand
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/service/DealScheduleSyncService.java → src/main/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncService.java
- 클래스/메서드: DealScheduleSyncService#upsertFromEvent

### 변경 내용
`DealScheduleUpsertCommand`를 `dto/request`에서 `dto/command`로 이동해 명령 모델 경계를 명확히 했다.
`DealScheduleSyncService`를 `service`에서 `sync` 패키지로 이동하고 import 경로를 동기화했다.
`upsertFromEvent(...)`의 externalKey 기반 upsert와 `DataIntegrityViolationException` 단일 재시도 구조는 기존 구현을 유지했다.

### 변경 이유
Phase 5 정책(동기화 전용 계층/명령 DTO 패키지 분리) 일치

## [2026-03-06] Phase 6 일정 도메인 테스트 추가

### 변경 대상
- 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleControllerTest.java
- 클래스/메서드: ScheduleControllerTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/command/PersonalScheduleCommandServiceTest.java
- 클래스/메서드: PersonalScheduleCommandServiceTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryServiceTest.java
- 클래스/메서드: ScheduleQueryServiceTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncServiceTest.java
- 클래스/메서드: DealScheduleSyncServiceTest

### 변경 내용
`@WebMvcTest + TestSecurityConfig` 기반 컨트롤러 테스트로 개인 일정 CRUD와 통합 조회(ADMIN/SALES_REP/CLIENT) 요청 바인딩을 검증했다.
개인 일정 명령 서비스 테스트에 create/update/delete 흐름과 `USER_NOT_FOUND`, `PERSONAL_SCHEDULE_NOT_FOUND`, 시간 검증 예외를 추가했다.
통합 조회 서비스 테스트에 역할별 접근 제어(`ACCESS_DENIED`, `EMPLOYEE_NOT_LINKED`) 및 include flag 검증을 추가했다.
거래 일정 동기화 테스트에 신규 생성, externalKey update, `DataIntegrityViolationException` 재시도, deal-client 불일치 검증을 추가했다.

### 변경 이유
Phase 6 정책(일정 도메인 테스트 보강 및 역할 기반 회귀 방지) 반영

## [2026-03-06] Schedule 예외 정책 및 API 경로 정렬

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/dto/command/DealScheduleUpsertCommand.java
- 클래스/메서드: DealScheduleUpsertCommand (canonical constructor)
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealSchedule.java
- 클래스/메서드: DealSchedule#validate
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java
- 클래스/메서드: PersonalSchedule#validate
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java
- 클래스/메서드: ScheduleController#createPersonalSchedule, ScheduleController(@RequestMapping)

### 변경 내용
일정 도메인 명령/엔티티 내부 검증에서 `IllegalArgumentException` 대신
`CoreException(ErrorType.INVALID_INPUT_VALUE)`를 사용하도록 통일했다.
개인 일정 생성 API는 `@ResponseStatus(HttpStatus.CREATED)`를 적용했고,
컨트롤러 base path를 프로젝트 공통 정책에 맞춰 `/api/v1/schedules`로 정렬했다.

### 변경 이유
예외 처리/REST 경로 정책 일관성 확보

## [2026-03-06] Schedule 하위호환성 및 선검증 보강

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleStatus.java
- 클래스/메서드: ScheduleStatus
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/ScheduleVisibility.java
- 클래스/메서드: ScheduleVisibility
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/PersonalSchedule.java
- 클래스/메서드: PersonalSchedule#validate
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/controller/ScheduleController.java
- 클래스/메서드: ScheduleController(@RequestMapping)

### 변경 내용
기존 DB 문자열 로딩 호환을 위해 일정 상태 enum에 `DONE`, 공개 범위 enum에 `TEAM` 레거시 값을 `@Deprecated`로 복구했다.
개인 일정 엔티티 검증에 `title.trim().length() > 200` 선검증을 추가해 DB 제약 위반 전에 `INVALID_INPUT_VALUE`를 반환하도록 정렬했다.
컨트롤러 base path는 기존 `/api/v1/schedules`를 유지하면서 `/api/schedules` alias를 추가해 구 클라이언트 요청도 수용하도록 확장했다.

### 변경 이유
운영 데이터/클라이언트 하위호환 및 표준 예외 정책 준수
