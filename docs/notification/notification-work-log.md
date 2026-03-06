## [2026-03-06 16:59] Notification Phase 2 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationType.java — Deal/Approval 알림 타입 4종 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java — 사용자 전체 삭제/보존 정책 삭제용 메서드 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java — 단건/전체/보존 정책 연관 delivery 삭제 메서드 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandService.java — deleteOne/deleteAll/deleteOlderThan 구현
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java — DELETE /{notificationId}, DELETE / 엔드포인트 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/scheduler/NotificationDeliveryScheduler.java — 발송 스케줄/30일 보존정리 스케줄 신규 추가
- 수정 파일: docs/notification/notification-architecture.md — 구조 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 3 (Deal/Approval 이벤트 알림) 진행
