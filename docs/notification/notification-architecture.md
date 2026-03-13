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

## [2026-03-06] Notification 예외/재연결 안정성 보완

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java
- 클래스/메서드: NotificationSseService.connect, send, remove
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/CultivationNotificationService.java
- 클래스/메서드: CultivationNotificationService.lockUser
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java
- 클래스/메서드: NotificationController.resolveUserId

### 변경 내용
SSE 연결 교체 시 기존 emitter의 completion 콜백이 신규 emitter를 제거하지 않도록 `emitters.remove(userId, emitter)` compare-remove 방식으로 정리 로직을 변경했다.
SSE send 실패 시에도 동일 compare-remove를 사용해 현재 연결과 실패 연결을 구분해 제거한다.
재배적기 알림 사용자 잠금 조회 실패는 `NullPointerException` 대신 `CoreException(ErrorType.USER_NOT_FOUND)`로 통일했다.
컨트롤러 인증 경계에서 `principal`뿐 아니라 `principal.userId` null도 `UNAUTHORIZED`로 정규화한다.

### 변경 이유
운영 장애 가능성이 있는 SSE 재연결 제거 경쟁 조건과 도메인 예외/인증 정책 불일치 이슈를 수정하기 위함

## [2026-03-06] Notification 결함 4건 정합성 수정

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/Notification.java
- 클래스/메서드: Notification
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationDelivery.java
- 클래스/메서드: NotificationDelivery.markFailed
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java
- 클래스/메서드: NotificationRepository.markAllAsRead
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java
- 클래스/메서드: NotificationDeliveryRepository.findTop100IdsForUpdateSkipLockedByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java
- 클래스/메서드: NotificationDeliveryWorkerService.loadDueDeliveriesWithAssociations

### 변경 내용
Notification 엔티티에 `@SQLDelete/@SQLRestriction`과 `is_deleted` 매핑을 추가해 삭제 시 소프트 삭제 정책이 적용되도록 정합성을 맞췄다.
NotificationDelivery의 `markFailed` 내부에서 실패 사유를 trim/blank fallback/500자 제한으로 정규화해 엔티티 불변식으로 캡슐화했다.
`markAllAsRead`에 저장소 트랜잭션을 명시해 리포지토리 단독 호출 상황에서도 트랜잭션 누락 위험을 제거했다.
SKIP LOCKED 쿼리에 MariaDB 10.6+ 전제 조건을 명시하고, 미지원/실패 시 워커가 비락 조회로 fallback 하도록 보강했다.

### 변경 이유
리뷰에서 식별된 Critical/Major/Minor 결함([1]~[4])을 기존 비즈니스 흐름 변경 없이 정확히 보완하기 위함

## [2026-03-06] Notification 트랜잭션 기본 정책 정렬

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandService.java
- 클래스/메서드: NotificationCommandService, markAsRead, markAllAsRead, deleteOne, deleteAll, deleteOlderThan, createCultivationSowingPromotion, createCultivationHarvestFeedback
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/CultivationNotificationService.java
- 클래스/메서드: CultivationNotificationService, createSowingPromotionNotification, createHarvestFeedbackNotification

### 변경 내용
두 서비스의 클래스 레벨 트랜잭션을 `@Transactional(readOnly = true)`로 변경했다.
데이터 변경이 발생하는 공개 메서드에만 `@Transactional`을 재선언해 쓰기 경계를 명시했다.
비즈니스 로직과 입출력 계약은 유지하고 트랜잭션 선언 정책만 정렬했다.

### 변경 이유
프로젝트 트랜잭션 컨벤션(클래스 기본 readOnly, 쓰기 메서드 재선언) 준수

## [2026-03-06] Notification afterCommit 발행 예외 격리

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventPublisher.java
- 클래스/메서드: NotificationEventPublisher.publishAfterCommit, publish

