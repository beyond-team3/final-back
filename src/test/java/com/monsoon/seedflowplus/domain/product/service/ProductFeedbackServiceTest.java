package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.FeedbackRequest;
import com.monsoon.seedflowplus.domain.product.dto.response.FeedbackResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductFeedback;
import com.monsoon.seedflowplus.domain.product.repository.ProductFeedbackRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFeedbackServiceTest {

    @Mock
    private ProductFeedbackRepository productFeedbackRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductFeedbackService productFeedbackService;

    @Test
    @DisplayName("피드백 생성 성공 테스트")
    void testCreateProductFeedback() {
        // given
        Long productId = 1L;
        Long userId = 2L;

        Product product = mock(Product.class);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Employee employee = Employee.builder().employeeName("테스터").build();
        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        FeedbackRequest request = mock(FeedbackRequest.class);
        when(request.getContent()).thenReturn("개선 요망");

        ProductFeedback dummyFeedback = ProductFeedback.builder().content("개선 요망").build();
        org.springframework.test.util.ReflectionTestUtils.setField(dummyFeedback, "id", 1L);
        when(productFeedbackRepository.save(any(ProductFeedback.class))).thenReturn(dummyFeedback);

        // when
        productFeedbackService.createFeedback(productId, userId, request);

        // then
        verify(productFeedbackRepository, times(1)).save(any(ProductFeedback.class));
    }

    @Test
    @DisplayName("피드백 조회 성공 테스트")
    void testGetProductFeedbacks() {
        // given
        Long productId = 1L;
        when(productRepository.existsById(productId)).thenReturn(true);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        Employee employee = Employee.builder().employeeName("홍길동").build();
        org.springframework.test.util.ReflectionTestUtils.setField(employee, "id", 2L);

        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .content("훌륭합니다")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(feedback, "id", 10L);

        when(productFeedbackRepository.findByProductIdWithEmployee(productId)).thenReturn(List.of(feedback));

        // when
        List<FeedbackResponse> responses = productFeedbackService.getProductFeedbacks(productId, null);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getContent()).isEqualTo("훌륭합니다");
        assertThat(responses.getFirst().getEmployeeName()).isEqualTo("홍길동");
    }
}
