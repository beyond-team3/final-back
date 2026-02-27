package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.domain.product.dto.response.ProductPriceHistoryResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductPriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}/price-histories")
@RequiredArgsConstructor
public class ProductPriceHistoryController {

    private final ProductPriceHistoryService productPriceHistoryService;

    @GetMapping
    public ResponseEntity<List<ProductPriceHistoryResponse>> getPriceHistories(@PathVariable Long productId) {
        List<ProductPriceHistoryResponse> responses = productPriceHistoryService.getPriceHistories(productId);
        return ResponseEntity.ok(responses);
    }
}
