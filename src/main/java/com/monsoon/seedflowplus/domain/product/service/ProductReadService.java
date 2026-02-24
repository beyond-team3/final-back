package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.Role;
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
    public List<ProductResponse> getAllProducts(Role role) {
        List<Product> products = productRepository.findAll();

        // 권한 체크후 관리자와 영업사원만 가격 정보 출력
        boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

        return products.stream()
                .map(product -> convertToDto(product, canViewPrice))
                .collect(Collectors.toList());
    }

    // 상품 비교하기 페이지 사용
    public List<ProductResponse> getCompareProducts(List<Long> productIds, Role role) {

        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        // 권한 체크후 관리자와 영업사원만 가격 정보 출력
        boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

        return products.stream()
                .map(product -> convertToDto(product, canViewPrice))
                .collect(Collectors.toList());
    }

    // 상품 상세페이지 사용
    public ProductResponse getProductDetail(Long productId, Role role) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

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
