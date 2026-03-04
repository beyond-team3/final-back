package com.monsoon.seedflowplus.domain.sales.request.dto.response;

import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;

import java.time.LocalDateTime;

public record QuotationRequestListResponse(
        Long id,
        String requestCode,
        String clientName,
        String managerName,
        LocalDateTime createdAt,
        QuotationRequestStatus status) {
    public static QuotationRequestListResponse from(QuotationRequestHeader header) {
        return new QuotationRequestListResponse(
                header.getId(),
                header.getRequestCode(),
                header.getClient().getClientName(),
                header.getClient().getManagerName(), // Client의 담당자명
                header.getCreatedAt(),
                header.getStatus());
    }
}
