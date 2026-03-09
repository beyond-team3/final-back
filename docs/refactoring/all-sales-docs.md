
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
