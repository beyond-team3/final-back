package com.monsoon.seedflowplus.domain.deal.log.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogDetailDto;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.core.service.TempUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Deal Logs")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DealLogController {

    private final DealLogQueryService dealLogQueryService;

    @Operation(summary = "Deal 기준 타임라인 조회")
    @GetMapping("/deals/{dealId}/logs")
    public ApiResult<Page<DealLogSummaryDto>> getTimelineByDeal(
            @PathVariable Long dealId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal TempUser user
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ApiResult.success(dealLogQueryService.getTimelineByDeal(dealId, pageable, user));
    }

    @Operation(summary = "Client 기준 타임라인 조회")
    @GetMapping("/clients/{clientId}/logs")
    public ApiResult<Page<DealLogSummaryDto>> getTimelineByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal TempUser user
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ApiResult.success(dealLogQueryService.getTimelineByClient(clientId, pageable, user));
    }

    @Operation(summary = "문서 기준 타임라인 조회")
    @GetMapping("/deal-logs")
    public ApiResult<Page<DealLogSummaryDto>> getTimelineByDocument(
            @RequestParam DealType docType,
            @RequestParam Long refId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal TempUser user
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ApiResult.success(dealLogQueryService.getTimelineByDocument(docType, refId, pageable, user));
    }

    @Operation(summary = "DealLog 상세 조회")
    @GetMapping("/deal-logs/{dealLogId}/detail")
    public ApiResult<DealLogDetailDto> getLogDetail(
            @PathVariable Long dealLogId,
            @AuthenticationPrincipal TempUser user
    ) {
        return ApiResult.success(dealLogQueryService.getLogDetail(dealLogId, user));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SalesDealLogRepository.DEFAULT_TIMELINE_SORT;
        }

        String[] tokens = sort.split(",");
        String property = tokens[0].trim();
        String direction = tokens.length > 1 ? tokens[1].trim() : "desc";

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(new Sort.Order(sortDirection, property));
    }
}
