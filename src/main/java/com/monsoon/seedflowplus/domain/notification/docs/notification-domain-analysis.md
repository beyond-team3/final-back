# Notification Domain Analysis

## 1) 작업 요약 (현재 상태)
notification 도메인은 CQRS-lite 형태로 분리되어 있습니다.

- `command` 패키지: 상태 변경/쓰기 로직
- `query` 패키지: 조회 전용 로직
- `service` 패키지: `CultivationNotificationService`만 유지 (알림 생성 유스케이스)
- `repository`/`entity`: 기존 위치 유지

핵심 변경 사항:
- `NotificationRepository` 조회/카운트/소유권 조회/벌크 읽음 처리 메서드 확장
- `NotificationDeliveryRepository` 전송 대상 배치 조회 메서드 확장
- `NotificationCommandService`/`NotificationQueryService`/`NotificationDeliveryWorkerService`를 각각 command/query로 분리
- `CultivationNotificationService`는 유지하며, `NotificationCommandService`가 위임 호출
- 기존 래퍼(`service.NotificationCommandService`, `service.NotificationQueryService`, `service.NotificationDeliveryWorkerService`)는 제거됨

## 2) 현재 패키지 구조

```text
com.monsoon.seedflowplus.domain.notification
├─ command
│  ├─ NotificationCommandService.java
│  └─ NotificationDeliveryWorkerService.java
├─ query
│  └─ NotificationQueryService.java
├─ service
│  └─ CultivationNotificationService.java
├─ repository
│  ├─ NotificationRepository.java
│  └─ NotificationDeliveryRepository.java
├─ entity
│  ├─ Notification.java
│  ├─ NotificationDelivery.java
│  ├─ NotificationType.java
│  ├─ NotificationTargetType.java
│  ├─ DeliveryChannel.java
│  └─ DeliveryStatus.java
└─ docs
   └─ notification-domain-analysis.md
```

## 3) 서비스 책임 분리

### command.NotificationCommandService
쓰기/상태 변경 담당.

- `markAsRead(...)`: 사용자 소유 알림 단건 읽음 처리
- `markAllAsRead(...)`: 사용자 미읽음 전체 벌크 읽음 처리
- `createCultivationSowingPromotion(...)`: `CultivationNotificationService.createSowingPromotionNotification(...)`로 위임
- `createCultivationHarvestFeedback(...)`: `CultivationNotificationService.createHarvestFeedbackNotification(...)`로 위임

### query.NotificationQueryService
조회 전용 담당 (`@Transactional(readOnly = true)`).

- `getMyNotifications(...)`: 사용자 알림 페이지 조회 (최신순)
- `getUnreadCount(...)`: 사용자 미읽음 개수 조회

### command.NotificationDeliveryWorkerService
전송 워커 담당.

- `dispatchDueDeliveries(now)`:
  - `PENDING && scheduledAt <= now` 100건 조회
  - `markAttempt(now)` 수행
  - `IN_APP` 채널은 즉시 `markSent(now, null)` 처리
  - 예외 시 `markFailed(now, reason)` 처리
  - 처리 건수 반환

### service.CultivationNotificationService (유지)
재배적기 알림 생성 유스케이스 구현.

- `createSowingPromotionNotification(...)`
  - 중복 체크: 같은 유저/타입/타겟/타겟ID 기준, 오늘 00:00~내일 00:00
  - Notification 생성 후 Delivery(IN_APP, PENDING, scheduledAt) 생성
  - scheduledAt: `sowingStart - 1 month`, 09:00
- `createHarvestFeedbackNotification(...)`
  - 정책 동일
  - scheduledAt: `harvestingStart` 당일 09:00

## 4) Repository 확장 상태

### NotificationRepository
- `existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtBetween(...)`
- `findByUser_IdOrderByCreatedAtDesc(...)`
- `countByUser_IdAndReadAtIsNull(...)`
- `findByIdAndUser_Id(...)`
- `markAllAsRead(userId, now)` (`@Modifying` JPQL bulk update)

### NotificationDeliveryRepository
- `findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(...)`
- `findTop100ByChannelAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(...)`

## 5) 동작 관점 체크

- CQRS-lite 의존 방향 준수:
  - query -> command 의존 없음
  - command -> query 의존 없음
- 핵심 위임 관계 유지:
  - `NotificationCommandService` -> `CultivationNotificationService`
- 트랜잭션 의도 유지:
  - query는 readOnly
  - command/worker/cultivation은 쓰기 트랜잭션

## 6) 남은 기술 부채 / TODO

- `NotificationDeliveryWorkerService`에 동시성 제어 TODO 존재
  - 다중 워커 시 락 전략(예: SKIP LOCKED) 필요
- `CultivationNotificationService`의 제목/본문 텍스트는 하드코딩 상태
  - 메시지 템플릿/소스 분리 여지 있음
