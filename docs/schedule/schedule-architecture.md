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
