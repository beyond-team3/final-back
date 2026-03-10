# Notification 기능 구현 요약 (Current)

## 1. 목적
알림 도메인은 사용자별 알림 생성, 조회, 읽음/삭제 처리, 실시간(SSE) 전달, 예약 발송, 보존기간 정리를 담당한다.

주요 시나리오:
- 재배 알림 생성: 파종 프로모션, 수확 피드백
- 결재/딜 이벤트 알림 생성: 승인 요청/완료/반려, 딜 상태 변경
- 내 알림 조회/미읽음 수 조회/읽음 처리
- 단건 삭제/전체 삭제
- SSE 구독 및 실시간 전송
- 스케줄러 기반 예약 발송 및 만료 알림 정리

## 2. 패키지 구조
- `domain.notification.controller`
  - `NotificationController`
- `domain.notification.query`
  - `NotificationQueryService`
- `domain.notification.command`
  - `NotificationCommandService`
  - `NotificationDeliveryWorkerService`
  - `NotificationSseService`
- `domain.notification.service`
  - `CultivationNotificationService`
  - `DealApprovalNotificationService`
- `domain.notification.event`
  - 이벤트 모델: `DealStatusChangedEvent`, `ApprovalRequestedEvent`, `ApprovalCompletedEvent`, `ApprovalRejectedEvent`
  - 발행/처리: `NotificationEventPublisher`, `NotificationEventHandler`
- `domain.notification.scheduler`
  - `NotificationDeliveryScheduler`
- `domain.notification.repository`
  - `NotificationRepository`, `NotificationDeliveryRepository`
- `domain.notification.entity`
  - `Notification`, `NotificationDelivery`
  - enum: `NotificationType`, `NotificationTargetType`, `DeliveryStatus`, `DeliveryChannel`

## 3. 핵심 도메인 정책

### 3.1 중복 생성 방지
- 공통 중복 범위: 당일 `[00:00, 익일 00:00)`
- 재배 알림:
  - 키: `user + type + targetType + targetId`
  - 조회: `existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtBetween(...)`
- 결재/딜 알림:
  - 기본 키: `user + type + targetType + targetId`
  - 딜 상태 변경은 전이 문자열(content)을 dedup 키로 사용해 전이 단위 중복 제어
- 생성 경합 완화: 사용자 row `PESSIMISTIC_WRITE` 락 후 중복 체크/저장

### 3.2 생성 후 Delivery 정책
- 재배 알림: `NotificationDelivery(status=PENDING, channel=IN_APP, scheduledAt=계산값)` 저장
- 결재/딜 이벤트 알림:
  - `scheduledAt=event.occurredAt`로 생성
  - 즉시 `markSent` 처리하여 상태를 `SENT`로 기록
  - 알림 본문은 이벤트 핸들러에서 after-commit 시점 SSE 전송

### 3.3 스케줄 시간 계산 (재배 알림)
- 파종 프로모션: `sowingStart - 1개월, 09:00`
  - 계산 결과가 `now` 이전이면 `now` 사용
- 수확 피드백: `harvestingStart, 09:00`
  - `now` 이전이면 하루씩 증가시켜 `>= now` 보장

### 3.4 Delivery 워커 처리
- 대상: `PENDING && scheduledAt <= now` 상위 100건
- 선점: `FOR UPDATE SKIP LOCKED` (MariaDB 10.6+ 가정)
- 선점/조회 실패(`DataAccessException`) 시 non-atomic fallback 미사용(중복 발송 방지 목적)
- 채널별 처리:
  - `IN_APP`: `NotificationSseService.send(...)` 성공 시 `markSent`
  - 전송 실패/예외 시 `markFailed`
- 실패 사유는 trim/blank fallback/500자 제한으로 정규화

### 3.5 읽음/삭제/보존
- 단건 읽음: `findByIdAndUser_Id(...)`로 소유 검증 후 `markAsRead(now)`
- 전체 읽음: `markAllAsRead(userId, now)` 벌크 업데이트
- 단건 삭제: delivery 소프트삭제 후 notification 소프트삭제
- 전체 삭제: 사용자 기준 delivery/notification 소프트삭제
- 보존기간 정리: `cutoff(createdAt < cutoff)` 기준 delivery 선삭제 후 notification 소프트삭제
- 엔티티 삭제 기본 정책:
  - `Notification`은 `@SQLDelete/@SQLRestriction` 기반 소프트삭제
  - `NotificationDelivery`는 리포지토리 native update로 `is_deleted=true` 처리

## 4. 이벤트 기반 처리 흐름
1. 도메인(Approval/Deal)에서 `NotificationEventPublisher.publishAfterCommit(event)` 호출
2. 커밋 이후 이벤트 발행
3. `NotificationEventHandler(@Async)`가 이벤트별 생성 서비스 호출
4. 생성 성공 시 `sendIfPresent`에서 after-commit SSE 전송 예약 또는 즉시 전송
5. 중복으로 미생성(null 반환) 시 SSE 미전송

## 5. API
기본 경로: `/api/v1/notifications`

- `GET /api/v1/notifications?page=0&size=20`
  - 내 알림 목록(Page)
  - `size`: 1~100
- `GET /api/v1/notifications/unread-count`
  - 미읽음 수
- `PATCH /api/v1/notifications/{notificationId}/read`
  - 단건 읽음 (`notificationId > 0`)
