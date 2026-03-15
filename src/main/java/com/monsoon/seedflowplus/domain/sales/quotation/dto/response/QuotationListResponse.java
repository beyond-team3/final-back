package com.monsoon.seedflowplus.domain.sales.quotation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "견적서 목록 응답")
public record QuotationListResponse(
        @Schema(description = "견적서 ID") Long id,

        @Schema(description = "견적서 코드") String quotationCode,

        @Schema(description = "거래처 ID") Long clientId,

        @Schema(description = "거래처명") String clientName,

        @Schema(description = "담당 영업사원명") String managerName,

        @Schema(description = "작성자 ID") Long authorId,

        @Schema(description = "작성일 (YYYY-MM-DD)") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,

        @Schema(description = "견적서 상태") QuotationStatus status,

        @Schema(description = "참조 견적 요청서 ID") Long requestId,

        @Schema(description = "영업 딜(Deal) ID") Long dealId,

        @Schema(description = "내부 비고(메모)") String memo,

        @Schema(description = "견적 요청서 요구사항") String requirements,

        @Schema(description = "반려 사유") String rejectionReason,

        @Schema(description = "견적 품목 목록") List<QuotationResponse.QuotationItemResponse> items) {
}
