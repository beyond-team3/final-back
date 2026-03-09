## [2026-03-07 02:53] notification 테스트 명세 CSV 추가

### 작업 내용
- 수정 파일: docs/testing/notification-test-spec.csv — notification 도메인 기능 기준 테스트 시나리오 명세 신규 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-07 03:14] schedule 테스트 명세 CSV 추가

### 작업 내용
- 수정 파일: docs/testing/schedule-test-spec.csv — schedule 도메인 통합조회/개인일정/동기화/검증 기준 테스트 시나리오 명세 신규 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-07 17:43] API URL 도메인별 CSV 정리

### 작업 내용
- 수정 파일: docs/api/domain-api-list.csv — 컨트롤러 기준 전체 API URL을 도메인/기능그룹/요구사항/메서드/경로/설명/권한으로 정리한 CSV 신규 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-09 09:43] notification 문서에 타입/권한 매핑 추가

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/notification/docs/notification-feature-summary.md — 프론트 연동용 알림 타입 전체 목록과 액터 권한별 접근 범위/수신자 매핑 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-09 09:44] billing statistics 문서 API/권한 보강

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/docs/statistics-backend.md — 프론트 연동을 위한 API 요청/응답 예시와 액터별 접근 권한 표 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-09 10:02] approval 기능 요약 문서 신규 작성

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/docs/approval-feature-summary.md — approval 도메인의 목적/정책/API/권한/알림 연계 내용을 현재 코드 기준으로 정리한 문서 신규 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-09 15:20] deal log docs 구현 정합성 점검 및 반영

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/docs/deal-log-changeset-summary.md — 실제 패키지 경로, 기본 정렬 규칙, 가이드 문서 파일명 및 기능 목록을 현재 구현 기준으로 정정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 01:18] IntelliJ HTTP Client 시나리오1 추가

### 작업 내용
- 수정 파일: api-test/http/pipeline/scenario1.http — 실제 컨트롤러 경로/권한/응답 구조에 맞춘 SeedFlow+ 전체 파이프라인 연쇄 검증 시나리오 신규 작성
- 수정 파일: src/test/http/http-client.env.json — local HTTP client 실행용 기본 환경 변수 파일 신규 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 01:29] HTTP 시나리오 파일 위치 정리

### 작업 내용
- 수정 파일: api-test/http/pipeline/scenario1.http — 요청한 디렉터리 구조에 맞게 시나리오 파일을 pipeline 하위로 이동

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 02:25] HTTP 시나리오 기본 변수 선언 추가

### 작업 내용
- 수정 파일: api-test/http/pipeline/scenario1.http — IntelliJ HTTP Client에서 환경 파일 선택이 누락되어도 baseUrl/계정 변수가 치환되도록 기본 변수 선언 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 03:34] scenario1 재실행용 DB 초기화 SQL 추가

### 작업 내용
- 수정 파일: api-test/http/pipeline/reset-scenario1.sql — 시나리오 재실행 시 중복 제약과 누적 상태를 제거할 수 있도록 approval/deal/schedule/billing/account/product 관련 테이블 전체 초기화 스크립트 신규 작성

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 03:34] scenario1 bootstrap SQL로 교체

### 작업 내용
- 수정 파일: api-test/http/pipeline/bootstrap-scenario1.sql — 기존 reset 전용 SQL을 삭제하고 초기화 후 admin/sales/client 계정과 거래처를 한 번에 다시 만드는 통합 bootstrap 스크립트로 교체
- 수정 파일: api-test/http/pipeline/reset-scenario1.sql — 단독 reset 파일 삭제

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 03:58] bootstrap SQL 초기화 방식을 DELETE로 변경

### 작업 내용
- 수정 파일: api-test/http/pipeline/bootstrap-scenario1.sql — 빈 테이블에서도 멈추지 않도록 TRUNCATE 대신 DELETE와 AUTO_INCREMENT 초기화 방식으로 전체 초기화 구문 변경

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 04:09] bootstrap SQL를 targeted cleanup 방식으로 변경

### 작업 내용
- 수정 파일: api-test/http/pipeline/bootstrap-scenario1.sql — tbl_client/tbl_employee 전체 삭제 대신 scenario 대상 계정/거래처만 정리하도록 조건부 DELETE 방식으로 변경해 락 대기 범위를 축소

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음
