package com.monsoon.seedflowplus.domain.rfq.entity;

import com.monsoon.seedflowplus.domain.pipeline.enums.DocumentStatus;

/**
 * 견적요청서(RFQ) 상태.
 * RFQ는 거래처가 제출하면 바로 PENDING 상태가 되며,
 * 별도 승인 플로우 없이 영업사원이 검토 후 견적서(QUO)로 전환(CONVERT)한다.
 */
public enum RequestStatus implements DocumentStatus {
    PENDING,    // 견적 대기 - 거래처 제출 완료, 영업사원 미확인
    REVIEWING,  // 검토 중   - 영업사원 확인 중
    CANCELED    // 요청 취소 - 거래처 또는 영업사원이 취소
}
