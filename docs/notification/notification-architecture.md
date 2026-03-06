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

## [2026-03-06] Notification Phase 3 이벤트 기반 알림 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/DealStatusChangedEvent.java
- 클래스/메서드: DealStatusChangedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalRequestedEvent.java
- 클래스/메서드: ApprovalRequestedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalCompletedEvent.java
- 클래스/메서드: ApprovalCompletedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalRejectedEvent.java
- 클래스/메서드: ApprovalRejectedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventPublisher.java
- 클래스/메서드: NotificationEventPublisher.publish
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DealApprovalNotificationService.java
- 클래스/메서드: DealApprovalNotificationService.createDealStatusChangedNotification, createApprovalRequestedNotification, createApprovalCompletedNotification, createApprovalRejectedNotification
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: NotificationEventHandler.handleDealStatusChanged, handleApprovalRequested, handleApprovalCompleted, handleApprovalRejected
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/service/DealPipelineFacade.java
- 클래스/메서드: DealPipelineFacade.recordAndSync, recordConvertAndSync
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.createApprovalRequest, decideStep
- 파일: src/main/java/com/monsoon/seedflowplus/domain/account/repository/UserRepository.java
- 클래스/메서드: UserRepository.findByClientId, findAllByRole
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationTargetType.java
- 클래스/메서드: NotificationTargetType
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java
- 클래스/메서드: NotificationSseService.connect, send, remove

### 변경 내용
Deal/Approval 알림을 동기 호출 대신 Spring ApplicationEvent 기반으로 분리하기 위해 이벤트 4종과 퍼블리셔를 추가했다.
이벤트 핸들러는 `@EventListener + @Async`로 저장을 위임하고, 저장 성공 시 SSE 서비스를 통해 사용자별 실시간 전송을 시도한다.
DealPipelineFacade와 ApprovalCommandService는 기존 로직 후단에서 이벤트만 발행하도록 연결해 직접 NotificationCommandService 의존을 피했다.
DealApprovalNotificationService는 사용자 row 락 + 당일 중복 체크 후 Notification/NotificationDelivery(IN_APP, scheduledAt=event 발생시각)를 생성한다.

### 변경 이유
AGENTS.md Phase 3(6~9번) 요구사항 반영

## [2026-03-06] Notification Phase 4 SSE 구독/워커 연동

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java
- 클래스/메서드: NotificationController.subscribe
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java
- 클래스/메서드: NotificationDeliveryWorkerService.dispatch

### 변경 내용
NotificationController에 `GET /api/v1/notifications/subscribe` 엔드포인트를 추가하고,
인증 principal에서 userId를 해석해 `NotificationSseService.connect(userId)`가 반환한 `SseEmitter`를 직접 응답하도록 구성했다.
NotificationDeliveryWorkerService는 IN_APP delivery 처리 시 `markSent` 직후
`NotificationSseService.send(userId, NotificationListItemResponse)`를 호출해 예약 알림도 즉시 SSE로 전달한다.

### 변경 이유
AGENTS.md Phase 4(10~12번) 요구사항 반영

## [2026-03-06] Notification Phase 5 테스트 추가

### 변경 대상
- 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationControllerTest.java
- 클래스/메서드: NotificationControllerTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandServiceTest.java
- 클래스/메서드: NotificationCommandServiceTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandlerTest.java
- 클래스/메서드: NotificationEventHandlerTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/scheduler/NotificationDeliverySchedulerTest.java
- 클래스/메서드: NotificationDeliverySchedulerTest
- 파일: src/test/java/com/monsoon/seedflowplus/domain/deal/log/service/DealPipelineFacadeTest.java
- 클래스/메서드: DealPipelineFacadeTest.setUp

### 변경 내용
NotificationController WebMvcTest를 추가해 기존 조회/읽음 API와 신규 삭제 API, SSE 구독 비동기 응답(`asyncDispatch`)을 검증했다.
NotificationCommandService 단위 테스트로 deleteOne/deleteAll, 소유 검증 실패 시 NOTIFICATION_NOT_FOUND를 고정했다.
NotificationEventHandler/NotificationDeliveryScheduler 테스트를 추가해 이벤트 위임 흐름과 스케줄러 호출 인자(now, cutoff-30일)를 검증했다.
DealPipelineFacade 테스트는 생성자 의존성 변경(NotificationEventPublisher, UserRepository) 반영을 위해 setup 초기화를 보정했다.

### 변경 이유
AGENTS.md Phase 5(13~16번) 요구사항 반영

## [2026-03-06] Notification 안정성 이슈 보완

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventPublisher.java
- 클래스/메서드: NotificationEventPublisher.publishAfterCommit
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.publishApprovalRequestedForFirstApprovers, publishApprovalEventsAfterDecision, now
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/service/DealPipelineFacade.java
- 클래스/메서드: DealPipelineFacade.publishDealStatusChangedEventIfNeeded
- 파일: src/main/java/com/monsoon/seedflowplus/core/config/AsyncConfig.java
- 클래스/메서드: AsyncConfig.notificationTaskExecutor
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: NotificationEventHandler.handleDealStatusChanged, handleApprovalRequested, handleApprovalCompleted, handleApprovalRejected
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java
- 클래스/메서드: NotificationSseService.connect
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java
- 클래스/메서드: NotificationDeliveryRepository.findTop100IdsForUpdateSkipLockedByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc, findAllWithNotificationAndUserByIdInOrderByScheduledAtAsc
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java
- 클래스/메서드: NotificationDeliveryWorkerService.loadDueDeliveriesWithAssociations

### 변경 내용
NotificationEventPublisher에 트랜잭션 활성 시 `afterCommit` 시점 발행 메서드를 추가하고, Approval/Deal 발행 호출을 해당 메서드로 교체했다.
ApprovalCommandService의 시간 생성을 `Clock` 주입 기반으로 통일해 직접 `LocalDateTime.now()` 호출을 제거했다.
NotificationEventHandler는 `@Async("notificationTaskExecutor")`를 사용하도록 명시하고, AsyncConfig에 알림 전용 executor를 추가했다.
SSE는 동일 user 재연결 시 기존 emitter를 complete 처리 후 교체하며, 워커는 SKIP LOCKED로 선점한 ID를 fetch join 재조회해 notification/user 지연로딩 N+1을 제거했다.

### 변경 이유
트랜잭션 일관성, 비동기 실행 안정성, Clock 정책 준수, SSE 연결 정리 및 배치 조회 성능 이슈 대응
