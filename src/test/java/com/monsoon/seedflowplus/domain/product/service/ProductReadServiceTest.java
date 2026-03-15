package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientCrop;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.repository.ClientCropRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductContractResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductEstimateReqResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReadServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CultivationTimeRepository cultivationTimeRepository;

    @Mock
    private ProductTagRepository productTagRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientCropRepository clientCropRepository;

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

    @Test
    @DisplayName("계약서용 특정 상품 단건 조회 시 올바른 DTO를 반환한다")
    void testGetProductForContract() {
        // given
        Long productId = 10L;
        Product product = createDummyProduct("P005", "참외 씨앗", new BigDecimal("45000"));
        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));

        // when
        ProductContractResponse response = productReadService.getProductForContract(productId);

        // then
        assertThat(response.getProductName()).isEqualTo("참외 씨앗");
        assertThat(response.getPrice()).isEqualByComparingTo("45000");
    }

    @Test
    @DisplayName("캘린더 추천 품종 조회 시 sowingStart와 plantingStart 사이의 상품만 반환한다")
    void getCalendarRecommendationsFiltersByMonthWindow() {
        Product recommended = createDummyProduct("P006", "봄 수박", new BigDecimal("12000"));
        Product excluded = createDummyProduct("P007", "여름 배추", new BigDecimal("9000"));

        when(productRepository.findAll()).thenReturn(List.of(recommended, excluded));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(recommended.getId(), excluded.getId())))
                .thenReturn(List.of(
                        createCultivationTime(recommended, 3, 4, 7, 8, "노지", "전남"),
                        createCultivationTime(excluded, 1, 2, 5, 6, "하우스", "경남")
                ));

        ProductCalendarRecommendationResponse response = productReadService.getCalendarRecommendations(3);

        assertThat(response.getMonth()).isEqualTo(3);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getProductName()).isEqualTo("봄 수박");
        assertThat(response.getItems().getFirst().getSowingStart()).isEqualTo(3);
        assertThat(response.getItems().getFirst().getPlantingStart()).isEqualTo(4);
    }

    @Test
    @DisplayName("수확 임박 조회 시 담당 거래처의 취급 품종과 매칭된 상품만 반환한다")
    void getHarvestImminentReturnsManagedClientMatches() {
        Client client = createClient("거래처A");
        ReflectionTestUtils.setField(client, "id", 1L);

        ClientCrop crop = ClientCrop.builder()
                .cropName("수박")
                .client(client)
                .build();
        ReflectionTestUtils.setField(crop, "id", 10L);

        Product matched = createDummyProduct("P008", "프리미엄 수박", new BigDecimal("15000"));
        Product notImminent = createDummyProduct("P009", "늦수확 수박", new BigDecimal("13000"));

        when(clientRepository.findAllByManagerEmployeeId(7L)).thenReturn(List.of(client));
        when(clientCropRepository.findAllByClientIdIn(List.of(1L))).thenReturn(List.of(crop));
        when(productRepository.findAll()).thenReturn(List.of(matched, notImminent));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(matched.getId(), notImminent.getId())))
                .thenReturn(List.of(
                        createCultivationTime(matched, 2, 3, 3, 4, "노지", "전북"),
                        createCultivationTime(notImminent, 2, 3, 6, 7, "노지", "경북")
                ));

        ProductHarvestImminentResponse response = productReadService.getHarvestImminent(3, salesRepPrincipal(7L));

        assertThat(response.getMonth()).isEqualTo(3);
        assertThat(response.getNextMonth()).isEqualTo(4);
        assertThat(response.getClients()).hasSize(1);
        assertThat(response.getClients().getFirst().getClientName()).isEqualTo("거래처A");
        assertThat(response.getClients().getFirst().getCrops()).hasSize(1);
        assertThat(response.getClients().getFirst().getCrops().getFirst().getCropName()).isEqualTo("수박");
        assertThat(response.getClients().getFirst().getCrops().getFirst().getMatchedProducts()).hasSize(1);
        assertThat(response.getClients().getFirst().getCrops().getFirst().getMatchedProducts().getFirst().getProductName())
                .isEqualTo("프리미엄 수박");
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

    private CultivationTime createCultivationTime(Product product, Integer sowingStart, Integer plantingStart,
                                                  Integer harvestingStart, Integer harvestingEnd,
                                                  String croppingSystem, String region) {
        CultivationTime cultivationTime = CultivationTime.builder()
                .product(product)
                .croppingSystem(croppingSystem)
                .region(region)
                .sowingStart(sowingStart)
                .sowingEnd(sowingStart)
                .plantingStart(plantingStart)
                .plantingEnd(plantingStart)
                .harvestingStart(harvestingStart)
                .harvestingEnd(harvestingEnd)
                .build();
        ReflectionTestUtils.setField(cultivationTime, "id", counter++);
        return cultivationTime;
    }

    private Client createClient(String clientName) {
        return Client.builder()
                .clientCode("CLNT-0001")
                .clientName(clientName)
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울시")
                .clientType(ClientType.NURSERY)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .build();
    }

    private CustomUserDetails salesRepPrincipal(Long employeeId) {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.SALES_REP);
        when(principal.getEmployeeId()).thenReturn(employeeId);
        return principal;
    }
}
