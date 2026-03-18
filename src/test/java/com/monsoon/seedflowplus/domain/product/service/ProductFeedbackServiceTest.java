package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.FeedbackRequest;
import com.monsoon.seedflowplus.domain.product.dto.response.FeedbackResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductFeedback;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductFeedbackRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
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

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Product createDummyProduct(String code, String name) {
        Product product = Product.builder()
                .productCode(code)
                .productName(name)
                .productCategory(ProductCategory.WATERMELON)
                .productDescription("설명")
                .amount(100)
                .unit("BOX")
                .price(new BigDecimal("50000"))
                .status(ProductStatus.SALE)
                .tags(Map.of())
                .build();
        return product;
    }

    // ─── createFeedback ────────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 생성 성공 테스트")
    void testCreateProductFeedback() {
        // given
        Long productId = 1L;
        Long userId = 2L;

        Product product = createDummyProduct("P001", "테스트 씨앗");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Employee employee = Employee.builder().employeeName("테스터").build();
        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        FeedbackRequest request = mock(FeedbackRequest.class);
        when(request.getContent()).thenReturn("개선 요망");

        ProductFeedback dummyFeedback = ProductFeedback.builder().content("개선 요망").build();
        ReflectionTestUtils.setField(dummyFeedback, "id", 1L);
        when(productFeedbackRepository.save(any(ProductFeedback.class))).thenReturn(dummyFeedback);

        // when
        productFeedbackService.createFeedback(productId, userId, request);

        // then
        verify(productFeedbackRepository, times(1)).save(any(ProductFeedback.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품에 피드백 생성 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void testCreateFeedback_ProductNotFound() {
        // given
        Long productId = 999L;
        Long userId = 2L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        FeedbackRequest request = mock(FeedbackRequest.class);

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> productFeedbackService.createFeedback(productId, userId, request))
                .isInstanceOf(com.monsoon.seedflowplus.core.common.support.error.CoreException.class)
                .hasFieldOrPropertyWithValue("errorType",
                        com.monsoon.seedflowplus.core.common.support.error.ErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("Employee가 없는 사용자가 피드백 생성 시 예외가 발생한다")
    void testCreateFeedback_EmployeeNotLinked() {
        // given
        Long productId = 1L;
        Long userId = 2L;

        Product product = createDummyProduct("P002", "씨앗");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(null); // employee 없음
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        FeedbackRequest request = mock(FeedbackRequest.class);

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> productFeedbackService.createFeedback(productId, userId, request))
                .isInstanceOf(com.monsoon.seedflowplus.core.common.support.error.CoreException.class)
                .hasFieldOrPropertyWithValue("errorType",
                        com.monsoon.seedflowplus.core.common.support.error.ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("답글(reply) 피드백 생성 시 부모 피드백이 올바르게 연결되어야 한다")
    void testCreateFeedback_WithParent() {
        // given
        Long productId = 1L;
        Long userId = 2L;
        Long parentFeedbackId = 5L;

        Product product = createDummyProduct("P003", "포도 씨앗");
        ReflectionTestUtils.setField(product, "id", productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Employee employee = Employee.builder().employeeName("답글작성자").build();
        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ProductFeedback parentFeedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .content("부모 피드백")
                .build();
        ReflectionTestUtils.setField(parentFeedback, "id", parentFeedbackId);

        FeedbackRequest request = mock(FeedbackRequest.class);
        when(request.getContent()).thenReturn("답글 내용");
        when(request.getParentId()).thenReturn(parentFeedbackId);

        when(productFeedbackRepository.findById(parentFeedbackId)).thenReturn(Optional.of(parentFeedback));

        ProductFeedback savedFeedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .parent(parentFeedback)
                .content("답글 내용")
                .build();
        ReflectionTestUtils.setField(savedFeedback, "id", 100L);
        when(productFeedbackRepository.save(any(ProductFeedback.class))).thenReturn(savedFeedback);

        // when
        Long createdId = productFeedbackService.createFeedback(productId, userId, request);

        // then
        assertThat(createdId).isEqualTo(100L);
        org.mockito.ArgumentCaptor<ProductFeedback> captor =
                org.mockito.ArgumentCaptor.forClass(ProductFeedback.class);
        verify(productFeedbackRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getParent()).isEqualTo(parentFeedback);
        assertThat(captor.getValue().getProduct().getId()).isEqualTo(productId);
    }

    // ─── getProductFeedbacks ────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 조회 성공 테스트")
    void testGetProductFeedbacks() {
        // given
        Long productId = 1L;
        when(productRepository.existsById(productId)).thenReturn(true);

        Product product = createDummyProduct("P004", "배 씨앗");
        ReflectionTestUtils.setField(product, "id", productId);
        Employee employee = Employee.builder().employeeName("홍길동").build();
        ReflectionTestUtils.setField(employee, "id", 2L);

        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .content("훌륭합니다")
                .build();
        ReflectionTestUtils.setField(feedback, "id", 10L);

        when(productFeedbackRepository.findByProductIdWithEmployee(productId)).thenReturn(List.of(feedback));

        // when
        List<FeedbackResponse> responses = productFeedbackService.getProductFeedbacks(productId, null);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getContent()).isEqualTo("훌륭합니다");
        assertThat(responses.getFirst().getEmployeeName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("존재하지 않는 상품에 대한 피드백 조회 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void testGetProductFeedbacks_ProductNotFound() {
        // given
        Long productId = 999L;
        when(productRepository.existsById(productId)).thenReturn(false);

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> productFeedbackService.getProductFeedbacks(productId, null))
                .isInstanceOf(com.monsoon.seedflowplus.core.common.support.error.CoreException.class)
                .hasFieldOrPropertyWithValue("errorType",
                        com.monsoon.seedflowplus.core.common.support.error.ErrorType.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("isMine 필드는 요청 사용자와 작성자가 동일할 때 true여야 한다")
    void testGetProductFeedbacks_isMine_true() {
        // given
        Long productId = 1L;
        Long userId = 5L;
        when(productRepository.existsById(productId)).thenReturn(true);

        Employee employee = Employee.builder().employeeName("작성자").build();
        ReflectionTestUtils.setField(employee, "id", 10L);

        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Product product = createDummyProduct("P005", "키위 씨앗");
        ReflectionTestUtils.setField(product, "id", productId);

        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .content("내 피드백")
                .build();
        ReflectionTestUtils.setField(feedback, "id", 20L);

        when(productFeedbackRepository.findByProductIdWithEmployee(productId)).thenReturn(List.of(feedback));

        // when
        List<FeedbackResponse> responses = productFeedbackService.getProductFeedbacks(productId, userId);

        // then
        assertThat(responses.getFirst().isMine()).isTrue();
    }

    // ─── updateFeedback ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 수정 성공 테스트")
    void testUpdateFeedback_Success() {
        // given
        Long feedbackId = 1L;
        Long userId = 2L;

        Employee employee = Employee.builder().employeeName("수정자").build();
        ReflectionTestUtils.setField(employee, "id", 10L);

        Product product = createDummyProduct("P006", "수정 테스트 씨앗");
        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .content("원본 내용")
                .build();
        ReflectionTestUtils.setField(feedback, "id", feedbackId);

        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);

        FeedbackRequest request = mock(FeedbackRequest.class);
        when(request.getContent()).thenReturn("수정된 내용");

        when(productFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        productFeedbackService.updateFeedback(feedbackId, userId, request);

        // then
        assertThat(feedback.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("본인 피드백이 아닌 경우 수정 시 ACCESS_DENIED 예외가 발생한다")
    void testUpdateFeedback_AccessDenied() {
        // given
        Long feedbackId = 1L;
        Long userId = 99L;

        Employee feedbackOwner = Employee.builder().employeeName("원작성자").build();
        ReflectionTestUtils.setField(feedbackOwner, "id", 10L);

        Employee anotherEmployee = Employee.builder().employeeName("다른사람").build();
        ReflectionTestUtils.setField(anotherEmployee, "id", 20L);

        Product product = createDummyProduct("P007", "권한 테스트 씨앗");
        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(feedbackOwner)
                .content("원본")
                .build();

        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(anotherEmployee);

        FeedbackRequest request = mock(FeedbackRequest.class);

        when(productFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> productFeedbackService.updateFeedback(feedbackId, userId, request))
                .isInstanceOf(com.monsoon.seedflowplus.core.common.support.error.CoreException.class)
                .hasFieldOrPropertyWithValue("errorType",
                        com.monsoon.seedflowplus.core.common.support.error.ErrorType.ACCESS_DENIED);
    }

    // ─── deleteFeedback ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 삭제 성공 테스트")
    void testDeleteFeedback_Success() {
        // given
        Long feedbackId = 1L;
        Long userId = 2L;

        Employee employee = Employee.builder().employeeName("삭제자").build();
        ReflectionTestUtils.setField(employee, "id", 10L);

        Product product = createDummyProduct("P008", "삭제 테스트 씨앗");
        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .content("삭제될 피드백")
                .build();
        ReflectionTestUtils.setField(feedback, "id", feedbackId);

        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);

        when(productFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        productFeedbackService.deleteFeedback(feedbackId, userId);

        // then
        verify(productFeedbackRepository, times(1)).delete(feedback);
    }

    @Test
    @DisplayName("본인 피드백이 아닌 경우 삭제 시 ACCESS_DENIED 예외가 발생한다")
    void testDeleteFeedback_AccessDenied() {
        // given
        Long feedbackId = 1L;
        Long userId = 99L;

        Employee feedbackOwner = Employee.builder().employeeName("원작성자").build();
        ReflectionTestUtils.setField(feedbackOwner, "id", 10L);

        Employee anotherEmployee = Employee.builder().employeeName("다른사람").build();
        ReflectionTestUtils.setField(anotherEmployee, "id", 20L);

        Product product = createDummyProduct("P009", "삭제 권한 테스트 씨앗");
        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(feedbackOwner)
                .content("원본")
                .build();

        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(anotherEmployee);

        when(productFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> productFeedbackService.deleteFeedback(feedbackId, userId))
                .isInstanceOf(com.monsoon.seedflowplus.core.common.support.error.CoreException.class)
                .hasFieldOrPropertyWithValue("errorType",
                        com.monsoon.seedflowplus.core.common.support.error.ErrorType.ACCESS_DENIED);
    }
}
