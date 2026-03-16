package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.FeedbackRequest;
import com.monsoon.seedflowplus.domain.product.dto.response.FeedbackResponse;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductFeedback;
import com.monsoon.seedflowplus.domain.product.repository.ProductFeedbackRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductFeedbackService {

    private final ProductFeedbackRepository productFeedbackRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Long createFeedback(Long productId, Long userId, FeedbackRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        Employee employee = user.getEmployee();
        if (employee == null) {
            // 직원이 아닌 경우 피드백을 작성할 수 없도록 처리 (비즈니스 요구사항에 따라 다름)
            throw new CoreException(ErrorType.USER_NOT_FOUND);
        }

        // 답글인 경우 부모 피드백 확인
        ProductFeedback parent = null;
        if (request.getParentId() != null) {
            parent = productFeedbackRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
            if (!parent.getProduct().getId().equals(productId)) {
                throw new CoreException(ErrorType.INVALID_FEEDBACK_PARENT);
            }
        }

        ProductFeedback feedback = ProductFeedback.builder()
                .product(product)
                .employee(employee)
                .parent(parent)
                .content(request.getContent())
                .build();

        return productFeedbackRepository.save(feedback).getId();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getProductFeedbacks(Long productId, Long requestUserId) {
        // 엔티티 존재 여부 확인
        if (!productRepository.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        // 현재 요청 사용자의 employeeId 조회 (로그인하지 않은 경우 null)
        Long requestEmployeeId = null;
        if (requestUserId != null) {
            requestEmployeeId = userRepository.findById(requestUserId)
                    .map(u -> u.getEmployee() != null ? u.getEmployee().getId() : null)
                    .orElse(null);
        }

        List<ProductFeedback> feedbackList = productFeedbackRepository.findByProductIdWithEmployee(productId);

        final Long finalRequestEmployeeId = requestEmployeeId;
        return feedbackList.stream().map(feedback -> FeedbackResponse.builder()
                .id(feedback.getId())
                .productId(feedback.getProduct().getId())
                .parentId(feedback.getParent() != null ? feedback.getParent().getId() : null)
                .employeeId(feedback.getEmployee().getId())
                .employeeName(feedback.getEmployee().getEmployeeName())
                .sender(feedback.getEmployee().getEmployeeName())       // 프론트엔드 표시명
                .isMine(finalRequestEmployeeId != null
                        && finalRequestEmployeeId.equals(feedback.getEmployee().getId()))
                .content(feedback.getContent())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build())
                .collect(Collectors.toList());
    }

    public void updateFeedback(Long feedbackId, Long userId, FeedbackRequest request) {
        ProductFeedback feedback = productFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        Employee employee = user.getEmployee();
        if (employee == null || !feedback.getEmployee().getId().equals(employee.getId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        feedback.updateContent(request.getContent());
    }

    public void deleteFeedback(Long feedbackId, Long userId) {
        ProductFeedback feedback = productFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        Employee employee = user.getEmployee();
        if (employee == null || !feedback.getEmployee().getId().equals(employee.getId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        productFeedbackRepository.delete(feedback);
    }
}
