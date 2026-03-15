package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientCrop;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientCropRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class ProductCultivationAlertServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CultivationTimeRepository cultivationTimeRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientCropRepository clientCropRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductCultivationAlertService productCultivationAlertService;

    private static long counter = 1L;

    @Test
    @DisplayName("캘린더 추천 품종 조회 시 sowingStart와 plantingStart 사이의 상품만 반환한다")
    void getCalendarRecommendationsFiltersByMonthWindow() {
        Product recommended = createProduct("P006", "봄 수박");
        Product excluded = createProduct("P007", "여름 수박");

        when(productRepository.findAll()).thenReturn(List.of(recommended, excluded));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(recommended.getId(), excluded.getId())))
                .thenReturn(List.of(
                        createCultivationTime(recommended, 3, 4, 7, 8, "노지", "전남"),
                        createCultivationTime(excluded, 1, 2, 5, 6, "하우스", "경남")
                ));

        ProductCalendarRecommendationResponse response = productCultivationAlertService.getCalendarRecommendations(3);

        assertThat(response.getMonth()).isEqualTo(3);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getProductName()).isEqualTo("봄 수박");
    }

    @Test
    @DisplayName("수확 임박 조회 시 담당 거래처의 취급 품종과 매칭된 상품만 반환한다")
    void getHarvestImminentReturnsManagedClientMatches() {
        Client client = createClient("거래처A", 7L);
        ClientCrop crop = createClientCrop(client, "수박");
        Product matched = createProduct("P008", "프리미엄 수박");
        Product notImminent = createProduct("P009", "늦수확 수박");

        when(clientRepository.findAllByManagerEmployeeId(7L)).thenReturn(List.of(client));
        when(clientCropRepository.findAllByClientIdIn(List.of(client.getId()))).thenReturn(List.of(crop));
        when(productRepository.findAll()).thenReturn(List.of(matched, notImminent));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(matched.getId(), notImminent.getId())))
                .thenReturn(List.of(
                        createCultivationTime(matched, 2, 3, 3, 4, "노지", "전북"),
                        createCultivationTime(notImminent, 2, 3, 6, 7, "노지", "경북")
                ));

        ProductHarvestImminentResponse response = productCultivationAlertService.getHarvestImminent(3, 7L);

        assertThat(response.getClients()).hasSize(1);
        assertThat(response.getClients().getFirst().getCrops()).hasSize(1);
        assertThat(response.getClients().getFirst().getCrops().getFirst().getMatchedProducts()).hasSize(1);
        assertThat(response.getClients().getFirst().getCrops().getFirst().getMatchedProducts().getFirst().getProductName())
                .isEqualTo("프리미엄 수박");
    }

    @Test
    @DisplayName("알림 후보 계산 시 같은 상품을 여러 거래처가 취급해도 사용자당 상품 1건만 생성한다")
    void getNotificationCandidatesAggregatesByUserAndProduct() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 15, 1, 0);
        User salesRep = createSalesRepUser(100L, 7L);
        Client clientA = createClient("거래처A", 7L);
        Client clientB = createClient("거래처B", 7L);
        ClientCrop cropA = createClientCrop(clientA, "수박");
        ClientCrop cropB = createClientCrop(clientB, "수박");
        Product product = createProduct("P010", "대표 수박");

        when(userRepository.findAllByRole(Role.SALES_REP)).thenReturn(List.of(salesRep));
        when(clientRepository.findAllByManagerEmployeeId(7L)).thenReturn(List.of(clientA, clientB));
        when(clientCropRepository.findAllByClientIdIn(List.of(clientA.getId(), clientB.getId())))
                .thenReturn(List.of(cropA, cropB));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId())))
                .thenReturn(List.of(createCultivationTime(product, 4, 5, 7, 8, "노지", "전남")));

        List<CultivationNotificationCandidate> candidates = productCultivationAlertService.getNotificationCandidates(now);

        assertThat(candidates).hasSize(1);
        CultivationNotificationCandidate candidate = candidates.getFirst();
        assertThat(candidate.getType()).isEqualTo(NotificationType.CULTIVATION_SOWING_PROMOTION);
        assertThat(candidate.getUserId()).isEqualTo(100L);
        assertThat(candidate.getProductId()).isEqualTo(product.getId());
        assertThat(candidate.getClientCount()).isEqualTo(2);
        assertThat(candidate.getReferenceMonth()).isEqualTo(4);
        assertThat(candidate.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 9, 0));
    }

    @Test
    @DisplayName("알림 후보 계산 시 수확 시작 월이 이번 달이면 피드백 알림 후보를 생성한다")
    void getNotificationCandidatesCreatesHarvestFeedbackCandidate() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 1, 0);
        User salesRep = createSalesRepUser(101L, 9L);
        Client client = createClient("거래처C", 9L);
        ClientCrop crop = createClientCrop(client, "수박");
        Product product = createProduct("P011", "피드백 수박");

        when(userRepository.findAllByRole(Role.SALES_REP)).thenReturn(List.of(salesRep));
        when(clientRepository.findAllByManagerEmployeeId(9L)).thenReturn(List.of(client));
        when(clientCropRepository.findAllByClientIdIn(List.of(client.getId()))).thenReturn(List.of(crop));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(cultivationTimeRepository.findAllByProductIdIn(List.of(product.getId())))
                .thenReturn(List.of(createCultivationTime(product, 3, 4, 7, 8, "노지", "충남")));

        List<CultivationNotificationCandidate> candidates = productCultivationAlertService.getNotificationCandidates(now);

        assertThat(candidates).hasSize(1);
        CultivationNotificationCandidate candidate = candidates.getFirst();
        assertThat(candidate.getType()).isEqualTo(NotificationType.CULTIVATION_HARVEST_FEEDBACK);
        assertThat(candidate.getReferenceMonth()).isEqualTo(7);
        assertThat(candidate.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    private Product createProduct(String code, String name) {
        Product product = Product.builder()
                .productCode(code)
                .productName(name)
                .productCategory(ProductCategory.WATERMELON)
                .productDescription("설명")
                .amount(100)
                .unit("BOX")
                .price(new BigDecimal("10000"))
                .status(ProductStatus.SALE)
                .tags(Map.of())
                .build();
        ReflectionTestUtils.setField(product, "id", counter++);
        return product;
    }

    private CultivationTime createCultivationTime(
            Product product,
            Integer sowingStart,
            Integer plantingStart,
            Integer harvestingStart,
            Integer harvestingEnd,
            String croppingSystem,
            String region
    ) {
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

    private Client createClient(String name, Long managerEmployeeId) {
        Employee managerEmployee = Employee.builder()
                .employeeCode("MGR-" + managerEmployeeId)
                .employeeName("담당 영업")
                .employeeEmail("manager@test.com")
                .employeePhone("010-0000-1111")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(managerEmployee, "id", managerEmployeeId);

        Client client = Client.builder()
                .clientCode("CLNT-" + counter)
                .clientName(name)
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울시")
                .clientType(ClientType.NURSERY)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .managerEmployee(managerEmployee)
                .build();
        ReflectionTestUtils.setField(client, "id", counter++);
        return client;
    }

    private ClientCrop createClientCrop(Client client, String cropName) {
        ClientCrop crop = ClientCrop.builder()
                .cropName(cropName)
                .client(client)
                .build();
        ReflectionTestUtils.setField(crop, "id", counter++);
        return crop;
    }

    private User createSalesRepUser(Long userId, Long employeeId) {
        Employee employee = Employee.builder()
                .employeeCode("EMP-" + employeeId)
                .employeeName("영업사원")
                .employeeEmail("sales@test.com")
                .employeePhone("010-0000-0000")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(employee, "id", employeeId);

        User user = User.builder()
                .loginId("sales-" + userId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .client(null)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
