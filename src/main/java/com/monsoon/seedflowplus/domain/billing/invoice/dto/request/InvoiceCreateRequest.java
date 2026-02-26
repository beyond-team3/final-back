package com.monsoon.seedflowplus.domain.billing.invoice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class InvoiceCreateRequest {

    @NotNull(message = "계약 ID는 필수입니다.")
    private Long contractId;

    @NotNull(message = "청구 시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "청구 종료일은 필수입니다.")
    private LocalDate endDate;

    private String memo;

    @AssertTrue(message = "청구 시작일은 종료일보다 늦을 수 없습니다.")
    private boolean isDateRangeValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}