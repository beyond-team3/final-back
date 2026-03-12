package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.FeedbackRequest;
import com.monsoon.seedflowplus.domain.product.dto.response.FeedbackResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/feedbacks")
@RequiredArgsConstructor
public class ProductFeedbackController {

    private final ProductFeedbackService productFeedbackService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Long> createFeedback(
            @PathVariable Long productId,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);
        Long feedbackId = productFeedbackService.createFeedback(productId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackId);
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getProductFeedbacks(@PathVariable Long productId) {
        List<FeedbackResponse> responses = productFeedbackService.getProductFeedbacks(productId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{feedbackId}")
    public ResponseEntity<Void> updateFeedback(
            @PathVariable Long productId,
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);
        productFeedbackService.updateFeedback(feedbackId, userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable Long productId,
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);
        productFeedbackService.deleteFeedback(feedbackId, userId);
        return ResponseEntity.noContent().build();
    }

    // UserId 추출 헬퍼 메서드 (ProductController와 동일한 방식 사용)
    private Long extractUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getId();
        }

        return userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND))
                .getId();
    }
}
