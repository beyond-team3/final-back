package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductUpdateParam;
import com.monsoon.seedflowplus.domain.product.entity.*;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductWriteService {

    private final ProductRepository productRepository;
    private final ProductBookmarkRepository productBookmarkRepository;
    private final TagService tagService;
    private final ProductTagRepository productTagRepository;

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

        // 찾은 엔티티의 정보 업데이트 (엔티티 내부의 수정 메서드 호출)
        product.updateProduct(param, param.tags());

        updateProductTags(product, param.tags());
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

    private void updateProductTags(Product product, Map<String, List<String>> tagMap) {
        productTagRepository.deleteByProduct_Id(product.getId());

        if (tagMap == null ) {
            return;
        }

        productTagRepository.deleteByProduct_Id(product.getId());

        if (tagMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : tagMap.entrySet()) {
            String categoryCode = entry.getKey();

            for (String tagName : entry.getValue()) {
                // 태그 생성/조회 책임 TagService로 위임
                Tag tag = tagService.getOrCreateTag(categoryCode, tagName);

                if (tag != null) { // 유효한 태그인 경우에만 매핑
                    productTagRepository.save(ProductTag.builder()
                            .product(product)
                            .tag(tag)
                            .build());
                }
            }
        }
    }
}
