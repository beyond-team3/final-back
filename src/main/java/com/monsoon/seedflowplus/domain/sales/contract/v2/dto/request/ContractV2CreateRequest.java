package com.monsoon.seedflowplus.domain.sales.contract.v2.dto.request;

import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractV2CreateRequest(
        Long quotationId,
        Long dealId,
        @NotNull(message = "거래처 ID는 필수입니다.") Long clientId,
        @NotNull(message = "계약 시작일은 필수입니다.") LocalDate startDate,
        @NotNull(message = "계약 종료일은 필수입니다.") LocalDate endDate,
        @NotNull(message = "청구 주기는 필수입니다.") BillingCycle billingCycle,
        String specialTerms,
        String memo,
        @NotEmpty(message = "계약 품목은 최소 한 개 이상이어야 합니다.") @Valid List<Item> items
) {
    public record Item(
            @NotNull(message = "상품 ID는 필수입니다.") Long productId,
            @NotBlank(message = "상품명은 필수입니다.") String productName,
            @NotBlank(message = "카테고리는 필수입니다.") String productCategory,
            @NotNull(message = "수량은 필수입니다.") @Positive(message = "수량은 1 이상이어야 합니다.") Integer totalQuantity,
            @NotBlank(message = "단위는 필수입니다.") String unit,
            @NotNull(message = "단가는 필수입니다.") @Positive(message = "단가는 0보다 커야 합니다.") BigDecimal unitPrice
    ) {
    }
}
