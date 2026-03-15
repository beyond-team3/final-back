package com.monsoon.seedflowplus.domain.sales.quotation.v2.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record QuotationV2CreateRequest(
        Long requestId,
        Long dealId,
        @NotNull(message = "거래처 ID는 필수입니다.") Long clientId,
        @NotEmpty(message = "견적 품목은 최소 하나 이상이어야 합니다.") @Valid List<Item> items,
        String memo
) {
    public record Item(
            Long productId,
            @NotNull(message = "제품명은 필수입니다.") String productName,
            String productCategory,
            @NotNull(message = "수량은 필수입니다.") @Positive(message = "수량은 0보다 커야 합니다.") Integer quantity,
            String unit,
            @NotNull(message = "단가는 필수입니다.") @Positive(message = "단가는 0보다 커야 합니다.") BigDecimal unitPrice
    ) {
    }
}
