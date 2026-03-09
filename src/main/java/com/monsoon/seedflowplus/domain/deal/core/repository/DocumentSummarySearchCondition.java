package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import lombok.Builder;

@Builder
public record DocumentSummarySearchCondition(
        DealType docType,
        String status,
        String keyword,
        Long ownerEmpId,
        Long clientId
) {
}
