
## [2026-03-09] document summary view 추가

### 변경 대상
- 파일: src/main/resources/db/migration/V1__create_v_document_summary.sql
- 클래스/메서드: v_document_summary (DB View)

### 변경 내용
문서 통합 조회를 위한 `v_document_summary` 뷰 생성 마이그레이션을 추가했다.
RFQ/QUO/CNT/ORD/STMT/INV/PAY 헤더 테이블을 `UNION ALL`로 통합했다.
문서별 surrogate_id, 타입, 코드, 금액, 상태, 생성일 컬럼을 동일 스키마로 정규화했다.

### 변경 이유
통합 문서 요약 조회를 위한 조회 소스 일원화.

## [2026-03-09 14:42] Flyway 문서 요약 뷰 마이그레이션 추가

### 작업 내용
- 수정 파일: src/main/resources/db/migration/V1__create_v_document_summary.sql — v_document_summary 뷰 생성 SQL 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
DocumentSummary 엔티티 생성

## [2026-03-09] DocumentSummary 엔티티 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/entity/DocumentSummary.java
- 클래스/메서드: DocumentSummary

### 변경 내용
`v_document_summary` 뷰 기반 읽기 전용 엔티티 `DocumentSummary`를 추가했다.
`@Immutable` + `@Subselect`를 사용해 물리 테이블 매핑 없이 조회 전용으로 구성했다.
문서 구분, 코드, 금액, 만료일, 상태, 생성일 등 공통 요약 필드를 정의했다.

### 변경 이유
통합 문서 요약 뷰를 JPA 레이어에서 직접 조회하기 위한 엔티티 매핑 필요.

## [2026-03-09 15:01] DocumentSummary 엔티티 생성

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/entity/DocumentSummary.java — v_document_summary 매핑 읽기 전용 엔티티 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
DocumentSummaryRepository 및 QueryDSL 커스텀 구현

## [2026-03-10] DocumentSummary 검색 조건 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummarySearchCondition.java
- 클래스/메서드: DocumentSummarySearchCondition

### 변경 내용
DocumentSummary 조회 전용 검색 조건 레코드를 추가했다.
문서 유형, 상태, 키워드, 담당자, 거래처 기준 필드를 `@Builder` 기반으로 정의했다.
이후 Repository 커스텀 조회 시그니처에서 재사용할 조건 객체를 분리했다.

### 변경 이유
통합 문서 조회 Repository 시그니처에 사용할 검색 조건 객체를 선행 정의하기 위해.

## [2026-03-10 01:08] DocumentSummary 검색 조건 생성

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummarySearchCondition.java — 통합 문서 검색 조건 레코드 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
DocumentSummaryRepository 구현 가능 여부 확인

## [2026-03-10] DocumentSummary Repository 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryRepository.java
- 클래스/메서드: DocumentSummaryRepository
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepository.java
- 클래스/메서드: DocumentSummaryQueryRepository#searchDocuments
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java
- 클래스/메서드: DocumentSummaryQueryRepositoryImpl#searchDocuments

### 변경 내용
DocumentSummary 전용 JPA Repository와 QueryDSL 커스텀 조회 인터페이스/구현체를 추가했다.
`QDocumentSummary`와 `QSalesDeal`를 `deal_id` 기준으로 조인해 역할별 조회 범위를 적용했다.
문서 유형, 상태, 문서 코드 키워드 조건과 `createdAt DESC` 정렬, 분리된 count 쿼리를 구성했다.

### 변경 이유
통합 문서 요약 뷰를 권한 범위 내에서 페이징 조회하기 위한 저장소 계층이 필요해서.

## [2026-03-10 01:12] DocumentSummary Repository 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryRepository.java — DocumentSummary JPA Repository 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepository.java — 커스텀 조회 인터페이스 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java — 역할 필터 및 동적 조건 QueryDSL 구현 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 05:03] all-sales-docs 기능 요약 문서 추가

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/docs/all-sales-docs-feature-summary.md — 통합 문서 조회 기능의 구성, 권한 정책, API, 제한 사항 정리

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 04:57] DocumentSummary Repository import 보정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java — `AccessDeniedException` import 누락 보정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10] DocumentSummary 조회 서비스 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/service/DocumentSummaryQueryService.java
- 클래스/메서드: DocumentSummaryQueryService#getDocuments
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/dto/response/DocumentSummaryResponse.java
- 클래스/메서드: DocumentSummaryResponse

### 변경 내용
DocumentSummary 조회 전용 서비스와 응답 DTO를 추가했다.
서비스에서 사용자 역할과 식별자 존재 여부를 검증한 뒤 Repository 조회로 위임하도록 구성했다.
현재 SalesDeal 이름 스냅샷 필드가 없어 `clientName`, `ownerEmployeeName`은 `null`로 매핑하고 TODO를 남겼다.

### 변경 이유
통합 문서 목록 API에서 사용할 읽기 전용 서비스 계층과 응답 모델이 필요해서.

## [2026-03-10 01:14] DocumentSummary 조회 서비스 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/service/DocumentSummaryQueryService.java — 역할 검증 및 Repository 위임 로직 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/dto/response/DocumentSummaryResponse.java — 문서 목록 응답 DTO 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
DocumentSummaryQueryController 추가

## [2026-03-10] DocumentSummary 조회 컨트롤러 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/controller/DocumentSummaryQueryController.java
- 클래스/메서드: DocumentSummaryQueryController#getDocuments

### 변경 내용
`GET /api/v1/documents` 엔드포인트를 추가했다.
문서 타입, 상태, 키워드 필터와 페이지네이션 파라미터를 받아 검색 조건과 Pageable로 변환하도록 구성했다.
정렬 필드는 `createdAt`만 허용하고 응답은 `ApiResult<Page<DocumentSummaryResponse>>`로 반환한다.

### 변경 이유
통합 문서 요약 목록을 외부 API로 조회할 수 있는 입력 경로가 필요해서.

## [2026-03-10 01:15] DocumentSummary 조회 컨트롤러 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/controller/DocumentSummaryQueryController.java — 문서 목록 조회 API 엔드포인트 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
bootRun 기동 및 /api/v1/documents 확인

## [2026-03-10 01:22] DocumentSummary import 누락 보정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/entity/DocumentSummary.java — `DealType` import 누락 보정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → bootRun 시 `jwt.secret` placeholder 미해결로 애플리케이션 기동 실패

### 다음 단계
`jwt.secret` 환경설정 주입 후 `/api/v1/documents` 기동 재확인

## [2026-03-10 04:36] DocumentSummary 조회 권한 및 조건 보강

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/service/DocumentSummaryQueryService.java — 허용 역할 화이트리스트 검증 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java — ownerEmpId/clientId 조건 반영 및 미지원 역할 차단 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
