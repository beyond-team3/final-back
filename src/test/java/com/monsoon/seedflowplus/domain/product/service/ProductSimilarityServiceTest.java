package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.response.SimilarProductResponse;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductSimilarityServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CultivationTimeRepository cultivationTimeRepository;

    @InjectMocks
    private ProductSimilarityService productSimilarityService;

    private static long counter = 1L;

    @Test
    @DisplayName("유사 상품 조회 시 기준 상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getSimilarProducts_ProductNotFound() {
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSimilarityService.getSimilarProducts(productId, 10, 0, null))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("유사 상품 조회 시 같은 카테고리의 후보 상품만 반환한다")
    void getSimilarProducts_ReturnsSameCategoryOnly() {
        Product base = createProduct("WM-001", "수박A", ProductCategory.WATERMELON);
        Product candidate1 = createProduct("WM-002", "수박B", ProductCategory.WATERMELON);
        Product candidate2 = createProduct("PP-001", "고추A", ProductCategory.PEPPER);

        when(productRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(productRepository.findAllByProductCategoryAndIdNot(ProductCategory.WATERMELON, base.getId()))
                .thenReturn(List.of(candidate1));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(candidate1.getId())))
                .thenReturn(List.of());
        when(cultivationTimeRepository.findByProductId(base.getId())).thenReturn(List.of());

        SimilarProductResponse response = productSimilarityService.getSimilarProducts(base.getId(), 10, 0, null);

        assertThat(response.getProductId()).isEqualTo(base.getId());
        // 고추 상품은 포함되지 않아야 함
        assertThat(response.getSimilarProducts()).allMatch(
                item -> !"고추A".equals(item.getProductName())
        );
    }

    @Test
    @DisplayName("유사 상품 조회 시 limit이 0 이하이면 기본값 10이 적용된다")
    void getSimilarProducts_DefaultLimit() {
        Product base = createProduct("WM-003", "수박C", ProductCategory.WATERMELON);

        when(productRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(productRepository.findAllByProductCategoryAndIdNot(ProductCategory.WATERMELON, base.getId()))
                .thenReturn(List.of());
        when(cultivationTimeRepository.findAllByProductIdIn(List.of())).thenReturn(List.of());
        when(cultivationTimeRepository.findByProductId(base.getId())).thenReturn(List.of());

        SimilarProductResponse response = productSimilarityService.getSimilarProducts(base.getId(), 0, 0, null);

        assertThat(response.getSimilarProducts()).isEmpty();
    }

    @Test
    @DisplayName("threshold 적용 시 유사도가 임계값 미만인 상품은 필터링된다")
    void getSimilarProducts_ThresholdFilter() {
        Product base = createProduct("WM-004", "수박D", ProductCategory.WATERMELON);
        Product candidate = createProduct("WM-005", "수박E", ProductCategory.WATERMELON);

        when(productRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(productRepository.findAllByProductCategoryAndIdNot(ProductCategory.WATERMELON, base.getId()))
                .thenReturn(List.of(candidate));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(candidate.getId()))).thenReturn(List.of());
        when(cultivationTimeRepository.findByProductId(base.getId())).thenReturn(List.of());

        // threshold=100 설정 시 유사도가 100% 아니면 모두 필터됨
        SimilarProductResponse response = productSimilarityService.getSimilarProducts(base.getId(), 10, 100, null);

        assertThat(response.getSimilarProducts()).isEmpty();
    }

    @Test
    @DisplayName("재배적기가 완전히 일치하면 유사도 점수가 높게 나온다")
    void getSimilarProducts_HighScoreOnMatchingCultivationTime() {
        Map<String, List<String>> commonTags = Map.of(
                "재배환경", List.of("노지"),
                "내병성", List.of("탄저병"),
                "생육및숙기", List.of("중생"),
                "과실품질", List.of("당도높음"),
                "재배편의성", List.of("관리쉬움")
        );

        Product base = createProductWithTags("WM-006", "수박F", ProductCategory.WATERMELON, commonTags);
        Product candidate = createProductWithTags("WM-007", "수박G", ProductCategory.WATERMELON, commonTags);

        CultivationTime baseCt = CultivationTime.builder()
                .product(base).croppingSystem("노지").region("전남")
                .sowingStart(3).sowingEnd(4)
                .plantingStart(4).plantingEnd(5)
                .harvestingStart(7).harvestingEnd(8)
                .build();

        CultivationTime candidateCt = CultivationTime.builder()
                .product(candidate).croppingSystem("노지").region("전남")
                .sowingStart(3).sowingEnd(4)
                .plantingStart(4).plantingEnd(5)
                .harvestingStart(7).harvestingEnd(8)
                .build();

        when(productRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(productRepository.findAllByProductCategoryAndIdNot(ProductCategory.WATERMELON, base.getId()))
                .thenReturn(List.of(candidate));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(candidate.getId())))
                .thenReturn(List.of(candidateCt));
        when(cultivationTimeRepository.findByProductId(base.getId())).thenReturn(List.of(baseCt));

        SimilarProductResponse response = productSimilarityService.getSimilarProducts(base.getId(), 10, 0, null);

        assertThat(response.getSimilarProducts()).hasSize(1);
        assertThat(response.getSimilarProducts().getFirst().getSimilarityScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("criteria 파라미터로 특정 태그 카테고리만 선택하면 해당 기준으로만 유사도가 계산된다")
    void getSimilarProducts_WithCriteria() {
        Map<String, List<String>> tags = Map.of("재배환경", List.of("노지"), "내병성", List.of("탄저병"));

        Product base = createProductWithTags("WM-008", "수박H", ProductCategory.WATERMELON, tags);
        Product candidate = createProductWithTags("WM-009", "수박I", ProductCategory.WATERMELON, tags);

        when(productRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(productRepository.findAllByProductCategoryAndIdNot(ProductCategory.WATERMELON, base.getId()))
                .thenReturn(List.of(candidate));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(candidate.getId()))).thenReturn(List.of());
        when(cultivationTimeRepository.findByProductId(base.getId())).thenReturn(List.of());

        SimilarProductResponse response = productSimilarityService.getSimilarProducts(
                base.getId(), 10, 0, List.of("env")); // env = 재배환경

        assertThat(response.getSimilarProducts()).hasSize(1);
        assertThat(response.getSimilarProducts().getFirst().getSimilarityScore()).isGreaterThan(0);
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Product createProduct(String code, String name, ProductCategory category) {
        return createProductWithTags(code, name, category, Map.of());
    }

    private Product createProductWithTags(String code, String name, ProductCategory category,
            Map<String, List<String>> tags) {
        Product product = Product.builder()
                .productCode(code)
                .productName(name)
                .productCategory(category)
                .productDescription("설명")
                .amount(100)
                .unit("BOX")
                .price(new BigDecimal("10000"))
                .status(ProductStatus.SALE)
                .tags(tags)
                .build();
        ReflectionTestUtils.setField(product, "id", counter++);
        return product;
    }
}
