package com.monsoon.seedflowplus.domain.deal.common;

/**
 * 딜 로그 행동(이벤트) 분류.
 * "무슨 행위로 인해" 상태가 변경되었는지를 표현한다.
 * 승인/반려의 행위자(관리자 vs 거래처) 구분은 SalesDealLog.actorType 으로 판별한다.
 *
 * <pre>
 * 예시:
 *   APPROVE + actorType=ADMIN   → 관리자 승인
 *   APPROVE + actorType=CLIENT  → 거래처 승인
 *   REJECT  + actorType=ADMIN   → 관리자 반려
 *   REJECT  + actorType=CLIENT  → 거래처 반려
 * </pre>
 *
 * CONVERT 발생 시 로그 기록 규칙:
 *   1. 원본 문서에 actionType=CONVERT, toStage=APPROVED 기록
 *   2. 신규 문서에 actionType=CREATE, toStage=CREATED 별도 기록
 */
public enum ActionType {
    CREATE,     // 생성
    SUBMIT,     // 제출 (승인 요청)
    RESUBMIT,   // 재제출 (반려 후 다시 제출)

    CONVERT,    // 전환 (RFQ→QUO, QUO→CNT 등 다음 문서로 전환)

    APPROVE,    // 승인 (actorType으로 관리자/거래처 구분)
    REJECT,     // 반려 (actorType으로 관리자/거래처 구분)
    CONFIRM,    // 확정 (주문 확정 등)

    ISSUE,      // 발행 (청구서 발행 등)
    PAY,        // 결제 완료 처리

    EXPIRE,     // 만료
    CANCEL      // 취소
}
