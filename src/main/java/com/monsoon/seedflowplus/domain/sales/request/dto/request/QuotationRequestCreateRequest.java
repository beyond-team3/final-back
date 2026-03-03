package com.monsoon.seedflowplus.domain.sales.request.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuotationRequestCreateRequest(
        String requirements,
        @NotEmpty(message = "품목은 최소 1개 이상이어야 합니다.") List<ItemRequest> items) {
    public record ItemRequest(
            Long productId,
            @NotBlank(message = "상품 품종은 필수입니다.") String productCategory,
            @NotBlank(message = "상품 이름은 필수입니다.") String productName,
            @NotNull(message = "수량은 필수입니다.") Integer quantity) {
    }
}