### 변경 내용
`afterCommit` 콜백 내부 이벤트 발행을 try-catch로 감싸 예외를 로깅하고 전파하지 않도록 변경했다.
즉시 발행 메서드 `publish(Object)`의 접근 범위를 package-private로 축소해 외부 정책 우회 가능성을 낮췄다.
기존 `publishAfterCommit` 동작 경로와 이벤트 payload 계약은 유지했다.

### 변경 이유
after-commit 예외가 API 실패로 오인되지 않도록 소스 트랜잭션 결과와 이벤트 발행 실패를 분리하고, 발행 정책 우회를 방지하기 위함

## [2026-03-07] Notification SSE remove 오용 경계 차단

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java
- 클래스/메서드: NotificationSseService.connect, send, removeIfMatch

### 변경 내용
SSE emitter 정리 메서드를 compare-remove 전용 `removeIfMatch(userId, emitter)`로 통일했다.
key-only 삭제를 수행하던 `remove(userId)` 공개 메서드를 제거해 stale 컨텍스트에서 신규 emitter 오삭제가 발생할 경로를 차단했다.
completion/timeout/error 콜백과 send 실패 경로 모두 동일한 compare-remove 메서드를 사용하도록 정리했다.

### 변경 이유
Phase 4 SSE 동시성 리뷰 지적사항(잘못된 emitter 삭제 경쟁 조건) 보완

## [2026-03-07] Notification fallback 조회 N+1 위험 제거

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java
- 클래스/메서드: NotificationDeliveryWorkerService.loadDueDeliveriesWithAssociations

### 변경 내용
SKIP LOCKED 실패 fallback에서 `NotificationDelivery` 엔티티를 즉시 반환하던 경로를 제거했다.
fallback에서도 먼저 due delivery ID 목록을 확보한 뒤, 공통 경로인 `findAllWithNotificationAndUserByIdInOrderByScheduledAtAsc`로 재조회하도록 정렬했다.
이를 통해 fallback 처리 시에도 `notification/user` 연관을 fetch join으로 일괄 로딩해 배치 루프의 지연 로딩 N+1 위험을 없앴다.

### 변경 이유
리뷰 이슈 [Major] fallback fetch join 누락 보완

## [2026-03-07] Notification SSE subscribe content-type 명시

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java
- 클래스/메서드: NotificationController.subscribe

### 변경 내용
SSE 구독 엔드포인트 매핑을 `@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)`로 변경했다.
응답 본문/인증/서비스 호출 흐름은 유지하고, 콘텐츠 협상 시 `text/event-stream`이 명시되도록 보강했다.

### 변경 이유
Phase 6 리뷰 이슈 [Major] SSE 구독 `produces` 미선언 보완

## [2026-03-07] Approval 테스트 의존성 주입 보강

### 변경 대상
- 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java
- 클래스/메서드: ApprovalCommandServiceTest.setUp

### 변경 내용
ApprovalCommandService의 신규 의존성(`Clock`, `UserRepository`, `NotificationEventPublisher`)이 테스트에서 누락되어 `NullPointerException(clock)`가 발생하던 문제를 수정했다.
테스트에 누락된 mock 필드를 추가하고, `setUp`에서 시간/사용자 조회 기본 스텁을 설정해 이벤트 발행 경로까지 안전하게 실행되도록 보완했다.

### 변경 이유
서비스 생성자 의존성 확장 이후 단위 테스트 주입 구성이 최신 구조를 반영하지 못해 Jenkins 빌드가 실패하던 이슈를 수정하기 위함

## [2026-03-07] Notification 발송 정합성/보존 정책 결함 수정

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java
- 클래스/메서드: NotificationSseService.send
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java
- 클래스/메서드: NotificationDeliveryWorkerService.dispatch, loadDueDeliveriesWithAssociations
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: NotificationEventHandler.sendIfPresent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DealApprovalNotificationService.java
- 클래스/메서드: createDealStatusChangedNotification, createIfNotDuplicated, isDuplicatedToday
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java
- 클래스/메서드: markAllAsRead, deleteByUser_Id, deleteByCreatedAtBefore, existsBy...CreatedAtGreaterThanEqualAndCreatedAtLessThan
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java
- 클래스/메서드: deleteByNotification_Id, deleteByNotification_User_Id, deleteByNotification_CreatedAtBefore
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/scheduler/NotificationDeliveryScheduler.java
- 클래스/메서드: deleteExpiredNotifications

