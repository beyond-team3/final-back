package com.monsoon.seedflowplus.domain.deal.v2.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.common.DealPaginationConstants;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.PaginationUtils;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDetailDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDocumentSummaryDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealSummaryDto;
import com.monsoon.seedflowplus.domain.deal.v2.service.DealV2QueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Deals V2")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/deals")
public class DealV2QueryController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("lastActivityAt", "closedAt");

    private final DealV2QueryService dealV2QueryService;

    @Operation(summary = "Deal 목록 조회", description = "v2 기준 deal 중심 목록 조회")
    @GetMapping
    public ApiResult<Page<DealSummaryDto>> getDeals(
            @Parameter(description = "담당 영업사원 ID")
            @RequestParam(required = false) Long ownerEmpId,
            @Parameter(description = "거래처 ID")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "현재 Deal 단계")
            @RequestParam(required = false) DealStage currentStage,
            @Parameter(description = "대표 문서 타입")
            @RequestParam(required = false) DealType latestDocType,
            @Parameter(description = "종결 여부")
            @RequestParam(required = false) Boolean isClosed,
            @Parameter(description = "검색어(summaryMemo, latestTargetCode, clientName)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "조회 시작일시(ISO_LOCAL_DATE_TIME)")
            @RequestParam(required = false) String fromAt,
            @Parameter(description = "조회 종료일시(ISO_LOCAL_DATE_TIME)")
            @RequestParam(required = false) String toAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastActivityAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LocalDateTime fromAtParsed = parseDateTime(fromAt, "fromAt");
        LocalDateTime toAtParsed = parseDateTime(toAt, "toAt");
        validateDateRange(fromAtParsed, toAtParsed);

        SalesDealSearchCondition condition = SalesDealSearchCondition.builder()
                .ownerEmpId(ownerEmpId)
                .clientId(clientId)
                .currentStage(currentStage)
                .latestDocType(latestDocType)
                .isClosed(isClosed)
                .keyword(keyword)
                .fromAt(fromAtParsed)
                .toAt(toAtParsed)
                .build();

        Pageable pageable = PaginationUtils.parsePageRequest(
                page,
                size,
                sort,
                Sort.by(Sort.Order.desc("lastActivityAt")),
                ALLOWED_SORT_PROPERTIES,
                DealPaginationConstants.MAX_PAGE_SIZE
        );

        return ApiResult.success(dealV2QueryService.getDeals(condition, pageable, userDetails));
    }

    @Operation(summary = "Deal 상세 조회", description = "deal 중심 상세 조회")
    @GetMapping("/{dealId}")
    public ApiResult<DealDetailDto> getDeal(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(dealV2QueryService.getDeal(dealId, userDetails));
    }

    @Operation(summary = "Deal 문서 목록 조회", description = "특정 deal에 연결된 전체 문서 이력 조회")
    @GetMapping("/{dealId}/documents")
    public ApiResult<List<DealDocumentSummaryDto>> getDealDocuments(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(dealV2QueryService.getDealDocuments(dealId, userDetails));
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(BAD_REQUEST, fieldName + " 형식이 올바르지 않습니다.");
        }
    }

    private void validateDateRange(LocalDateTime fromAt, LocalDateTime toAt) {
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new ResponseStatusException(BAD_REQUEST, "fromAt은 toAt보다 늦을 수 없습니다.");
        }
    }
}
