package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.ClientCrop;
import com.monsoon.seedflowplus.domain.account.repository.ClientCropRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
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
import com.monsoon.seedflowplus.domain.product.entity.ProductTag;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReadService {

        private final ProductRepository productRepository;
        private final CultivationTimeRepository cultivationTimeRepository;
        private final ProductBookmarkRepository productBookmarkRepository;
        private final ProductCompareRepository productCompareRepository;
        private final ProductTagRepository productTagRepository;
        private final ClientRepository clientRepository;
        private final ClientCropRepository clientCropRepository;

        // 상품 전체목록 (검색 조건 적용)
        public List<ProductResponse> getAllProducts(Role role, ProductSearchCondition condition) {
                List<Product> products = productRepository.searchByCondition(condition);

                // 권한 체크후 관리자와 영업사원만 가격 정보 출력
                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                List<Long> productIds = products.stream().map(Product::getId).toList();
                Map<Long, List<CultivationTime>> ctMap = getCultivationTimeMap(productIds);

                return products.stream()
                                .map(product -> convertToDto(product, canViewPrice, ctMap.get(product.getId())))
                                .toList();
        }

        // 사용자의 즐겨찾기 상품 목록 조회
        public List<ProductResponse> getBookmarkedProducts(Long userId, Role role) {
                List<ProductBookmark> bookmarks = productBookmarkRepository.findMyBookmarksWithProduct(userId);

                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                List<Long> productIds = bookmarks.stream().map(b -> b.getProduct().getId()).toList();
                Map<Long, List<CultivationTime>> ctMap = getCultivationTimeMap(productIds);

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

                Map<Long, List<CultivationTime>> ctMap = getCultivationTimeMap(productIds);

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
                Map<Long, List<CultivationTime>> ctMap = getCultivationTimeMap(allProductIds);

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

        // 상품 상세페이지 사용
        public ProductResponse getProductDetail(Long productId, Role role) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

                boolean canViewPrice = (role == Role.ADMIN) || (role == Role.SALES_REP);

                return convertToDto(product, canViewPrice);
        }

        private Map<Long, List<CultivationTime>> getCultivationTimeMap(List<Long> productIds) {
                if (productIds == null || productIds.isEmpty()) {
                        return Collections.emptyMap();
                }
                return cultivationTimeRepository.findAllByProductIdIn(productIds).stream()
                                .collect(Collectors.groupingBy(ct -> ct.getProduct().getId()));
        }

        public List<CultivationTimeDto> getCultivationTimes(Long productId) {
                return cultivationTimeRepository.findByProductId(productId).stream()
                                .map(ct -> CultivationTimeDto.builder()
                                                .croppingSystem(ct.getCroppingSystem())
                                                .region(ct.getRegion())
                                                .sowingStart(ct.getSowingStart())
                                                .sowingEnd(ct.getSowingEnd())
                                                .plantingStart(ct.getPlantingStart())
                                                .plantingEnd(ct.getPlantingEnd())
                                                .harvestingStart(ct.getHarvestingStart())
                                                .harvestingEnd(ct.getHarvestingEnd())
                                                .build())
                                .toList();
        }

        public ProductCalendarRecommendationResponse getCalendarRecommendations(Integer month) {
                int targetMonth = resolveMonth(month);
                List<Product> products = productRepository.findAll();
                Map<Long, List<CultivationTime>> cultivationTimeMap = getCultivationTimeMap(
                                products.stream().map(Product::getId).toList());

                List<ProductCalendarRecommendationResponse.RecommendedProductItem> items = products.stream()
                                .map(product -> buildRecommendationItem(product, cultivationTimeMap.get(product.getId()),
                                                targetMonth))
                                .flatMap(Optional::stream)
                                .sorted(Comparator
                                                .comparing(
                                                                ProductCalendarRecommendationResponse.RecommendedProductItem::getPlantingStart,
                                                                Comparator.nullsLast(Integer::compareTo))
                                                .thenComparing(
                                                                ProductCalendarRecommendationResponse.RecommendedProductItem::getProductCategoryLabel,
                                                                Comparator.nullsLast(String::compareTo))
                                                .thenComparing(
                                                                ProductCalendarRecommendationResponse.RecommendedProductItem::getProductName,
                                                                Comparator.nullsLast(String::compareTo)))
                                .toList();

                return ProductCalendarRecommendationResponse.builder()
                                .month(targetMonth)
                                .items(items)
                                .build();
        }

        public ProductHarvestImminentResponse getHarvestImminent(Integer month, CustomUserDetails userDetails) {
                int targetMonth = resolveMonth(month);
                int nextMonth = targetMonth == 12 ? 1 : targetMonth + 1;
                Long employeeId = resolveSalesRepEmployeeId(userDetails);

                List<Client> managedClients = clientRepository.findAllByManagerEmployeeId(employeeId);
                if (managedClients.isEmpty()) {
                        return ProductHarvestImminentResponse.builder()
                                        .month(targetMonth)
                                        .nextMonth(nextMonth)
                                        .clients(List.of())
                                        .build();
                }

                List<Long> clientIds = managedClients.stream().map(Client::getId).toList();
                Map<Long, List<ClientCrop>> clientCropMap = clientCropRepository.findAllByClientIdIn(clientIds).stream()
                                .collect(Collectors.groupingBy(crop -> crop.getClient().getId()));

                List<Product> products = productRepository.findAll();
                Map<Long, List<CultivationTime>> cultivationTimeMap = getCultivationTimeMap(
                                products.stream().map(Product::getId).toList());

                List<ProductHarvestImminentResponse.ClientHarvestImminentItem> clientItems = managedClients.stream()
                                .map(client -> buildHarvestClientItem(client, clientCropMap.get(client.getId()), products,
                                                cultivationTimeMap, targetMonth, nextMonth))
                                .flatMap(Optional::stream)
                                .sorted(Comparator.comparing(
                                                ProductHarvestImminentResponse.ClientHarvestImminentItem::getClientName,
                                                Comparator.nullsLast(String::compareTo)))
                                .toList();

                return ProductHarvestImminentResponse.builder()
                                .month(targetMonth)
                                .nextMonth(nextMonth)
                                .clients(clientItems)
                                .build();
        }

        private ProductResponse convertToDto(Product product, boolean canViewPrice) {
                return convertToDto(product, canViewPrice,
                                cultivationTimeRepository.findByProductId(product.getId()));
        }

        private ProductResponse convertToDto(Product product, boolean canViewPrice, List<CultivationTime> ctList) {
                
                // 1. 태그 정보 조회 (ProductTagRepository 활용)
                List<ProductTag> productTags = Collections.emptyList();
                try {
                    productTags = productTagRepository.findAllByProduct_Id(product.getId());
                } catch (Exception e) { /* 태그 조회 실패 시 빈 목록으로 처리 */ }
                
                Map<String, List<String>> tagMap = productTags.stream()
                        .collect(Collectors.groupingBy(
                                pt -> pt.getTag().getCategoryCode().toLowerCase(),
                                Collectors.mapping(pt -> pt.getTag().getTagName(), Collectors.toList())
                        ));
                
                // 연관 테이블에 태그가 비어있다면, 기존 JSON 컬럼 참조(하위호환성 유지)
                if (tagMap.isEmpty() && product.getTags() != null) {
                    Map<String, List<String>> lowerTags = new java.util.HashMap<>();
                    for (Map.Entry<String, List<String>> entry : product.getTags().entrySet()) {
                        lowerTags.put(entry.getKey().toLowerCase(), entry.getValue());
                    }
                    tagMap = lowerTags;
                }

                ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                                .id(product.getId())
                                .category(product.getProductCategory().name())
                                .name(product.getProductName())
                                .description(product.getProductDescription())
                                .imageUrl(product.getProductImageUrl())
                                .tags(tagMap);

                if (ctList != null && !ctList.isEmpty()) {
                        builder.cultivationTimes(ctList.stream().map(ct -> new CultivationTimeDto(
                                        ct.getCroppingSystem(),
                                        ct.getRegion(),
                                        ct.getSowingStart(),
                                        ct.getSowingEnd(),
                                        ct.getPlantingStart(),
                                        ct.getPlantingEnd(),
                                        ct.getHarvestingStart(),
                                        ct.getHarvestingEnd())
                        ).collect(Collectors.toList()));
                }

                if (canViewPrice) {
                        builder.priceData(new ProductResponse.PriceData(
                                        product.getAmount(),
                                        product.getPrice(),
                                        product.getUnit()));
                }

                return builder.build();
        }

        private Optional<ProductCalendarRecommendationResponse.RecommendedProductItem> buildRecommendationItem(
                        Product product,
                        List<CultivationTime> cultivationTimes,
                        int targetMonth) {
                if (cultivationTimes == null || cultivationTimes.isEmpty()) {
                        return Optional.empty();
                }

                return cultivationTimes.stream()
                                .filter(ct -> isBetweenInclusive(targetMonth, ct.getSowingStart(), ct.getPlantingStart()))
                                .min(Comparator.comparing(CultivationTime::getPlantingStart,
                                                Comparator.nullsLast(Integer::compareTo)))
                                .map(ct -> ProductCalendarRecommendationResponse.RecommendedProductItem.builder()
                                                .productId(product.getId())
                                                .productName(product.getProductName())
                                                .productCategory(product.getProductCategory().name())
                                                .productCategoryLabel(product.getProductCategory().getDescription())
                                                .description(product.getProductDescription())
                                                .imageUrl(product.getProductImageUrl())
                                                .sowingStart(ct.getSowingStart())
                                                .plantingStart(ct.getPlantingStart())
                                                .croppingSystem(ct.getCroppingSystem())
                                                .region(ct.getRegion())
                                                .build());
        }

        private Optional<ProductHarvestImminentResponse.ClientHarvestImminentItem> buildHarvestClientItem(
                        Client client,
                        List<ClientCrop> clientCrops,
                        List<Product> products,
                        Map<Long, List<CultivationTime>> cultivationTimeMap,
                        int targetMonth,
                        int nextMonth) {
                if (clientCrops == null || clientCrops.isEmpty()) {
                        return Optional.empty();
                }

                List<ProductHarvestImminentResponse.CropHarvestImminentItem> cropItems = clientCrops.stream()
                                .map(clientCrop -> buildHarvestCropItem(clientCrop, products, cultivationTimeMap,
                                                targetMonth, nextMonth))
                                .flatMap(Optional::stream)
                                .sorted(Comparator.comparing(
                                                ProductHarvestImminentResponse.CropHarvestImminentItem::getCropName,
                                                Comparator.nullsLast(String::compareTo)))
                                .toList();

                if (cropItems.isEmpty()) {
                        return Optional.empty();
                }

                return Optional.of(ProductHarvestImminentResponse.ClientHarvestImminentItem.builder()
                                .clientId(client.getId())
                                .clientName(client.getClientName())
                                .crops(cropItems)
                                .build());
        }

        private Optional<ProductHarvestImminentResponse.CropHarvestImminentItem> buildHarvestCropItem(
                        ClientCrop clientCrop,
                        List<Product> products,
                        Map<Long, List<CultivationTime>> cultivationTimeMap,
                        int targetMonth,
                        int nextMonth) {
                LinkedHashMap<Long, ProductHarvestImminentResponse.HarvestProductItem> matchedProducts = new LinkedHashMap<>();

                for (Product product : products) {
                        if (!matchesClientCrop(clientCrop.getCropName(), product)) {
                                continue;
                        }

                        pickHarvestWindow(product, cultivationTimeMap.get(product.getId()), targetMonth, nextMonth)
                                        .ifPresent(item -> matchedProducts.put(product.getId(), item));
                }

                if (matchedProducts.isEmpty()) {
                        return Optional.empty();
                }

                List<ProductHarvestImminentResponse.HarvestProductItem> items = matchedProducts.values().stream()
                                .sorted(Comparator
                                                .comparing(
                                                                ProductHarvestImminentResponse.HarvestProductItem::getHarvestingStart,
                                                                Comparator.nullsLast(Integer::compareTo))
                                                .thenComparing(
                                                                ProductHarvestImminentResponse.HarvestProductItem::getProductName,
                                                                Comparator.nullsLast(String::compareTo)))
                                .toList();

                return Optional.of(ProductHarvestImminentResponse.CropHarvestImminentItem.builder()
                                .cropName(clientCrop.getCropName())
                                .matchedProducts(items)
                                .build());
        }

        private Optional<ProductHarvestImminentResponse.HarvestProductItem> pickHarvestWindow(
                        Product product,
                        List<CultivationTime> cultivationTimes,
                        int targetMonth,
                        int nextMonth) {
                if (cultivationTimes == null || cultivationTimes.isEmpty()) {
                        return Optional.empty();
                }

                return cultivationTimes.stream()
                                .filter(ct -> isHarvestImminent(ct, targetMonth, nextMonth))
                                .sorted(Comparator
                                                .comparing((CultivationTime ct) -> harvestPriority(ct, targetMonth))
                                                .thenComparing(CultivationTime::getHarvestingStart,
                                                                Comparator.nullsLast(Integer::compareTo)))
                                .findFirst()
                                .map(ct -> ProductHarvestImminentResponse.HarvestProductItem.builder()
                                                .productId(product.getId())
                                                .productName(product.getProductName())
                                                .productCategory(product.getProductCategory().name())
                                                .productCategoryLabel(product.getProductCategory().getDescription())
                                                .imageUrl(product.getProductImageUrl())
                                                .harvestingStart(ct.getHarvestingStart())
                                                .harvestingEnd(ct.getHarvestingEnd())
                                                .croppingSystem(ct.getCroppingSystem())
                                                .region(ct.getRegion())
                                                .build());
        }

        private boolean matchesClientCrop(String cropName, Product product) {
                String normalizedCropName = normalize(cropName);
                if (normalizedCropName.isEmpty()) {
                        return false;
                }

                String categoryLabel = normalize(product.getProductCategory().getDescription());
                String productName = normalize(product.getProductName());
                String categoryName = normalize(product.getProductCategory().name());

                return normalizedCropName.equals(categoryLabel)
                                || normalizedCropName.equals(categoryName)
                                || productName.contains(normalizedCropName)
                                || normalizedCropName.contains(productName);
        }

        private boolean isHarvestImminent(CultivationTime cultivationTime, int targetMonth, int nextMonth) {
                Integer harvestingStart = cultivationTime.getHarvestingStart();
                Integer harvestingEnd = cultivationTime.getHarvestingEnd();
                if (harvestingStart == null || harvestingEnd == null) {
                        return false;
                }
                return isBetweenInclusive(targetMonth, harvestingStart, harvestingEnd)
                                || isBetweenInclusive(nextMonth, harvestingStart, harvestingEnd);
        }

        private int harvestPriority(CultivationTime cultivationTime, int targetMonth) {
                return isBetweenInclusive(targetMonth, cultivationTime.getHarvestingStart(),
                                cultivationTime.getHarvestingEnd()) ? 0 : 1;
        }

        private boolean isBetweenInclusive(int targetMonth, Integer start, Integer end) {
                if (start == null || end == null) {
                        return false;
                }
                return start <= targetMonth && targetMonth <= end;
        }

        private int resolveMonth(Integer month) {
                return month != null ? month : LocalDate.now().getMonthValue();
        }

        private Long resolveSalesRepEmployeeId(CustomUserDetails userDetails) {
                if (userDetails == null || userDetails.getRole() != Role.SALES_REP) {
                        throw new CoreException(ErrorType.ACCESS_DENIED);
                }
                if (userDetails.getEmployeeId() == null) {
                        throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
                }
                return userDetails.getEmployeeId();
        }

        private String normalize(String value) {
                if (value == null) {
                        return "";
                }
                return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        }
}
