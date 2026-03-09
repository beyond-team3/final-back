package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.domain.product.dto.response.ProductContractResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductEstimateReqResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductDocumentController {

    private final ProductReadService productReadService;

    // 견적서, 계약서용 상품 목록 조회
    @GetMapping("/doc/contract")
    public ResponseEntity<List<ProductContractResponse>> getProductsForContract() {
        List<ProductContractResponse> responses = productReadService.getProductsForContract();
        return ResponseEntity.ok(responses);
    }

    // 견적요청서 용 상품 목록 조회
    @GetMapping("/doc/estimate")
    public ResponseEntity<List<ProductEstimateReqResponse>> getProductsForEstimateReq() {
        List<ProductEstimateReqResponse> responses = productReadService.getProductsForEstimateReq();
        return ResponseEntity.ok(responses);
    }

    // 특정 상품 정보 조회
    @GetMapping("/doc/contract/{productId}")
    public ResponseEntity<ProductContractResponse> getProductForContract(
            @org.springframework.web.bind.annotation.PathVariable Long productId) {
        ProductContractResponse response = productReadService.getProductForContract(productId);
        return ResponseEntity.ok(response);
    }
}
