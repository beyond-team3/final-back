package com.monsoon.seedflowplus.domain.deal.core.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.SalesDealListItemDto;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import com.monsoon.seedflowplus.domain.deal.core.service.SalesDealQueryService;
import com.monsoon.seedflowplus.domain.deal.core.service.TempUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Tag(name = "Deals")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deals")
public class SalesDealQueryController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SalesDealQueryService salesDealQueryService;

    @Operation(
            summary = "모든 문서(딜) 목록 조회",
            description = "SalesDeal 스냅샷 기반 목록 조회. 역할에 따라 조회 범위가 자동 제한됨."
    )
    @GetMapping
    public ApiResult<Page<SalesDealListItemDto>> getDeals(
            @Parameter(description = "담당 영업사원 ID")
            @RequestParam(required = false) Long ownerEmpId,
            @Parameter(description = "거래처 ID")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "현재 Deal 단계")
            @RequestParam(required = false) DealStage currentStage,
            @Parameter(description = "최신 문서 타입")
            @RequestParam(required = false) DealType latestDocType,
            @Parameter(description = "종결 여부(true: closedAt not null, false: closedAt null)")
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
            // TODO: 실제 인증 Principal 타입이 확정되면 TempUser를 해당 타입으로 교체
            @AuthenticationPrincipal TempUser user
    ) {
        SalesDealSearchCondition condition = SalesDealSearchCondition.builder()
                .ownerEmpId(ownerEmpId)
                .clientId(clientId)
                .currentStage(currentStage)
                .latestDocType(latestDocType)
                .isClosed(isClosed)
                .keyword(keyword)
                .fromAt(parseDateTime(fromAt, "fromAt"))
                .toAt(parseDateTime(toAt, "toAt"))
                .build();

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        return ApiResult.success(salesDealQueryService.getDealsForCurrentUser(condition, pageable, user));
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

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("lastActivityAt"));
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
