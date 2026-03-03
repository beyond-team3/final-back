package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductContractResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductEstimateReqResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductSearchCondition;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import com.monsoon.seedflowplus.domain.product.entity.ProductCompare;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareRepository;
import com.monsoon.seedflowplus.domain.product.dto.response.CompareHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReadService {

        private final ProductRepository productRepository;
        private final CultivationTimeRepository cultivationTimeRepository;
        private final ProductBookmarkRepository productBookmarkRepository;
        private final ProductCompareRepository productCompareRepository;

        // 상품 전체목록 (검색 조건 적용)
        public List<ProductResponse> getAllProducts(Role role, ProductSearchCondition condition) {
                List<Product> products = productRepository.searchByCondition(condition);

                // 권한 체크후 관리자와 영업사원만 가격 정보 출력
                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                List<Long> productIds = products.stream().map(Product::getId).toList();
                Map<Long, CultivationTime> ctMap = getCultivationTimeMap(productIds);

                return products.stream()
                                .map(product -> convertToDto(product, canViewPrice, ctMap.get(product.getId())))
                                .toList();
        }

        // 사용자의 즐겨찾기 상품 목록 조회
        public List<ProductResponse> getBookmarkedProducts(Long userId, Role role) {
                List<ProductBookmark> bookmarks = productBookmarkRepository.findMyBookmarksWithProduct(userId);

                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                List<Long> productIds = bookmarks.stream().map(b -> b.getProduct().getId()).toList();
                Map<Long, CultivationTime> ctMap = getCultivationTimeMap(productIds);

                return bookmarks.stream()
                                .map(bookmark -> convertToDto(bookmark.getProduct(), canViewPrice,
                                                ctMap.get(bookmark.getProduct().getId())))
                                .toList();
        }

        // 견적서/계약서용 단건 상품 조회 (누구나 열람 가능)
        public ProductContractResponse getProductForContract(Long productId) {
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

                return ProductContractResponse.builder()
                                .productId(product.getId())
                                .productCode(product.getProductCode())
                                .productCategory(product.getProductCategory().getDescription())
                                .productName(product.getProductName())
                                .unit(product.getUnit())
                                .price(product.getPrice())
                                .build();
        }

        // 견적서/계약서용 상품 목록 조회 (누구나 열람 가능)
        public List<ProductContractResponse> getProductsForContract() {
                return productRepository.findAll().stream()
                                .map(product -> ProductContractResponse.builder()
                                                .productId(product.getId())
                                                .productCode(product.getProductCode())
                                                .productCategory(product.getProductCategory().getDescription())
                                                .productName(product.getProductName())
                                                .unit(product.getUnit())
                                                .price(product.getPrice())
                                                .build())
                                .toList();
        }

        // 견적 요청서용 상품 목록 조회
        public List<ProductEstimateReqResponse> getProductsForEstimateReq() {
                return productRepository.findAll().stream()
                                .map(product -> ProductEstimateReqResponse.builder()
                                                .productId(product.getId())
                                                .productCode(product.getProductCode())
                                                .productCategory(product.getProductCategory().getDescription())
                                                .productName(product.getProductName())
                                                .unit(product.getUnit())
                                                .build())
                                .toList();
        }

        // 상품 비교하기 페이지 사용
        public List<ProductResponse> getCompareProducts(List<Long> productIds, Role role) {

                List<Product> products = productRepository.findAllById(productIds);

                if (products.size() != productIds.size()) {
                        throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
                }

                // 권한 체크후 관리자와 영업사원만 가격 정보 출력
                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                Map<Long, CultivationTime> ctMap = getCultivationTimeMap(productIds);

                return products.stream()
                                .map(product -> convertToDto(product, canViewPrice, ctMap.get(product.getId())))
                                .toList();
        }

        // 비교 내역 히스토리 목록 조회
        public List<CompareHistoryResponse> getCompareHistories(Long userId, Role role) {
                List<ProductCompare> histories = productCompareRepository.findAllByAccountIdWithItems(userId);

                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                List<Long> allProductIds = histories.stream()
                                .flatMap(h -> h.getItems().stream())
                                .map(item -> item.getProduct().getId())
                                .distinct()
                                .toList();
                Map<Long, CultivationTime> ctMap = getCultivationTimeMap(allProductIds);

                return histories.stream()
                                .map(history -> CompareHistoryResponse.builder()
                                                .compareId(history.getId())
                                                .title(history.getTitle())
                                                .createdAt(history.getCreatedAt())
                                                .products(history.getItems().stream()
                                                                .map(item -> convertToDto(item.getProduct(),
                                                                                canViewPrice,
                                                                                ctMap.get(item.getProduct().getId())))
                                                                .toList())
                                                .build())
                                .toList();
        }

        // 유사 상품 추천 (동일 카테고리 최대 5개 반환 - DB 단에서 쿼리 처리)
        public List<ProductResponse> getSimilarProducts(Long productId, Role role) {
                Product currentProduct = productRepository.findById(productId)
                                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

                // 메모리에서 필터/limit을 적용하지 않고, Repository 레벨에 쿼리를 위임
                List<Product> similarProducts = productRepository
                                .findTop5ByProductCategoryAndIdNotOrderByIdDesc(currentProduct.getProductCategory(),
                                                productId);

                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                List<Long> productIds = similarProducts.stream().map(Product::getId).toList();
                Map<Long, CultivationTime> ctMap = getCultivationTimeMap(productIds);

                return similarProducts.stream()
                                .map(product -> convertToDto(product, canViewPrice, ctMap.get(product.getId())))
                                .toList();
        }

        // 상품 상세페이지 사용
        public ProductResponse getProductDetail(Long productId, Role role) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                return convertToDto(product, canViewPrice);
        }

        private Map<Long, CultivationTime> getCultivationTimeMap(List<Long> productIds) {
                if (productIds == null || productIds.isEmpty()) {
                        return Collections.emptyMap();
                }
                return cultivationTimeRepository.findAllByProductIdIn(productIds).stream()
                                .collect(Collectors.toMap(ct -> ct.getProduct().getId(), ct -> ct));
        }

        private ProductResponse convertToDto(Product product, boolean canViewPrice) {
                return convertToDto(product, canViewPrice,
                                cultivationTimeRepository.findByProductId(product.getId()).orElse(null));
        }

        private ProductResponse convertToDto(Product product, boolean canViewPrice, CultivationTime ct) {
                ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                                .id(product.getId())
                                .category(product.getProductCategory().name())
                                .name(product.getProductName())
                                .description(product.getProductDescription())
                                .imageUrl(product.getProductImageUrl())
                                .tags(product.getTags());

                if (ct != null) {
                        builder.cultivationTime(CultivationTimeDto.builder()
                                        .sowingStart(ct.getSowingStart())
                                        .sowingEnd(ct.getSowingEnd())
                                        .plantingStart(ct.getPlantingStart())
                                        .plantingEnd(ct.getPlantingEnd())
                                        .harvestingStart(ct.getHarvestingStart())
                                        .harvestingEnd(ct.getHarvestingEnd())
                                        .build());
                }

                if (canViewPrice) {
                        builder.priceData(new ProductResponse.PriceData(
                                        product.getAmount(),
                                        product.getPrice(),
                                        product.getUnit()));
                }

                return builder.build();
        }
}
