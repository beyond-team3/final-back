package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products/calendar")
@RequiredArgsConstructor
public class ProductCalendarController {

    private final ProductReadService productReadService;

    @GetMapping("/recommendations")
    public ApiResult<ProductCalendarRecommendationResponse> getRecommendations(
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return ApiResult.success(productReadService.getCalendarRecommendations(month));
    }

    @GetMapping("/harvest-imminent")
    public ApiResult<ProductHarvestImminentResponse> getHarvestImminent(
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResult.success(productReadService.getHarvestImminent(month, userDetails));
    }
}
