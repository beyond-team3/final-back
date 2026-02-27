package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductContractResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductEstimateReqResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReadServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    @SuppressWarnings("unused")
    private CultivationTimeRepository cultivationTimeRepository;

    @InjectMocks
    private ProductReadService productReadService;

    @Test
    @DisplayName("관리자 권한의 전체 상품 조회 시 가격 정보가 포함되어야 한다")
    void testGetAllProducts_Admin() {
        // given
        Product product = createDummyProduct("P001", "사과 씨앗", new BigDecimal("50000"));
        when(productRepository.searchByCondition(null)).thenReturn(List.of(product));

        // when
        List<ProductResponse> responses = productReadService.getAllProducts(Role.ADMIN, null);

        // then
        assertThat(responses).hasSize(1);
        ProductResponse response = responses.getFirst();
        assertThat(response.getName()).isEqualTo("사과 씨앗");
        assertThat(response.getPriceData()).isNotNull();
        assertThat(response.getPriceData().getPrice()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("일반 사용자 권한의 전체 상품 조회 시 가격 정보가 제외되어야 한다")
    void testGetAllProducts_Client() {
        // given
        Product product = createDummyProduct("P002", "배 씨앗", new BigDecimal("60000"));
        when(productRepository.searchByCondition(null)).thenReturn(List.of(product));

        // when
        List<ProductResponse> responses = productReadService.getAllProducts(Role.CLIENT, null);

        // then
        assertThat(responses).hasSize(1);
        ProductResponse response = responses.getFirst();
        assertThat(response.getPriceData()).isNull(); // CLIENT는 가격 정보를 볼 수 없음
    }

    @Test
    @DisplayName("계약서용 상품 조회 시 계약 전용 DTO를 반환한다")
    void testGetProductsForContract() {
        // given
        Product product = createDummyProduct("P003", "포도 씨앗", new BigDecimal("70000"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        // when
        List<ProductContractResponse> responses = productReadService.getProductsForContract();

        // then
        assertThat(responses).hasSize(1);
        ProductContractResponse response = responses.getFirst();
        assertThat(response.getProductName()).isEqualTo("포도 씨앗");
        assertThat(response.getPrice()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("견적 요청서용 상품 조회 시 단가가 제외된 DTO를 반환한다")
    void testGetProductsForEstimateReq() {
        // given
        Product product = createDummyProduct("P004", "키위 씨앗", new BigDecimal("80000"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        // when
        List<ProductEstimateReqResponse> responses = productReadService.getProductsForEstimateReq();

        // then
        assertThat(responses).hasSize(1);
        ProductEstimateReqResponse response = responses.getFirst();
        assertThat(response.getProductName()).isEqualTo("키위 씨앗");
        // 견적 요청서는 price 필드가 존재하지 않으므로 검증(Null 체크) 불필요 (타입 자체에 없음)
    }

    private static long counter = 1L;

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
}
