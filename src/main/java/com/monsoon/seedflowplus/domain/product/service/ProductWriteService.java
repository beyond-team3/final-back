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
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.notification.event.ProductCreatedEvent;
import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareItemRepository;
import com.monsoon.seedflowplus.infra.aws.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
    private final S3UploadService s3UploadService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public Long createProduct(ProductRequest request, MultipartFile productImage) {

        // S3 이미지 업로드 처리 로직 추가
        String uploadedImageUrl = null;
        boolean isNewlyUploaded = false;

        if (productImage != null && !productImage.isEmpty()) {
            uploadedImageUrl = s3UploadService.uploadProductImage(productImage);
            isNewlyUploaded = true; // S3에 요금이 부과되는 새 파일이 올라갔음을 표시
        } else {
            uploadedImageUrl = request.getProductImageUrl();
        }

        ProductCategory category = ProductCategory.valueOf(request.getProductCategory());
        String generatedCode = generateProductCode(category);

        Product newProduct = Product.builder()
                .productCode(generatedCode)
                .productName(request.getProductName())
                .productCategory(category)
                .productDescription(request.getProductDescription())
                .productImageUrl(uploadedImageUrl)
                .amount(request.getAmount())
                .unit(request.getUnit())
                .price(request.getPrice())
                .status(ProductStatus.valueOf(request.getStatus()))
                .tags(request.getTags())
                .build();

        try {
            // saveAndFlush를 사용하여 유니크 검사
            Product savedProduct = productRepository.saveAndFlush(newProduct);

            // 재배적기 정보가 있다면 저장
            if (request.getCultivationTimes() != null && !request.getCultivationTimes().isEmpty()) {
                if (request.getCultivationTimes().stream().anyMatch(java.util.Objects::isNull)) {
                    throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
                }
                List<CultivationTime> ctList = request.getCultivationTimes().stream()
                        .map(ctDto -> CultivationTime.builder()
                                .product(savedProduct)
                                .croppingSystem(ctDto.getCroppingSystem())
                                .region(ctDto.getRegion())
                                .sowingStart(ctDto.getSowingStart())
                                .sowingEnd(ctDto.getSowingEnd())
                                .plantingStart(ctDto.getPlantingStart())
                                .plantingEnd(ctDto.getPlantingEnd())
                                .harvestingStart(ctDto.getHarvestingStart())
                                .harvestingEnd(ctDto.getHarvestingEnd())
                                .build())
                        .toList();
                cultivationTimeRepository.saveAll(ctList);
            }

            updateProductTags(savedProduct, request.getTags());
            publishProductCreatedNotifications(savedProduct);
            return savedProduct.getId();

        } catch (DataIntegrityViolationException e) {
            // 유니크 에러(중복)가 터졌을 때 보상 로직 실행
            if (isNewlyUploaded) {
                s3UploadService.deleteImageFromUrl(uploadedImageUrl); // S3에서 사진 삭-제!
            }
            throw new CoreException(ErrorType.DUPLICATE_PRODUCT_CODE);

        } catch (Exception e) {
            // 그 외에 알 수 없는 DB 에러가 터졌을 때도 안전하게 삭제
            if (isNewlyUploaded) {
                s3UploadService.deleteImageFromUrl(uploadedImageUrl);
            }
            throw e; // 원래 발생한 에러를 그대로 다시 던짐
        }
    }

    private void publishProductCreatedNotifications(Product savedProduct) {
        userRepository.findAllByRole(Role.SALES_REP).stream()
                .map(User::getId)
                .distinct()
                .forEach(userId -> notificationEventPublisher.publishAfterCommit(new ProductCreatedEvent(
                        userId,
                        savedProduct.getId(),
                        savedProduct.getProductCode(),
                        savedProduct.getProductName(),
                        java.time.LocalDateTime.now()
                )));
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest request, MultipartFile productImage, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }

        String uploadedImageUrl = product.getProductImageUrl();
        if (productImage != null && !productImage.isEmpty()) {
            uploadedImageUrl = s3UploadService.uploadProductImage(productImage);
        } else if (request.getProductImageUrl() == null || request.getProductImageUrl().isEmpty()) {
            uploadedImageUrl = null;
        } else if (!request.getProductImageUrl().startsWith("data:image")) {
            uploadedImageUrl = request.getProductImageUrl();
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
                uploadedImageUrl,
                request.getAmount(),
                request.getUnit(),
                request.getPrice(),
                request.getStatus(),
                request.getTags(),
                request.getCultivationTimes());

        // 찾은 엔티티의 정보 업데이트 (엔티티 내부의 수정 메서드 호출)
        product.updateProduct(param, param.tags());

        updateProductTags(product, param.tags());
        updateCultivationTime(product, param.cultivationTimes());
    }

    private void updateCultivationTime(Product product, List<CultivationTimeDto> ctDtoList) {
        // 기존 데이터 조회 후 개별 삭제 + flush 하여 EntityManager 동기화 보장
        List<CultivationTime> currentCts = cultivationTimeRepository.findByProductId(product.getId());
        if (currentCts != null && !currentCts.isEmpty()) {
            cultivationTimeRepository.deleteAll(currentCts);
            cultivationTimeRepository.flush();
        }

        // 새 데이터가 없으면 종료
        if (ctDtoList == null || ctDtoList.isEmpty()) {
            return;
        }

        if (ctDtoList.stream().anyMatch(java.util.Objects::isNull)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        // 기존 데이터 삭제 후 새 데이터 재등록
        for (CultivationTimeDto ctDto : ctDtoList) {
            CultivationTime ct = CultivationTime.builder()
                    .product(product)
                    .croppingSystem(ctDto.getCroppingSystem())
                    .region(ctDto.getRegion())
                    .sowingStart(ctDto.getSowingStart())
                    .sowingEnd(ctDto.getSowingEnd())
                    .plantingStart(ctDto.getPlantingStart())
                    .plantingEnd(ctDto.getPlantingEnd())
                    .harvestingStart(ctDto.getHarvestingStart())
                    .harvestingEnd(ctDto.getHarvestingEnd())
                    .build();
            cultivationTimeRepository.save(ct);
        }
        cultivationTimeRepository.flush();
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

        // 기존 매핑 삭제 후 즉시 flush하여 delete가 DB에 반영된 뒤 insert가 실행되도록 보장
        productTagRepository.deleteByProduct_Id(product.getId());
        productTagRepository.flush();

        if (tagMap.isEmpty()) {
            return;
        }

        List<ProductTag> newProductTags = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : tagMap.entrySet()) {
            String categoryCode = entry.getKey();

            for (String tagName : entry.getValue()) {
                // 태그 생성/조회 책임 TagService로 위임
                Tag tag = tagService.getOrCreateTag(categoryCode, tagName);

                if (tag != null) { // 유효한 태그인 경우에만 매핑
                    newProductTags.add(ProductTag.builder()
                            .product(product)
                            .tag(tag)
                            .build());
                }
            }
        }

        if (!newProductTags.isEmpty()) {
            productTagRepository.saveAll(newProductTags);
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
        if (productIds == null || productIds.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        // null 원소 제거 및 중복 제거
        List<Long> distinctIds = productIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        List<Product> products = productRepository.findAllById(distinctIds);
        if (products.size() != distinctIds.size()) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        ProductCompare compare = ProductCompare.builder()
                .account(user)
                .title(title != null && !title.isBlank() ? title
                        : products.get(0).getProductName() + " 등 " + products.size() + "건 비교")
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
