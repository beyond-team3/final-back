package com.monsoon.seedflowplus.domain.sales.request.dto.response;

import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record QuotationRequestResponse(
        Long id,
        String requestCode,
        String clientName,
        String requirements,
        QuotationRequestStatus status,
        LocalDateTime createdAt,
        List<ItemResponse> items) {
    public static QuotationRequestResponse from(QuotationRequestHeader header) {
        return new QuotationRequestResponse(
                header.getId(),
                header.getRequestCode(),
                header.getClient().getClientName(),
                header.getRequirements(),
                header.getStatus(),
                header.getCreatedAt(),
                header.getItems().stream()
                        .map(ItemResponse::from)
                        .toList());
    }

    public record ItemResponse(
            Long id,
            Long productId,
            String productCategory,
            String productName,
            Integer quantity,
            String unit) {
        public static ItemResponse from(
                com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestDetail detail) {
            return new ItemResponse(
                    detail.getId(),
                    detail.getProduct() != null ? detail.getProduct().getId() : null,
                    detail.getProductCategory(),
                    detail.getProductName(),
                    detail.getQuantity(),
                    detail.getUnit());
        }
    }
}
