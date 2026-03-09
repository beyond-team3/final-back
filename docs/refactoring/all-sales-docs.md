
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
