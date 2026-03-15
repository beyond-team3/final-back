package com.monsoon.seedflowplus.domain.sales.quotation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record QuotationCreateRequest(
        Long requestId, // 견적요청서 기반 작성 시 필수

        Long sourceQuotationId, // 반려/만료 견적서 재작성 시 원본 견적서 ID

        @NotNull(message = "거래처 ID는 필수입니다.") Long clientId,

        @NotEmpty(message = "견적 품목은 최소 하나 이상이어야 합니다.") @Valid List<QuotationItemRequest> items,

        String memo) {
    public record QuotationItemRequest(
            Long productId,

            @NotNull(message = "제품명은 필수입니다.") String productName,

            String productCategory,

            @NotNull(message = "수량은 필수입니다.") @Positive(message = "수량은 0보다 커야 합니다.") Integer quantity,

            String unit,

            @NotNull(message = "단가는 필수입니다.") @Positive(message = "단가는 0보다 커야 합니다.") BigDecimal unitPrice) {
    }
}
