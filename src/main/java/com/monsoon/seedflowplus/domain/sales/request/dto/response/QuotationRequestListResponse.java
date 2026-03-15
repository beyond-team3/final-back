package com.monsoon.seedflowplus.domain.sales.request.dto.response;

import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;

import java.time.LocalDate;

public record QuotationRequestListResponse(
        Long id,
        String requestCode,
        Long clientId,
        String clientName,
        String managerName,
        LocalDate createdAt,
        QuotationRequestStatus status
) {
    public static QuotationRequestListResponse from(QuotationRequestHeader header) {
        return new QuotationRequestListResponse(
                header.getId(),
                header.getRequestCode(),
                header.getClient().getId(),
                header.getClient().getClientName(),
                header.getClient().getManagerName(),
                header.getCreatedAt().toLocalDate(),
                header.getStatus()
        );
    }
}
