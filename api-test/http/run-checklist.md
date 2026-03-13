# API HTTP Test Run Checklist

## 0) 사전 준비
- 서버 기동: `{{baseUrl}}` (기본 `http://localhost:8080`) 응답 가능 상태
- IntelliJ HTTP Client 사용
- 파일: `api-test/http/*.http`

## 1) 환경값 세팅 (`_env.http`)
- `@baseUrl` 확인
- 로그인 계정 값 수정
  - `@client1LoginId`, `@client1LoginPw`
  - `@salesLoginId`, `@salesLoginPw`
  - `@adminLoginId`, `@adminLoginPw`
- 주문 생성용 ID 세팅
  - `@testContractId`
  - `@testContractDetailId`

## 2) contractDetailId 확인 (중요)
- 이유: 주문 생성 API(`/api/v1/orders`)는 `headerId`와 `items[].contractDetailId`를 요구함.
- 그런데 계약 조회 응답(`ContractResponse.items`)은 아래만 제공하고 `contractDetailId`를 제공하지 않음.
  - `productId`, `productName`, `productCategory`, `totalQuantity`, `unit`, `unitPrice`, `amount`
- 따라서 현재 테스트에서는 DB에서 `contractDetailId(=cnt_detail_id)`를 조회해 `_env.http`에 수동 입력해야 함.
- 예시 SQL:
```sql
SELECT d.cnt_detail_id, d.cnt_id, d.product_name, d.total_quantity
FROM tbl_contract_detail d
WHERE d.cnt_id = <contractId>
ORDER BY d.cnt_detail_id;
```

## 3) 토큰 발급 (`_env.http`)
- 아래 3개 요청 순차 실행
  1. `Auth: CLIENT #1`
  2. `Auth: SALES_REP`
  3. `Auth: ADMIN`
- 기대 결과
  - 각 요청 `200`
  - `client.global`에 토큰 저장됨
    - `client1AccessToken`, `salesAccessToken`, `adminAccessToken`

## 4) 공통 데이터 확보 (`_common.http`) - 선택
- 리스트 조회로 ID 자동 세팅
  - `orderId`, `statementId`, `invoiceId`, `invoiceDraftId`, `invoicePublishedId`, `paymentId`
- 데이터가 없으면 `_env.http`의 fallback ID를 수동 입력해서 진행

## 5) 도메인 실행 순서 (권장)
1. `order.http`
2. `statement.http`
3. `invoice.http`
4. `payment.http`
5. `deal/deal.http`
6. `deal/deal-log.http`

## 6) 파일별 최소 확인 포인트
- `order.http`
  - 성공: create(201) -> get(200) -> ORD 승인 완료 이벤트 후 상태/명세서 확인
  - 주문 확정은 공개 `PATCH /api/v1/orders/{orderId}/confirm`가 아니라 승인 흐름에서 `OrderApprovalConfirmedEvent` 후 `OrderService.confirmOrderFromApproval()`로 내부 처리됨
  - 실패: `C004`, `A001`, `O001`, `O002`, `A002`, `O007`
- `statement.http`
  - 성공: list/get
  - 실패: `A001`, `C004`, `S001`, `A002`, `C003`, `O007`
- `invoice.http`
  - 성공: create -> detail -> toggle -> publish(PUBLISHED)
  - 실패: `C002`, `A002`, `I001`, `I002`, `I004`, `C004`
- `payment.http`
  - 성공: process(201) -> get/list
  - 실패: `C002`, `A002`, `I001`, `I003`, `P101`, `A001`
- `deal/deal.http`
  - 성공: deal 목록 조회(sales/admin) + `dealId/clientId/docType/refId` 전역 변수 추출
  - 실패: `A001`, `C002`, `C004`
- `deal/deal-log.http`
  - 성공: deal/client/document 기준 타임라인 + 상세 조회
  - 실패: `A001`, `A002`, `C004`, `C005`, `D001`

## 7) 주의사항
- 테스트 데이터는 local/dev 전용 계정/계약만 사용
- `payment`의 `P102(ALREADY_PAID)`는 코드상 동시성 경합에서 주로 재현됨
  - 단건 재시도는 보통 선행 상태검증 `I003`으로 종료됨
