# Notification 기능 구현 요약

## 1. 목적
사용자 알림의 생성/조회/읽음처리/전송 상태 관리를 담당한다.

- 재배적기 알림 생성 (파종 추천, 수확 피드백)
- 내 알림 목록 조회 및 미읽음 수 확인
- 단건/전체 읽음 처리
- 발송 예정 건 워커 처리

## 2. 패키지 구조

- `domain.notification.command`
  - `NotificationCommandService`
  - `NotificationDeliveryWorkerService`
- `domain.notification.query`
  - `NotificationQueryService`
- `domain.notification.service`
  - `CultivationNotificationService` (재배적기 알림 생성 유스케이스)
- `domain.notification.controller`
  - `NotificationController`
- `domain.notification.dto.response`
  - `NotificationListItemResponse`
  - `UnreadCountResponse`
- `domain.notification.repository`
  - `NotificationRepository`
  - `NotificationDeliveryRepository`
- `domain.notification.entity`
  - `Notification`, `NotificationDelivery`, 관련 enum

## 3. 핵심 도메인 정책

### 3.1 중복 생성 방지
- 키: `user + type + targetType + targetId`
- 범위: `오늘 00:00 ~ 내일 00:00`
- `existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtBetween(...)`로 확인
- 생성 경합 완화: `CultivationNotificationService`에서 사용자 row를 `PESSIMISTIC_WRITE` 락으로 선점 후 체크/저장

### 3.2 스케줄 시간 계산
- 파종 추천: `sowingStart - 1개월, 09:00`
  - 계산 결과가 기준 시각(`now`)보다 과거면 `now` 사용
- 수확 피드백: `harvestingStart, 09:00`
  - 계산 결과가 과거면 하루씩 증가시켜 `>= now` 보장

### 3.3 Delivery 생성/처리
- 생성 시 기본값: `channel=IN_APP`, `status=PENDING`, `scheduledAt` 필수
- 워커는 `PENDING && scheduledAt <= now` 100건을 락 조회(`FOR UPDATE SKIP LOCKED`) 후 처리
- 성공: `markSent(...)`
- 실패: `markFailed(...)`
  - 실패 사유는 trim + 500자 제한 후 저장/로그 동일 문자열 사용

### 3.4 읽음 처리
- 단건: `findByIdAndUser_Id(...)`로 소유권 포함 조회 후 `markAsRead(now)`
- 전체: 벌크 업데이트 `markAllAsRead(userId, now)`

## 4. API
기본 경로: `/api/v1/notifications`

- `GET /api/v1/notifications?page=0&size=20`
  - 내 알림 목록(Page)
  - `size`는 1~100 검증
- `GET /api/v1/notifications/unread-count`
  - 미읽음 개수 반환
- `PATCH /api/v1/notifications/{notificationId}/read`
  - 단건 읽음 처리 (`notificationId`는 양수 검증)
- `PATCH /api/v1/notifications/read-all`
  - 전체 읽음 처리

응답 포맷은 공통 `ApiResult` 사용.

## 5. 데이터/영속성 포인트

- `NotificationDelivery.scheduledAt`은 `nullable = false`
- `NotificationDelivery` 빌더 생성자에서 필수 필드 null 방지
- 조회 성능:
  - 알림: `findByUser_IdOrderByCreatedAtDesc(...)`
  - 미읽음 카운트: `countByUser_IdAndReadAtIsNull(...)`

## 6. 트랜잭션/시간 일관성

- Query 서비스: `@Transactional(readOnly = true)`
- Command/Worker/Cultivation: `@Transactional`
- 컨트롤러 시간은 `Clock` 주입 + `LocalDateTime.now(clock)` 사용

## 7. 예외/검증

- 인증 principal 누락 시 `UNAUTHORIZED`
- 알림 미존재 시 `NOTIFICATION_NOT_FOUND`
- 입력 검증 실패(`@Positive`, `@Min`, `@Max`)는 전역 예외핸들러를 통해 공통 에러 응답 처리