### 변경 내용
SSE send 결과를 boolean으로 반환해 전송 예외(IO/IllegalState) 시 워커가 실패 처리할 수 있게 했고, 워커는 send 성공 시에만 markSent 하도록 조정했다.
SKIP LOCKED 조회 실패 fallback은 비원자 non-locking 선점 경로를 중단하도록 빈 목록 반환으로 변경해 중복 발송 가능성을 차단했다.
이벤트 핸들러의 SSE 전송은 트랜잭션 afterCommit 콜백에서 수행되도록 이동했고, Deal 상태 변경 dedup은 전이별 content 키를 포함해 당일 전이 누락을 방지했다.
알림/딜리버리 벌크 삭제는 물리 DELETE 대신 is_deleted=true UPDATE로 교체하고, retention day는 프로퍼티(`notification.retention-days`) 주입 방식으로 전환했다.

### 변경 이유
리뷰 이슈 [1]~[9] 중 수정 제외 항목을 제외한 발송 원자성/중복 전송/소프트삭제 우회/설정 하드코딩 결함을 수정하기 위함

## [2026-03-12] Approval 알림 문맥 payload 확장

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalRequestedEvent.java
- 클래스/메서드: ApprovalRequestedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalCompletedEvent.java
- 클래스/메서드: ApprovalCompletedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalRejectedEvent.java
- 클래스/메서드: ApprovalRejectedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalSubmissionService.java
- 클래스/메서드: ApprovalSubmissionService.publishApprovalRequestedForFirstApprovers
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.publishApprovalRequestedForFirstApprovers, publishApprovalEventsAfterDecision
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DealApprovalNotificationService.java
- 클래스/메서드: DealApprovalNotificationService.createApprovalRequestedNotification, createApprovalCompletedNotification, createApprovalRejectedNotification

### 변경 내용
승인 이벤트 3종 record에 `targetCode`, `actorType`를 추가해 문서 코드와 승인 단계 액터를 after-commit 시점까지 그대로 전달하도록 바꿨다.
ApprovalSubmissionService/ApprovalCommandService는 이벤트 발행 시 `ApprovalRequest.targetCodeSnapshot`과 현재/다음 step의 `actorType`을 함께 실어 보낸다.
DealApprovalNotificationService는 알림 `type`은 generic 3종을 유지하면서도 `targetType/targetId`를 실제 문서(`QUOTATION`, `CONTRACT`, `ORDER`)로 저장하고, title/content를 문서 종류+승인 단계 문맥으로 생성한다.
승인 완료/반려 알림 dedup은 stage별 content 키를 사용해 동일 문서에서 관리자/거래처 처리 알림이 같은 날 연속 발생해도 서로 누락되지 않게 했다.

### 변경 이유
Phase 1 정책 1번(승인 알림 generic 유지 + 문서/액터 문맥 노출) 반영

## [2026-03-12] 견적요청서 생성 알림 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/QuotationRequestCreatedEvent.java
- 클래스/메서드: QuotationRequestCreatedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DocumentNotificationService.java
- 클래스/메서드: DocumentNotificationService.createQuotationRequestCreatedNotification
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: NotificationEventHandler.handleQuotationRequestCreated
- 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/request/service/QuotationRequestService.java
- 클래스/메서드: QuotationRequestService.createQuotationRequest

