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

## [2026-03-06 17:11] Notification Phase 3 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/service/DealPipelineFacade.java — 딜 상태 변경 시 담당자 대상 NotificationEvent 발행 연결
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — 승인 요청/완료/반려 시 NotificationEvent 발행 연결
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/account/repository/UserRepository.java — 이벤트 수신자 해석용 조회 메서드 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationTargetType.java — DEAL, APPROVAL targetType 추가
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/DealStatusChangedEvent.java — 딜 상태 변경 이벤트 정의
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalRequestedEvent.java — 승인 요청 이벤트 정의
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalCompletedEvent.java — 승인 완료 이벤트 정의
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/ApprovalRejectedEvent.java — 승인 반려 이벤트 정의
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventPublisher.java — ApplicationEventPublisher 래퍼 추가
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/DealApprovalNotificationService.java — 이벤트 기반 알림 생성 및 중복 방지 로직 추가
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java — @Async 이벤트 수신 및 저장 후 SSE 전송 연결
- 신규 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java — 사용자별 emitter 관리/전송/제거 기본 구현
- 수정 파일: docs/notification/notification-architecture.md — 구조 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 4 (SSE 구독 API 및 워커 연동) 진행

## [2026-03-06 17:20] Notification Phase 4 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java — SSE 구독 API `GET /subscribe` 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java — IN_APP markSent 직후 NotificationSseService.send 연동
- 수정 파일: docs/notification/notification-architecture.md — SSE 구독/워커 연동 구조 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
Phase 5 테스트 추가

## [2026-03-06 18:10] Notification Phase 5 테스트 구현

### 작업 내용
- 신규 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationControllerTest.java — Notification API(WebMvcTest)와 SSE 구독 asyncDispatch 검증 추가
- 신규 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandServiceTest.java — 삭제 로직/소유 검증/NOTIFICATION_NOT_FOUND 테스트 추가
- 신규 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandlerTest.java — 이벤트 수신 시 생성 위임 및 SSE 전송 흐름 테스트 추가
- 신규 파일: src/test/java/com/monsoon/seedflowplus/domain/notification/scheduler/NotificationDeliverySchedulerTest.java — dispatchDueDeliveries/보존 정책 삭제 메서드 호출 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/deal/log/service/DealPipelineFacadeTest.java — 생성자 의존성 변경에 맞춘 테스트 초기화 보정
- 수정 파일: docs/notification/notification-architecture.md — 테스트 구조 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-07 01:12] NotificationSseService compare-remove 경계 보강

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java — key-only 제거 메서드 삭제, compare-remove(`removeIfMatch`) 단일 경로로 통일
- 수정 파일: docs/notification/notification-architecture.md — SSE remove 오용 경계 차단 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 19:15] Notification 결함 이슈 4건 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/Notification.java — 소프트 삭제(@SQLDelete/@SQLRestriction)와 `is_deleted` 컬럼 매핑 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/entity/NotificationDelivery.java — `markFailed` 내부 failReason 정규화(trim/blank fallback/500자 제한) 캡슐화
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java — SKIP LOCKED 쿼리의 MariaDB 10.6+ 전제 및 fallback 경로 문서화
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java — SKIP LOCKED 실패 시 non-locking 조회 fallback 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationRepository.java — `markAllAsRead`에 `@Transactional` 명시
- 수정 파일: docs/notification/notification-architecture.md — 구조 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 18:24] Notification 안정성 이슈 6건 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventPublisher.java — 트랜잭션 커밋 후 발행용 `publishAfterCommit` 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — 알림 이벤트 발행을 after-commit으로 전환, Clock 주입 기반 now 정책 통일
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/service/DealPipelineFacade.java — 딜 상태 변경 이벤트 발행을 after-commit으로 전환
- 수정 파일: src/main/java/com/monsoon/seedflowplus/core/config/AsyncConfig.java — notification 전용 `ThreadPoolTaskExecutor` 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventHandler.java — `@Async("notificationTaskExecutor")` 명시
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java — 동일 user 재연결 시 기존 emitter complete 후 교체
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/repository/NotificationDeliveryRepository.java — SKIP LOCKED 선점 ID 조회 + fetch join 재조회 메서드 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java — 배치 처리 시 연관 엔티티 일괄 로딩으로 N+1 회피
- 수정 파일: docs/notification/notification-architecture.md — 구조 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 18:41] Notification SSE 재연결/예외 정책 정합성 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationSseService.java — emitter 제거를 compare-remove로 변경해 재연결 직후 신규 연결 제거 경쟁 조건 방지
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/CultivationNotificationService.java — 사용자 미존재 시 CoreException(USER_NOT_FOUND)로 예외 정책 통일
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/controller/NotificationController.java — principal.userId null도 UNAUTHORIZED로 정규화
- 수정 파일: docs/notification/notification-architecture.md — 구조/정책 보완 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 19:21] Notification 트랜잭션 readOnly 정책 정렬

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationCommandService.java — 클래스 기본 트랜잭션을 readOnly로 전환하고 쓰기 메서드에 `@Transactional` 재선언
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/service/CultivationNotificationService.java — 클래스 기본 트랜잭션을 readOnly로 전환하고 생성 메서드에 `@Transactional` 재선언
- 수정 파일: docs/notification/notification-architecture.md — 트랜잭션 정책 정렬 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 19:57] NotificationEventPublisher afterCommit 예외 전파 방지

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/event/NotificationEventPublisher.java — afterCommit 내부 publish 예외를 로깅 후 삼키도록 변경, `publish(Object)` 접근 범위를 package-private로 축소
- 수정 파일: docs/notification/notification-architecture.md — 구조/정책 변경 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-07 01:30] Notification 배치 fallback 조회 경로 N+1 제거

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/command/NotificationDeliveryWorkerService.java — SKIP LOCKED 실패 fallback에서도 due delivery ID를 먼저 수집한 뒤 fetch join 재조회 경로로 통일
- 수정 파일: docs/notification/notification-architecture.md — fallback 조회 경로 보강 이력 추가
- 수정 파일: docs/notification/notification-work-log.md — 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