- `PATCH /api/v1/notifications/read-all`
  - 전체 읽음
- `DELETE /api/v1/notifications/{notificationId}`
  - 단건 삭제
- `DELETE /api/v1/notifications`
  - 전체 삭제
- `GET /api/v1/notifications/subscribe` (`text/event-stream`)
  - 사용자별 SSE 구독

응답은 공통 `ApiResult` 포맷 사용.

## 6. 스케줄러/설정
- `NotificationDeliveryScheduler.dispatchDueDeliveries`
  - fixed delay: `${notification.delivery.dispatch-fixed-delay-ms:60000}`
- `NotificationDeliveryScheduler.deleteExpiredNotifications`
  - cron: `${notification.retention.cron:0 0 3 * * *}`
  - retention days: `${notification.retention-days:30}`

## 7. 트랜잭션/시간 정책
- Query 서비스: `@Transactional(readOnly = true)`
- Command/Worker/DealApproval/Cultivation/EventHandler: 쓰기 트랜잭션 경계에서 동작
- 시간 기준:
  - 컨트롤러/스케줄러는 `Clock` 주입 + `LocalDateTime.now(clock)`
  - 이벤트 발생 시간은 이벤트 payload(`occurredAt`) 사용
- 이벤트 발행은 `publishAfterCommit` 우선 사용

## 8. 예외/검증
- 인증 principal 또는 userId 누락: `UNAUTHORIZED`
- 소유 알림 미존재(읽음/삭제): `NOTIFICATION_NOT_FOUND`
- 사용자 잠금 조회 실패: `USER_NOT_FOUND`
- 입력 검증(`@Positive`, `@Min`, `@Max`) 실패는 전역 예외 처리기로 공통 오류 응답

## 9. 알림 타입 목록 (프론트 매핑용)
`Notification.type`은 아래 enum 문자열로 내려온다.

- `ACCOUNT_ACTIVATED`: 계정 활성화 (예약)
- `QUOTATION_REQUEST_CREATED`: 견적요청서 생성 (예약)
- `QUOTATION_APPROVAL_RESULT`: 견적 승인 결과 (예약)
- `CONTRACT_APPROVAL_REQUESTED`: 계약 승인 요청 (예약)
- `CONTRACT_APPROVAL_RESULT`: 계약 승인 결과 (예약)
- `INVOICE_ISSUED`: 청구서 발행 (예약)
- `STATEMENT_ISSUED`: 명세서 발행 (예약)
- `STOCK_CHANGED`: 재고 변경 (예약)
- `CULTIVATION_SOWING_PROMOTION`: 재배 파종 프로모션 (운영 중)
- `CULTIVATION_HARVEST_FEEDBACK`: 재배 수확 피드백 (운영 중)
- `DEAL_STATUS_CHANGED`: 딜 상태 변경 (운영 중)
- `APPROVAL_REQUESTED`: 승인 요청 도착 (운영 중)
- `APPROVAL_COMPLETED`: 승인 완료 (운영 중)
- `APPROVAL_REJECTED`: 승인 반려 (운영 중)

프론트 권장:
- 목록/뱃지 렌더링은 `type` 문자열 기준 분기
- 예약 타입(현재 미발행)도 기본 fallback UI(`title`, `content`)로 처리

## 10. 액터 권한/접근 범위

### 10.1 알림 API 접근 권한
- 보안 설정 기준(`/api/v1/notifications/**`): 특정 role 제한 없음, `authenticated`면 접근 가능
- 즉 `ADMIN`, `SALES_REP`, `CLIENT` 모두 호출 가능
- 단, 데이터 범위는 항상 본인 소유(userId)로 제한
  - 목록/미읽음/읽음/삭제/SSE 구독 모두 `@AuthenticationPrincipal`의 `userId`를 사용
  - 다른 사용자 알림을 조회/조작하는 API는 없음

### 10.2 타입별 수신 액터 (현재 구현)
- `CULTIVATION_SOWING_PROMOTION`, `CULTIVATION_HARVEST_FEEDBACK`
  - 수신자: 생성 요청에 전달된 `userId` 1명
  - 주 용도: 사용자 개인 재배 알림
- `DEAL_STATUS_CHANGED`
  - 수신자: 해당 딜 `ownerEmp`에 매핑된 사용자(`Role.SALES_REP`가 일반적)
  - 발행 지점: `DealPipelineFacade`
- `APPROVAL_REQUESTED`
  - 수신자(1차): 모든 `ADMIN`
  - 수신자(2차): 다음 step이 `CLIENT`이면 해당 `clientIdSnapshot` 사용자 1명
  - 발행 지점: `ApprovalCommandService`
- `APPROVAL_COMPLETED`, `APPROVAL_REJECTED`
  - 수신자: 승인 요청자(문서 소유 딜의 담당 영업 사용자)
  - 발행 지점: `ApprovalCommandService`

### 10.3 프론트 구현 포인트
- 권한별 메뉴 노출은 프론트 role 정책대로 처리하되, 알림 API 자체는 인증 사용자 공통 엔드포인트로 사용
- 실시간 탭에서는 `/subscribe` 연결 후 `notification` 이벤트 수신 payload의 `type`으로 라우팅
- 승인 요청 알림은 동일 타입이라도 수신 액터가 `ADMIN`/`CLIENT`로 달라질 수 있으므로 문맥(`targetType`, `targetId`, `content`) 기반 화면 이동을 권장