### 변경 내용
견적요청서 생성 전용 이벤트 `QuotationRequestCreatedEvent`와 문서 알림 서비스 `DocumentNotificationService`를 추가했다.
QuotationRequestService는 저장/코드 발급/딜 로그 기록 후 `client.managerEmployee -> UserRepository.findByEmployeeId(...)` 경로로 담당 영업사원 사용자 1명을 해석해 after-commit 이벤트를 발행한다.
NotificationEventHandler는 신규 이벤트를 비동기 수신해 `QUOTATION_REQUEST_CREATED` 알림을 저장하고, 기존 SSE payload 형식(`NotificationListItemResponse`)으로 즉시 전송한다.
알림은 `targetType=QUOTATION_REQUEST`, `targetId=rfqId`로 저장되어 기존 목록/SSE 응답 구조를 변경하지 않는다.

### 변경 이유
Phase 1 정책 4번(견적요청서 생성 알림 수신 대상은 담당 영업사원 1명) 반영

## [2026-03-12] 문서 라이프사이클 알림 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationType.java
- 클래스/메서드: NotificationType
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ContractCompletedEvent.java
- 클래스/메서드: ContractCompletedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/StatementIssuedEvent.java
- 클래스/메서드: StatementIssuedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/InvoiceIssuedEvent.java
- 클래스/메서드: InvoiceIssuedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DocumentNotificationService.java
- 클래스/메서드: createContractCompletedNotification, createStatementIssuedNotification, createInvoiceIssuedNotification
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: handleContractCompleted, handleStatementIssued, handleInvoiceIssued
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: publishContractCompletedEventsIfNeeded
- 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java
- 클래스/메서드: createAndRecordStatement, publishStatementIssuedNotifications
- 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java
- 클래스/메서드: publishInvoice, publishInvoiceIssuedNotification

### 변경 내용
계약 체결 알림을 승인 처리 알림과 분리하기 위해 `NotificationType.CONTRACT_COMPLETED`와 이벤트 3종(contract/statment/invoice)을 추가했다.
ApprovalCommandService는 계약서 최종 승인 완료 후 영업사원, 전체 관리자, 거래처 사용자에게 `ContractCompletedEvent`를 after-commit 발행한다.
StatementService는 주문 확정 직후 자동 생성된 명세서에 대해 영업사원/거래처 양쪽으로 `StatementIssuedEvent`를 발행하고, InvoiceService는 publish 시 거래처 대상 `InvoiceIssuedEvent`를 발행한다.
NotificationEventHandler와 DocumentNotificationService는 이 이벤트들을 기존 Notification 저장/SSE 전송 흐름에 연결해 API 응답 포맷과 SSE payload 형식을 그대로 유지한다.

### 변경 이유
Phase 1 정책 2~3번(계약 체결 알림 분리, 명세서 즉시 발송, 청구서 발행 알림) 반영

## [2026-03-13] 계약 예약 알림 및 visible query 정렬

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationType.java
- 클래스/메서드: NotificationType
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationService.java
- 클래스/메서드: ScheduledNotificationService.scheduleContractLifecycleNotifications
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ContractApprovalSchedulesSyncEventHandler.java
- 클래스/메서드: ContractApprovalSchedulesSyncEventHandler.syncContractSchedules
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java
- 클래스/메서드: findVisibleByUserIdOrderByCreatedAtDesc, countVisibleUnreadByUserId
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/query/NotificationQueryService.java
- 클래스/메서드: getMyNotifications, getUnreadCount

### 변경 내용
계약 최종 승인 후 `ScheduledNotificationService`가 계약 시작/종료 30일 전/종료 알림을 `NotificationDelivery(status=PENDING, scheduledAt=해당 일자 09:00)`로 미리 생성하도록 추가했다.
이 예약 생성은 기존 `ContractApprovalSchedulesSyncEventHandler`의 after-commit 비동기 후처리 경로에 연결되어 계약 스케줄 upsert와 같은 트랜잭션 경계를 공유한다.
알림 목록/미읽음 집계는 이제 `NotificationDelivery.status = SENT`인 알림만 노출하도록 저장소 쿼리를 교체했다.
이 변경으로 미래 예약 알림이 미리 목록에 노출되지 않으며, 기존 재배 알림의 예약 발송 정책과도 정렬된다.

