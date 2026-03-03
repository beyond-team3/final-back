package com.monsoon.seedflowplus.domain.sales.quotation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "견적서 목록 응답")
public record QuotationListResponse(
        @Schema(description = "견적서 ID") Long id,

        @Schema(description = "견적서 코드") String quotationCode,

        @Schema(description = "거래처명") String clientName,

        @Schema(description = "담당 영업사원명") String managerName,

        @Schema(description = "작성일 (YYYY-MM-DD)") @JsonFormat(pattern = "yyyy-MM-dd") LocalDateTime createdAt,

        @Schema(description = "견적서 상태") QuotationStatus status) {
}
