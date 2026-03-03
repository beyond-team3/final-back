package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductUpdateParam;
import com.monsoon.seedflowplus.domain.product.entity.*;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductPriceHistoryRepository;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductWriteService {

    private final ProductRepository productRepository;
    private final ProductBookmarkRepository productBookmarkRepository;
    private final TagService tagService;
    private final ProductTagRepository productTagRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    private final CultivationTimeRepository cultivationTimeRepository;
    private final ProductCompareItemRepository productCompareItemRepository;
    private final UserRepository userRepository;
    private final ProductCompareRepository productCompareRepository;

    @Transactional
    public Long createProduct(ProductRequest request) {

        ProductCategory category = ProductCategory.valueOf(request.getProductCategory());
        String generatedCode = generateProductCode(category);

        Product newProduct = Product.builder()
                .productCode(generatedCode)
                .productName(request.getProductName())
                .productCategory(category) // 위에서 변환한 category 객체 재사용
                .productDescription(request.getProductDescription())
                .productImageUrl(request.getProductImageUrl())
                .amount(request.getAmount())
                .unit(request.getUnit())
                .price(request.getPrice())
                .status(ProductStatus.valueOf(request.getStatus())) // String -> Enum
                .tags(request.getTags())
                .build();

        try {
            // saveAndFlush를 사용하여 유니크 검사
            Product savedProduct = productRepository.saveAndFlush(newProduct);

            // 재배적기 정보가 있다면 저장
            if (request.getCultivationTime() != null) {
                CultivationTimeDto ctDto = request.getCultivationTime();
                CultivationTime cultivationTime = CultivationTime.builder()
                        .product(savedProduct)
                        .sowingStart(ctDto.getSowingStart())
                        .sowingEnd(ctDto.getSowingEnd())
                        .plantingStart(ctDto.getPlantingStart())
                        .plantingEnd(ctDto.getPlantingEnd())
                        .harvestingStart(ctDto.getHarvestingStart())
                        .harvestingEnd(ctDto.getHarvestingEnd())
                        .build();
                cultivationTimeRepository.save(cultivationTime);
            }

            updateProductTags(savedProduct, request.getTags());
            return savedProduct.getId();

        } catch (DataIntegrityViolationException e) {
            // 동시에 다른 사람이 똑같은 코드를 등록해서 DB 유니크 에러가 터지면 중복 에러 고지
            throw new CoreException(ErrorType.DUPLICATE_PRODUCT_CODE);
        }
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest request, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }

        // 가격 변동 검사 및 이력 저장
        if (product.getPrice().compareTo(request.getPrice()) != 0) {
            ProductPriceHistory history = ProductPriceHistory.builder()
                    .product(product)
                    .oldPrice(product.getPrice())
                    .newPrice(request.getPrice())
                    .modifiedBy(employee)
                    .build();
            productPriceHistoryRepository.save(history);
        }

        ProductUpdateParam param = new ProductUpdateParam(
                request.getProductName(),
                request.getProductCategory(),
                request.getProductDescription(),
                request.getProductImageUrl(),
                request.getAmount(),
                request.getUnit(),
                request.getPrice(),
                request.getStatus(),
                request.getTags(),
                request.getCultivationTime());

        // 찾은 엔티티의 정보 업데이트 (엔티티 내부의 수정 메서드 호출)
        product.updateProduct(param, param.tags());

        updateProductTags(product, param.tags());
        updateCultivationTime(product, param.cultivationTime());
    }

    private void updateCultivationTime(Product product, CultivationTimeDto ctDto) {
        CultivationTime currentCt = cultivationTimeRepository.findByProductId(product.getId()).orElse(null);

        // DTO가 없는 경우
        if (ctDto == null) {
            if (currentCt != null) {
                cultivationTimeRepository.delete(currentCt);
            }
            return;
        }

        // 기존 데이터가 있다면 삭제 후 재등록 (또는 엔티티 더티 체킹 활용 가능)
        if (currentCt != null) {
            cultivationTimeRepository.delete(currentCt);
            cultivationTimeRepository.flush(); // 즉시 삭제 반영 (유니크 키 제약조건 위반 방지)
        }

        CultivationTime newCt = CultivationTime.builder()
                .product(product)
                .sowingStart(ctDto.getSowingStart())
                .sowingEnd(ctDto.getSowingEnd())
                .plantingStart(ctDto.getPlantingStart())
                .plantingEnd(ctDto.getPlantingEnd())
                .harvestingStart(ctDto.getHarvestingStart())
                .harvestingEnd(ctDto.getHarvestingEnd())
                .build();
        cultivationTimeRepository.save(newCt);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        // 비교 분석 내역 삭제
        productCompareItemRepository.deleteAllByProductId(productId);

        // 즐겨찾기 데이터 삭제
        productBookmarkRepository.deleteAllByProductId(productId);

        // 즐겨찾기 데이터 삭제 후 상품 삭제
        productRepository.delete(product);
    }

    private void updateProductTags(Product product, Map<String, List<String>> tagMap) {

        if (tagMap == null) {
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

    // 상품 코드 생성
    private String generateProductCode(ProductCategory category) {

        // 카테고리 약자
        String categoryStr = category.getCode();

        // 생성 연도 뒤 2자리
        String yearStr = String.valueOf(java.time.Year.now().getValue()).substring(2); // 2026 -> 26

        int nextSequence = 1;

        Optional<Product> lastProduct = productRepository.findTopByProductCategoryOrderByIdDesc(category);

        if (lastProduct.isPresent()) {

            String lastCode = lastProduct.get().getProductCode();

            String[] parts = lastCode.split("-");

            if (parts.length == 3) {
                try {
                    int lastSequence = Integer.parseInt(parts[2]);
                    nextSequence = lastSequence + 1;
                } catch (NumberFormatException e) {
                    // 코드 형식이 예상과 다를 경우 시퀀스 1로 시작 (시스템 마비 방지)
                    nextSequence = 1;
                }
            }
        }
        return String.format("%s-%s-%02d", categoryStr, yearStr, nextSequence);
    }

    // 비교 내역 저장
    @Transactional
    public Long saveCompareHistory(Long userId, List<Long> productIds, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        ProductCompare compare = ProductCompare.builder()
                .account(user)
                .title(title != null ? title : products.get(0).getProductName() + " 등 " + products.size() + "건 비교")
                .build();

        List<ProductCompareItem> items = products.stream()
                .map(p -> ProductCompareItem.builder().product(p).build())
                .toList();

        compare.addItems(items);
        ProductCompare saved = productCompareRepository.save(compare);
        return saved.getId();
    }

    // 비교 내역 삭제
    @Transactional
    public void deleteCompareHistory(Long userId, Long compareId) {
        ProductCompare compare = productCompareRepository.findById(compareId)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_INPUT_VALUE));

        if (!compare.getAccount().getId().equals(userId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        productCompareRepository.delete(compare);
    }
}