### 변경 이유
Phase 2 정책 1번, 4번(계약 종료 30일 전, 오전 9시 예약 발송) 반영

## [2026-03-13] 계정 활성화 알림 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/AccountActivatedEvent.java
- 클래스/메서드: AccountActivatedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/account/service/AccountService.java
- 클래스/메서드: AccountService.updateUserStatus
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DocumentNotificationService.java
- 클래스/메서드: createAccountActivatedNotification
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: handleAccountActivated

### 변경 내용
`AccountService.updateUserStatus`는 상태 변경 전 값을 비교해 `DEACTIVATE -> ACTIVATE` 전이일 때만 `AccountActivatedEvent`를 after-commit 발행하도록 변경했다.
`createAccount`는 여전히 기본 상태를 `DEACTIVATE`로 저장하므로 최초 등록 직후 활성화 알림은 발생하지 않는다.
DocumentNotificationService는 `ACCOUNT_ACTIVATED` 알림을 `targetType=ACCOUNT`, `targetId=userId`로 즉시 저장하고, NotificationEventHandler는 기존 SSE payload 형식으로 실시간 전송한다.

### 변경 이유
Phase 2 정책 3번(활성화 전이만 발송, 최초 등록 직후 제외) 반영

## [2026-03-13] 상품 등록 알림 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ProductCreatedEvent.java
- 클래스/메서드: ProductCreatedEvent
- 파일: src/main/java/com/monsoon/seedflowplus/domain/product/service/ProductWriteService.java
- 클래스/메서드: ProductWriteService.createProduct
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DocumentNotificationService.java
- 클래스/메서드: createProductCreatedNotification
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java
- 클래스/메서드: handleProductCreated

### 변경 내용
ProductWriteService는 상품 저장 완료 후 `userRepository.findAllByRole(SALES_REP)`로 전체 영업사원 사용자 목록을 조회해 `ProductCreatedEvent`를 after-commit 발행한다.
DocumentNotificationService는 이를 `PRODUCT_CREATED` 타입, `targetType=PRODUCT`, `targetId=productId` 기준 즉시 알림으로 저장한다.
NotificationEventHandler는 신규 상품 알림도 기존 SSE payload(`NotificationListItemResponse`) 형식을 그대로 사용해 실시간 전송한다.

### 변경 이유
Phase 2 정책 2번(상품 등록 알림은 모든 영업사원 수신) 반영

## [2026-03-13] 예약 알림 visible 경계 정합성 수정

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/ScheduledNotificationService.java
- 클래스/메서드: ScheduledNotificationService.isDuplicated
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java
- 클래스/메서드: existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt, deleteVisibleByNotification_User_Id
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java
- 클래스/메서드: findVisibleByUserIdOrderByCreatedAtDesc, countVisibleUnreadByUserId, findVisibleByIdAndUserId, markAllVisibleAsRead, deleteVisibleByUser_Id
- 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandService.java
- 클래스/메서드: markAsRead, markAllAsRead, deleteOne, deleteAll

### 변경 내용
계약 예약 알림 dedup 기준을 `createdAt` 날짜 비교에서 `NotificationDelivery.scheduledAt` 기준 존재 검사로 변경해 같은 계약/사용자/예약 시각 알림이 재생성되지 않도록 수정했다.
visible 목록/미읽음 쿼리는 `JOIN` 대신 `EXISTS`를 사용하도록 바꿔 다중 delivery 채널이 생겨도 중복 row와 count 부풀림이 발생하지 않게 했다.
단건/전체 읽음 및 삭제는 이제 visible 알림(`DeliveryStatus.SENT`)만 대상으로 동작하며, 미래 예약 알림은 사전 읽음/삭제되지 않는다.

### 변경 이유
리뷰 이슈 반영: 예약 dedup 오류, 숨은 예약 알림 bulk 처리, visible query 중복 row 위험 수정
