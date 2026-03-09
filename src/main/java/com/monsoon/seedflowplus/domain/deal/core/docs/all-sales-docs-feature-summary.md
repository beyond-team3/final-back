# All Sales Docs Feature Summary

## 개요

all-sales-docs 기능은 RFQ, QUO, CNT, ORD, STMT, INV, PAY 문서를 하나의 조회 모델로 통합해
권한 범위 내에서 문서 목록을 조회할 수 있도록 구성한 기능이다.

## 구성 요소

### 1. DB View
- 파일: `src/main/resources/db/migration/V1__create_v_document_summary.sql`
- 뷰명: `v_document_summary`
- 역할:
  - 문서 헤더 테이블들을 `UNION ALL`로 통합
  - 문서 유형, 문서 코드, 금액, 상태, 생성일을 공통 컬럼으로 정규화
  - `surrogate_id`를 `<DOC_TYPE>-<ID>` 형식으로 생성

### 2. 조회 엔티티
- 파일: `domain/deal/core/entity/DocumentSummary.java`
- 특징:
  - `@Immutable`
  - `@Subselect` 기반 읽기 전용 엔티티
  - `v_document_summary`를 직접 매핑

### 3. 검색 조건
- 파일: `domain/deal/core/repository/DocumentSummarySearchCondition.java`
- 지원 조건:
  - `docType`
  - `status`
  - `keyword`
  - `ownerEmpId`
  - `clientId`

### 4. Repository 계층
- 파일:
  - `domain/deal/core/repository/DocumentSummaryRepository.java`
  - `domain/deal/core/repository/DocumentSummaryQueryRepository.java`
  - `domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java`
- 역할:
  - QueryDSL 기반 목록 조회
  - `DocumentSummary`와 `SalesDeal`를 `deal_id` 기준으로 조인
  - 권한 범위 필터 및 조건 검색 처리
  - `createdAt DESC` 정렬
  - count 쿼리 분리

## 권한 정책

- `ADMIN`
  - 전체 문서 조회 가능
- `SALES_REP`
  - `deal.ownerEmp.id == userDetails.employeeId` 범위만 조회
- `CLIENT`
  - `documentSummary.clientId == userDetails.clientId` 범위만 조회
- 그 외 역할
  - `AccessDeniedException` 발생

## API

### 문서 목록 조회
- 경로: `GET /api/v1/documents`
- 컨트롤러: `DocumentSummaryQueryController`
- 응답: `ApiResult<Page<DocumentSummaryResponse>>`

### 요청 파라미터
- `docType`: 문서 유형
- `status`: 문서 상태
- `keyword`: 문서 코드 검색어
- `page`: 기본값 `0`
- `size`: 기본값 `20`, 최대 `DealPaginationConstants.MAX_PAGE_SIZE`
- `sort`: 기본값 `createdAt,desc`

### 정렬 정책
- 허용 정렬 필드: `createdAt`

## 응답 DTO

- 파일: `domain/deal/core/dto/response/DocumentSummaryResponse.java`
- 포함 값:
  - `surrogateId`
  - `docType`
  - `docId`
  - `docCode`
  - `amount`
  - `expiredDate`
  - `status`
  - `createdAt`
  - `clientName`
  - `ownerEmployeeName`

## 현재 제한 사항

- `clientName`, `ownerEmployeeName`은 현재 `SalesDeal`에 이름 스냅샷 필드가 없어 `null`로 반환된다.
- 이름 정보가 필요하면 `SalesDeal` 또는 조회 모델에 스냅샷 필드를 추가한 뒤 매핑 보완이 필요하다.
