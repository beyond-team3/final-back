package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class CultivationTimeController {

    private final ProductReadService productReadService;

    @GetMapping("/{productId}/cultivation-time")
    @Operation(summary = "상품 재배적기 단독 조회", description = "특정 상품의 작형 및 지역별 재배적기 목록을 반환합니다.")
    public ResponseEntity<List<CultivationTimeDto>> getCultivationTimes(@PathVariable Long productId) {
        List<CultivationTimeDto> response = productReadService.getCultivationTimes(productId);
        return ResponseEntity.ok(response);
    }
}
