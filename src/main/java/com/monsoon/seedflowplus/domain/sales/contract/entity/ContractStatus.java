package com.monsoon.seedflowplus.domain.sales.contract.entity;

import com.monsoon.seedflowplus.domain.deal.common.DocumentStatus;

public enum ContractStatus implements DocumentStatus {
    WAITING_ADMIN, // 관리자 승인 대기
    REJECTED_ADMIN, // 관리자 반려
    WAITING_CLIENT, // 거래처 승인 대기 [관리자 승인]
    REJECTED_CLIENT, // 거래처 반려
    COMPLETED, // 계약 완료 [거래처 승인, 시작일이 미래인 승인 완료 계약]
    ACTIVE_CONTRACT, // 계약진행중 [거래처 승인 직후 즉시 활성 또는 시작날짜 ~ 종료날짜]
    EXPIRED, // 계약 만료
    DELETED; // 삭제
}
