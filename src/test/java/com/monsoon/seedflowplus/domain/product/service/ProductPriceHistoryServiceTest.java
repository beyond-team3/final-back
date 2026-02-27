package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductPriceHistoryResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductPriceHistory;
import com.monsoon.seedflowplus.domain.product.repository.ProductPriceHistoryRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPriceHistoryServiceTest {

    @Mock
    private ProductPriceHistoryRepository productPriceHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductPriceHistoryService productPriceHistoryService;

    @Test
    @DisplayName("가격 변경 이력 조회 성공 테스트")
    void testGetPriceHistories() {
        // given
        Long productId = 10L;
        when(productRepository.existsById(productId)).thenReturn(true);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(10L);

        Employee employee = Employee.builder().employeeName("관리자").build();
        org.springframework.test.util.ReflectionTestUtils.setField(employee, "id", 100L);

        ProductPriceHistory history = ProductPriceHistory.builder()
                .product(product)
                .oldPrice(new BigDecimal("50000"))
                .newPrice(new BigDecimal("60000"))
                .modifiedBy(employee)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(history, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.now());

        when(productPriceHistoryRepository.findByProductIdWithEmployee(productId)).thenReturn(List.of(history));

        // when
        List<ProductPriceHistoryResponse> responses = productPriceHistoryService.getPriceHistories(productId);

        // then
        assertThat(responses).hasSize(1);
        ProductPriceHistoryResponse response = responses.getFirst();
        assertThat(response.getOldPrice()).isEqualByComparingTo("50000");
        assertThat(response.getNewPrice()).isEqualByComparingTo("60000");
        assertThat(response.getEmployeeName()).isEqualTo("관리자");
    }
}
