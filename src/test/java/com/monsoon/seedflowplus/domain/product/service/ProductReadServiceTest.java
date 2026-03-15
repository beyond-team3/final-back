package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductContractResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductEstimateReqResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private ProductTagRepository productTagRepository;

    @Mock
    private ProductCultivationAlertService productCultivationAlertService;

    @InjectMocks
    private ProductReadService productReadService;

    private static long counter = 1L;

    @Test
    @DisplayName("관리자 권한의 전체 상품 조회 시 가격 정보가 포함되어야 한다")
    void testGetAllProductsAdmin() {
        Product product = createDummyProduct("P001", "사과 씨앗", new BigDecimal("50000"));
        when(productRepository.searchByCondition(null)).thenReturn(List.of(product));

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

        List<ProductResponse> responses = productReadService.getAllProducts(Role.CLIENT, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getPriceData()).isNull();
    }

    @Test
    @DisplayName("계약서용 상품 조회 시 계약 전용 DTO를 반환한다")
    void testGetProductsForContract() {
        Product product = createDummyProduct("P003", "포도 씨앗", new BigDecimal("70000"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductContractResponse> responses = productReadService.getProductsForContract();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getProductName()).isEqualTo("포도 씨앗");
        assertThat(responses.getFirst().getPrice()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("견적 요청서용 상품 조회 시 단가가 제외된 DTO를 반환한다")
    void testGetProductsForEstimateReq() {
        Product product = createDummyProduct("P004", "키위 씨앗", new BigDecimal("80000"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductEstimateReqResponse> responses = productReadService.getProductsForEstimateReq();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getProductName()).isEqualTo("키위 씨앗");
    }

    @Test
    @DisplayName("계약서용 특정 상품 단건 조회 시 올바른 DTO를 반환한다")
    void testGetProductForContract() {
        Long productId = 10L;
        Product product = createDummyProduct("P005", "참외 씨앗", new BigDecimal("45000"));
        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));

        ProductContractResponse response = productReadService.getProductForContract(productId);

        assertThat(response.getProductName()).isEqualTo("참외 씨앗");
        assertThat(response.getPrice()).isEqualByComparingTo("45000");
    }

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
