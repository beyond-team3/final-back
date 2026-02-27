package com.monsoon.seedflowplus.domain.deal.log.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.common.PaginationUtils;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogDetailDto;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.core.service.TempUser;
import com.monsoon.seedflowplus.domain.deal.core.service.TempUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Deal Logs")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DealLogController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("actionAt", "targetCode", "id");

    private final DealLogQueryService dealLogQueryService;
    private final TempUserResolver tempUserResolver;

    @Operation(summary = "Deal 기준 타임라인 조회")
    @GetMapping("/deals/{dealId}/logs")
    public ApiResult<Page<DealLogSummaryDto>> getTimelineByDeal(
            @PathVariable @Positive Long dealId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Positive int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TempUser user = tempUserResolver.resolve(userDetails);
        Pageable pageable = PaginationUtils.parsePageRequest(
                page,
                size,
                sort,
                SalesDealLogRepository.DEFAULT_TIMELINE_SORT,
                ALLOWED_SORT_PROPERTIES,
                MAX_PAGE_SIZE
        );
        return ApiResult.success(dealLogQueryService.getTimelineByDeal(dealId, pageable, user));
    }

    @Operation(summary = "Client 기준 타임라인 조회")
    @GetMapping("/clients/{clientId}/logs")
    public ApiResult<Page<DealLogSummaryDto>> getTimelineByClient(
            @PathVariable @Positive Long clientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Positive int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TempUser user = tempUserResolver.resolve(userDetails);
        Pageable pageable = PaginationUtils.parsePageRequest(
                page,
                size,
                sort,
                SalesDealLogRepository.DEFAULT_TIMELINE_SORT,
                ALLOWED_SORT_PROPERTIES,
                MAX_PAGE_SIZE
        );
        return ApiResult.success(dealLogQueryService.getTimelineByClient(clientId, pageable, user));
    }

    @Operation(summary = "문서 기준 타임라인 조회")
    @GetMapping("/deal-logs")
    public ApiResult<Page<DealLogSummaryDto>> getTimelineByDocument(
            @RequestParam DealType docType,
            @RequestParam @Positive Long refId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Positive int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TempUser user = tempUserResolver.resolve(userDetails);
        Pageable pageable = PaginationUtils.parsePageRequest(
                page,
                size,
                sort,
                SalesDealLogRepository.DEFAULT_TIMELINE_SORT,
                ALLOWED_SORT_PROPERTIES,
                MAX_PAGE_SIZE
        );
        return ApiResult.success(dealLogQueryService.getTimelineByDocument(docType, refId, pageable, user));
    }

    @Operation(summary = "DealLog 상세 조회")
    @GetMapping("/deal-logs/{dealLogId}/detail")
    public ApiResult<DealLogDetailDto> getLogDetail(
            @PathVariable @Positive Long dealLogId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TempUser user = tempUserResolver.resolve(userDetails);
        return ApiResult.success(dealLogQueryService.getLogDetail(dealLogId, user));
    }
}
