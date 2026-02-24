package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReadService {

    private final ProductRepository productRepository;

    // 상품 전체목록
    public List<ProductResponse> getAllProducts(String Role) {
        List<Product> products = productRepository.findAll();

        // 권한 체크후 관리자와 영업사원만 가격 정보 출력
        boolean canViewPrice = "ADMIN".equals(Role) || "SALES_REP".equals(Role);

        return products.stream()
                .map(product -> convertToDto(product, canViewPrice))
                .collect(Collectors.toList());
    }

    // 상품 비교하기 페이지 사용
    public List<ProductResponse> getCompareProducts(List<Long> productIds, String role) {

        List<Product> products = productRepository.findAllById(productIds);

        // 권한 체크후 관리자와 영업사원만 가격 정보 출력
        boolean canViewPrice = "ADMIN".equals(role) || "SALES_REP".equals(role);

        return products.stream()
                .map(product -> convertToDto(product, canViewPrice))
                .collect(Collectors.toList());
    }

    // 상품 상세페이지 사용
    public ProductResponse getProductDetail(Long productId, String role) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        boolean canViewPrice = "ADMIN".equals(role) || "SALES_REP".equals(role);

        return convertToDto(product, canViewPrice);
    }

    private ProductResponse convertToDto(Product product, boolean canViewPrice) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .category(product.getProductCategory().name())
                .name(product.getProductName())
                .description(product.getProductDescription())
                .imageUrl(product.getProductImageUrl())
                .tags(product.getTags());

        if (canViewPrice) {
            builder.priceData(new ProductResponse.PriceData(
                    product.getAmount(),
                    product.getPrice(),
                    product.getUnit()
            ));
        }

        return builder.build();
    }
}
