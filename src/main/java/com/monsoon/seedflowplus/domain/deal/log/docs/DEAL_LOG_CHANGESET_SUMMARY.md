# DealLog 작업 정리 + 커밋 메시지 가이드

## 1) 이번 작업의 목표
- Deal 로그를 `SalesDealLog(tbl_sales_deal_log)` 중심으로 표준화
- 상태 전이 검증을 서비스 레벨에서 강제
- 로그 조회 API(조회 전용) 제공
- 로그 상세(`DealLogDetail`) 확장 구조 도입
- CONVERT 시 원본/신규 2건 로그 규칙 반영 가능 구조 구축

---

## 2) 변경된 주요 기능

### A. 엔티티
- `DealLogDetail` 신규 추가
  - 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/entity/DealLogDetail.java`
  - 테이블: `tbl_sales_deal_log_detail`
  - 컬럼: `deal_log_detail_id`, `deal_log_id(unique)`, `reason(TEXT)`, `diff_json(TEXT)`, `created_at`
- `SalesDealLog`에 선택적 1:1 역매핑 추가
  - 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/entity/SalesDealLog.java`

### B. Repository
- `SalesDealLogRepository` 신규
  - deal/client/docType+refId 타임라인 조회
  - 권한 스코프 조회용 메서드(영업사원/거래처 조건) 추가
  - 기본 타임라인 정렬 상수 제공
- `DealLogDetailRepository` 신규
  - `findByDealLogId(Long dealLogId)`

### C. 로그 기록 서비스(내부 전용)
- `DealLogWriteService` 신규
  - 상태변경 로그 1건 저장
  - reason/diffJson 존재 시 `DealLogDetail` 동시 저장
  - `ActorType.SYSTEM -> actorId=null` 검증
  - KST(`Asia/Seoul`) actionAt 기본 처리
  - CONVERT 전용 메서드 및 pair 저장 메서드 제공

### D. 상태 전이 정책/검증
- `DocStatusTransitionPolicy` 신규
  - DealType별 전이 규칙 중앙 관리
  - terminal 상태 재요청 차단
- `DocStatusTransitionValidator` 신규
  - 상태 변경 전에 `validateOrThrow(...)` 호출하는 진입점 제공
  - 실패 시 예외 발생(트랜잭션 롤백, 로그 미기록)

### E. 로그 조회 서비스/DTO/컨트롤러
- `DealLogQueryService` 신규
  - `getTimelineByDeal`
  - `getTimelineByClient`
  - `getTimelineByDocument(docType, refId)`
  - `getLogDetail(dealLogId)` (옵션)
  - sort 미지정 시 기본 정렬 강제: `actionAt DESC`, `targetCode ASC`
- DTO 신규
  - `DealLogSummaryDto`
  - `DealLogDetailDto`
- 조회 전용 API 컨트롤러 신규 (`GET` only)
  - `GET /api/v1/deals/{dealId}/logs`
  - `GET /api/v1/clients/{clientId}/logs`
  - `GET /api/v1/deal-logs?docType=XXX&refId=123`
  - `GET /api/v1/deal-logs/{dealLogId}/detail` (옵션)
  - 권한 실패 시 `403` (`ACCESS_DENIED`)

### F. 연결 가이드 문서
- `DealLogIntegrationGuide.md` 신규
  - 실제 연결 없이, 각 문서 서비스에서의 호출 순서/예시 코드 제공
  - 순서:
    1. 현재 상태 조회
    2. 상태 전이 검증
    3. 상태 변경 저장
    4. DealLog 기록 호출

---

## 3) 공통 규칙 반영 체크
- Controller에 로그 생성 API 미구현 (Service 내부에서만 기록)
- 문서 기준 조회(docType+refId) 제공
- 기본 조회 3종 제공(deal/client/document)
- 페이징: `Pageable`(offset)
- 정렬 기본값: `actionAt DESC, targetCode ASC`
- CONVERT 2건 기록 구조 제공(원본 CONVERT + 신규 CREATE)
- 상태 전이 검증 분리(`형식 검증` vs `전이 가능성 검증`)

---

## 4) 빌드 검증
- `./gradlew compileJava` 성공

---

## 5) 커밋 메시지 예시

### 옵션 A: 한 번에 1커밋
```text
feat(deal-log): implement end-to-end deal log domain (entity/repository/service/policy/query api)

- add DealLogDetail entity and one-to-one mapping with SalesDealLog
- add SalesDealLogRepository and DealLogDetailRepository
- implement DealLogWriteService with KST actionAt and convert-pair logging
- add DocStatusTransitionPolicy/Validator for strict status transition checks
- implement DealLogQueryService + DealLogController (GET timelines/detail only)
- add timeline DTOs and integration guide markdown
```

### 옵션 B: 기능별 분리 커밋
1. `feat(deal-log): add DealLogDetail entity with one-to-one mapping to SalesDealLog`
2. `feat(deal-log): add repositories for timeline and detail queries`
3. `feat(deal-log): implement DealLogWriteService with convert pair logging`
4. `feat(deal-log): add centralized status transition policy and validator`
5. `feat(deal-log): implement read-only timeline/detail query service and controller`
6. `docs(deal-log): add service integration guide for status-change logging flow`

---

## 6) 참고: 이번 변경 파일
- `src/main/java/com/monsoon/seedflowplus/domain/deal/entity/SalesDealLog.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/entity/DealLogDetail.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/repository/SalesDealLogRepository.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/repository/DealLogDetailRepository.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/service/DealLogWriteService.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/policy/DocStatusTransitionPolicy.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/service/DocStatusTransitionValidator.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/dto/response/DealLogSummaryDto.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/dto/response/DealLogDetailDto.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/service/DealLogQueryService.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/controller/DealLogController.java`
- `src/main/java/com/monsoon/seedflowplus/domain/deal/service/DealLogIntegrationGuide.md`
