## [2026-03-06] Notification Phase 2 기본 보완

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationType.java
- 클래스/메서드: NotificationType
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java
- 클래스/메서드: NotificationRepository.deleteByUser_Id, deleteByCreatedAtBefore
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java
- 클래스/메서드: NotificationDeliveryRepository.deleteByNotification_Id, deleteByNotification_User_Id, deleteByNotification_CreatedAtBefore
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandService.java
- 클래스/메서드: NotificationCommandService.deleteOne, deleteAll, deleteOlderThan
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java
- 클래스/메서드: NotificationController.deleteOne, deleteAll
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/scheduler/NotificationDeliveryScheduler.java
- 클래스/메서드: NotificationDeliveryScheduler.dispatchDueDeliveries, deleteExpiredNotifications

### 변경 내용
NotificationType에 Deal/Approval 알림 타입 4종을 추가했다.
알림 단건/전체 삭제를 위해 컨트롤러-서비스-리포지토리 삭제 경로를 확장했고, 소유 검증 실패 시 NOTIFICATION_NOT_FOUND를 유지했다.
스케줄러를 신규 추가해 fixedDelay 기반 배치 발송과 30일 초과 알림 정리(cron)를 분리 실행하도록 구성했다.
보존 정책 삭제는 NotificationDelivery 선삭제 후 Notification 삭제 순서로 처리해 연관 데이터 정합성을 유지했다.

### 변경 이유
AGENTS.md Phase 2(3~5번) 요구사항 반영
