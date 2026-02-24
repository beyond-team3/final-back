# SalesHistory Enum Guide

## DocType
- `RFQ`: 견적 요청서(Request For Quotation)
- `QUO`: 견적서(Quotation)
- `CNT`: 계약서(Contract)
- `ORD`: 발주서(Order)
- `STMT`: 거래명세서(Statement)
- `INV`: 인보이스(Invoice)

## SalesStage
- `CREATED`: 문서가 생성됨
- `IN_PROGRESS`: 처리 진행 중
- `PENDING_ADMIN`: 관리자 승인 대기
- `REJECTED_ADMIN`: 관리자 반려
- `PENDING_CLIENT`: 고객 승인 대기
- `REJECTED_CLIENT`: 고객 반려
- `APPROVED`: 승인 완료
- `CONFIRMED`: 최종 확정
- `ISSUED`: 발행 완료
- `PAID`: 결제 완료
- `CANCELED`: 취소됨
- `EXPIRED`: 만료됨

## SalesActionType
- `CREATE`: 생성
- `SUBMIT`: 제출
- `RESUBMIT`: 재제출
- `APPROVE_ADMIN`: 관리자 승인
- `REJECT_ADMIN`: 관리자 반려
- `APPROVE_CLIENT`: 고객 승인
- `REJECT_CLIENT`: 고객 반려
- `CONFIRM`: 확정
- `ISSUE`: 발행
- `PAY`: 결제
- `CANCEL`: 취소
- `EXPIRE`: 만료
- `CONVERT`: 다음 문서로 전환

## ActorType
- `EMP`: 일반 직원
- `ADMIN`: 관리자
- `CLIENT`: 고객
- `SYSTEM`: 시스템 자동 처리
