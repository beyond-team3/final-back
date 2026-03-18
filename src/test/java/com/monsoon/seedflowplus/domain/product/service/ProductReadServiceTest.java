package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;
import com.monsoon.seedflowplus.domain.product.dto.response.CompareHistoryResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductContractResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductEstimateReqResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductCompare;
import com.monsoon.seedflowplus.domain.product.entity.ProductCompareItem;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
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
class ProductReadServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CultivationTimeRepository cultivationTimeRepository;

    @Mock
    private ProductBookmarkRepository productBookmarkRepository;

    @Mock
    private ProductCompareRepository productCompareRepository;

    @Mock
    private ProductTagRepository productTagRepository;

    @Mock
    private ProductCultivationAlertService productCultivationAlertService;

    @InjectMocks
    private ProductReadService productReadService;

    private static long counter = 1L;

    // ─── 전체 상품 조회 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자 권한의 전체 상품 조회 시 가격 정보가 포함되어야 한다")
    void testGetAllProductsAdmin() {
        Product product = createDummyProduct("P001", "사과 씨앗", new BigDecimal("50000"));
        when(productRepository.searchByCondition(null)).thenReturn(List.of(product));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId()))).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(List.of(product.getId()))).thenReturn(List.of());

        List<ProductResponse> responses = productReadService.getAllProducts(Role.ADMIN, null);

        assertThat(responses).hasSize(1);
        ProductResponse response = responses.getFirst();
        assertThat(response.getName()).isEqualTo("사과 씨앗");
        assertThat(response.getPriceData()).isNotNull();
        assertThat(response.getPriceData().getPrice()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("일반 사용자 권한의 전체 상품 조회 시 가격 정보가 제외되어야 한다")
    void testGetAllProductsClient() {
        Product product = createDummyProduct("P002", "배 씨앗", new BigDecimal("60000"));
        when(productRepository.searchByCondition(null)).thenReturn(List.of(product));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId()))).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(List.of(product.getId()))).thenReturn(List.of());

        List<ProductResponse> responses = productReadService.getAllProducts(Role.CLIENT, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getPriceData()).isNull();
    }

    @Test
    @DisplayName("영업사원 권한의 전체 상품 조회 시 가격 정보가 포함되어야 한다")
    void testGetAllProductsSalesRep() {
        Product product = createDummyProduct("P003", "포도 씨앗", new BigDecimal("30000"));
        when(productRepository.searchByCondition(null)).thenReturn(List.of(product));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId()))).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(List.of(product.getId()))).thenReturn(List.of());

        List<ProductResponse> responses = productReadService.getAllProducts(Role.SALES_REP, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getPriceData()).isNotNull();
    }

    // ─── 계약서 / 견적서용 조회 ─────────────────────────────────────────────────

    @Test
    @DisplayName("계약서용 상품 조회 시 계약 전용 DTO를 반환한다")
    void testGetProductsForContract() {
        Product product = createDummyProduct("P004", "포도 씨앗", new BigDecimal("70000"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductContractResponse> responses = productReadService.getProductsForContract();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getProductName()).isEqualTo("포도 씨앗");
        assertThat(responses.getFirst().getPrice()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("견적 요청서용 상품 조회 시 단가가 제외된 DTO를 반환한다")
    void testGetProductsForEstimateReq() {
        Product product = createDummyProduct("P005", "키위 씨앗", new BigDecimal("80000"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductEstimateReqResponse> responses = productReadService.getProductsForEstimateReq();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getProductName()).isEqualTo("키위 씨앗");
    }

    @Test
    @DisplayName("계약서용 특정 상품 단건 조회 시 올바른 DTO를 반환한다")
    void testGetProductForContract() {
        Long productId = 10L;
        Product product = createDummyProduct("P006", "참외 씨앗", new BigDecimal("45000"));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductContractResponse response = productReadService.getProductForContract(productId);

        assertThat(response.getProductName()).isEqualTo("참외 씨앗");
        assertThat(response.getPrice()).isEqualByComparingTo("45000");
    }

    @Test
    @DisplayName("존재하지 않는 상품 단건 조회 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void testGetProductForContract_NotFound() {
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productReadService.getProductForContract(productId))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PRODUCT_NOT_FOUND);
    }

    // ─── 상품 상세 조회 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("상품 상세 조회 시 관리자는 가격 정보를 포함한 DTO를 반환한다")
    void testGetProductDetail_Admin() {
        Long productId = 20L;
        Product product = createDummyProduct("P007", "수박 씨앗", new BigDecimal("55000"));
        ReflectionTestUtils.setField(product, "id", productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cultivationTimeRepository.findByProductId(productId)).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_Id(productId)).thenReturn(List.of());

        ProductResponse response = productReadService.getProductDetail(productId, Role.ADMIN);

        assertThat(response.getName()).isEqualTo("수박 씨앗");
        assertThat(response.getPriceData()).isNotNull();
        assertThat(response.getPriceData().getPrice()).isEqualByComparingTo("55000");
    }

    @Test
    @DisplayName("상품 상세 조회 시 거래처는 가격 정보가 null인 DTO를 반환한다")
    void testGetProductDetail_Client() {
        Long productId = 21L;
        Product product = createDummyProduct("P008", "고추 씨앗", new BigDecimal("20000"));
        ReflectionTestUtils.setField(product, "id", productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cultivationTimeRepository.findByProductId(productId)).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_Id(productId)).thenReturn(List.of());

        ProductResponse response = productReadService.getProductDetail(productId, Role.CLIENT);

        assertThat(response.getPriceData()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 상품 상세 조회 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void testGetProductDetail_NotFound() {
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productReadService.getProductDetail(productId, Role.ADMIN))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PRODUCT_NOT_FOUND);
    }

    // ─── 즐겨찾기 상품 조회 ────────────────────────────────────────────────────

    @Test
    @DisplayName("즐겨찾기 상품 목록 조회 시 해당 유저의 북마크 상품이 반환된다")
    void testGetBookmarkedProducts() {
        Long userId = 5L;
        Product product = createDummyProduct("P009", "오이 씨앗", new BigDecimal("15000"));
        ProductBookmark bookmark = mock(ProductBookmark.class);
        when(bookmark.getProduct()).thenReturn(product);

        when(productBookmarkRepository.findMyBookmarksWithProduct(userId)).thenReturn(List.of(bookmark));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId()))).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(List.of(product.getId()))).thenReturn(List.of());

        List<ProductResponse> responses = productReadService.getBookmarkedProducts(userId, Role.SALES_REP);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getName()).isEqualTo("오이 씨앗");
        assertThat(responses.getFirst().getPriceData()).isNotNull();
    }

    @Test
    @DisplayName("즐겨찾기 상품 조회 시 거래처는 가격 정보가 포함되지 않는다")
    void testGetBookmarkedProducts_Client() {
        Long userId = 6L;
        Product product = createDummyProduct("P010", "토마토 씨앗", new BigDecimal("25000"));
        ProductBookmark bookmark = mock(ProductBookmark.class);
        when(bookmark.getProduct()).thenReturn(product);

        when(productBookmarkRepository.findMyBookmarksWithProduct(userId)).thenReturn(List.of(bookmark));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId()))).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(List.of(product.getId()))).thenReturn(List.of());

        List<ProductResponse> responses = productReadService.getBookmarkedProducts(userId, Role.CLIENT);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getPriceData()).isNull();
    }

    // ─── 상품 비교 조회 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("비교 상품 조회 시 요청한 ID 목록에 맞는 상품 목록이 반환된다")
    void testGetCompareProducts() {
        Product p1 = createDummyProduct("P011", "수박A", new BigDecimal("10000"));
        Product p2 = createDummyProduct("P012", "수박B", new BigDecimal("12000"));
        List<Long> productIds = List.of(p1.getId(), p2.getId());

        when(productRepository.findAllById(productIds)).thenReturn(List.of(p1, p2));
        when(cultivationTimeRepository.findAllByProductIdIn(productIds)).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(productIds)).thenReturn(List.of());

        List<ProductResponse> responses = productReadService.getCompareProducts(productIds, Role.ADMIN);

        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("비교 상품 조회 시 존재하지 않는 상품 ID가 포함되면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void testGetCompareProducts_NotFound() {
        List<Long> productIds = List.of(100L, 200L);
        when(productRepository.findAllById(productIds)).thenReturn(List.of(
                createDummyProduct("P013", "수박C", new BigDecimal("10000"))
        ));

        assertThatThrownBy(() -> productReadService.getCompareProducts(productIds, Role.ADMIN))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PRODUCT_NOT_FOUND);
    }

    // ─── 비교 내역 히스토리 조회 ────────────────────────────────────────────────

    @Test
    @DisplayName("비교 내역 조회 시 유저 ID에 해당하는 히스토리 목록이 반환된다")
    void testGetCompareHistories() {
        Long userId = 7L;
        Product product = createDummyProduct("P014", "수박D", new BigDecimal("10000"));

        ProductCompareItem item = mock(ProductCompareItem.class);
        when(item.getProduct()).thenReturn(product);

        ProductCompare history = mock(ProductCompare.class);
        when(history.getId()).thenReturn(1L);
        when(history.getTitle()).thenReturn("테스트 비교");
        when(history.getCreatedAt()).thenReturn(null);
        when(history.getItems()).thenReturn(List.of(item));

        when(productCompareRepository.findAllByAccountIdWithItems(userId)).thenReturn(List.of(history));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId()))).thenReturn(List.of());
        when(productTagRepository.findAllByProduct_IdIn(List.of(product.getId()))).thenReturn(List.of());

        List<CompareHistoryResponse> responses = productReadService.getCompareHistories(userId, Role.ADMIN);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getTitle()).isEqualTo("테스트 비교");
        assertThat(responses.getFirst().getProducts()).hasSize(1);
    }

    // ─── 재배적기 조회 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("재배적기 조회 시 해당 상품의 재배적기 DTO 목록이 반환된다")
    void testGetCultivationTimes() {
        Long productId = 30L;
        Product product = createDummyProduct("P015", "수박E", new BigDecimal("10000"));
        CultivationTime ct = CultivationTime.builder()
                .product(product)
                .croppingSystem("노지")
                .region("전남")
                .sowingStart(3)
                .sowingEnd(4)
                .plantingStart(4)
                .plantingEnd(5)
                .harvestingStart(7)
                .harvestingEnd(8)
                .build();
        when(cultivationTimeRepository.findByProductId(productId)).thenReturn(List.of(ct));

        List<CultivationTimeDto> dtos = productReadService.getCultivationTimes(productId);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.getFirst().getCroppingSystem()).isEqualTo("노지");
        assertThat(dtos.getFirst().getRegion()).isEqualTo("전남");
        assertThat(dtos.getFirst().getHarvestingStart()).isEqualTo(7);
    }

    // ─── 캘린더 / 수확 임박 ────────────────────────────────────────────────────

    @Test
    @DisplayName("캘린더 추천 품종 조회는 공통 재배 집계 서비스를 호출한다")
    void getCalendarRecommendationsDelegatesToAlertService() {
        ProductCalendarRecommendationResponse expected = ProductCalendarRecommendationResponse.builder()
                .month(3)
                .items(List.of())
                .build();
        when(productCultivationAlertService.getCalendarRecommendations(3)).thenReturn(expected);

        ProductCalendarRecommendationResponse response = productReadService.getCalendarRecommendations(3);

        assertThat(response).isSameAs(expected);
        verify(productCultivationAlertService).getCalendarRecommendations(3);
    }

    @Test
    @DisplayName("수확 임박 조회는 SALES_REP principal의 employeeId를 공통 집계 서비스에 전달한다")
    void getHarvestImminentDelegatesToAlertService() {
        ProductHarvestImminentResponse expected = ProductHarvestImminentResponse.builder()
                .month(3)
                .nextMonth(4)
                .clients(List.of())
                .build();
        when(productCultivationAlertService.getHarvestImminent(3, 7L)).thenReturn(expected);

        ProductHarvestImminentResponse response = productReadService.getHarvestImminent(3, salesRepPrincipal(7L));

        assertThat(response).isSameAs(expected);
        verify(productCultivationAlertService).getHarvestImminent(3, 7L);
    }

    @Test
    @DisplayName("수확 임박 조회 시 SALES_REP 외 권한이면 ACCESS_DENIED 예외가 발생한다")
    void getHarvestImminent_AccessDenied_ForNonSalesRep() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);

        assertThatThrownBy(() -> productReadService.getHarvestImminent(3, principal))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("수확 임박 조회 시 employeeId가 null이면 EMPLOYEE_NOT_LINKED 예외가 발생한다")
    void getHarvestImminent_EmployeeNotLinked() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.SALES_REP);
        when(principal.getEmployeeId()).thenReturn(null);

        assertThatThrownBy(() -> productReadService.getHarvestImminent(3, principal))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.EMPLOYEE_NOT_LINKED);
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Product createDummyProduct(String code, String name, BigDecimal price) {
        Product product = Product.builder()
                .productCode(code)
                .productName(name)
                .productCategory(ProductCategory.WATERMELON)
                .productDescription("설명")
                .amount(100)
                .unit("BOX")
                .price(price)
                .status(ProductStatus.SALE)
                .tags(Map.of())
                .build();
        ReflectionTestUtils.setField(product, "id", counter++);
        return product;
    }

    private CustomUserDetails salesRepPrincipal(Long employeeId) {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.SALES_REP);
        when(principal.getEmployeeId()).thenReturn(employeeId);
        return principal;
    }
}
