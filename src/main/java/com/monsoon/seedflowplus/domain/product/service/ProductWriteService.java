package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductUpdateParam;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductWriteService {

    private final ProductRepository productRepository;
    private final ProductBookmarkRepository productBookmarkRepository;

    @Transactional
    public Long createProduct(ProductRequest request) {
        Product newProduct = Product.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .productCategory(ProductCategory.valueOf(request.getProductCategory())) // String -> Enum
                .productDescription(request.getProductDescription())
                .productImageUrl(request.getProductImageUrl())
                .amount(request.getAmount())
                .unit(request.getUnit())
                .price(request.getPrice())
                .status(ProductStatus.valueOf(request.getStatus())) // String -> Enum
                .tags(request.getTags())
                .build();

        // 저장 후 생성된 상품의 ID 반환
        return productRepository.save(newProduct).getId();
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        ProductUpdateParam param = new ProductUpdateParam(
                request.getProductName(),
                request.getProductCategory(),
                request.getProductDescription(),
                request.getProductImageUrl(),
                request.getAmount(),
                request.getUnit(),
                request.getPrice(),
                request.getStatus(),
                request.getTags()
        );
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        // 즐겨찾기 데이터 삭제
        productBookmarkRepository.deleteAllByProductId(productId);

        // 즐겨찾기 데이터 삭제 후 상품 삭제
        productRepository.delete(product);
    }
}
