package com.monsoon.seedflowplus.domain.sales.order.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.service.OrderApprovalConfirmedEvent;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderApprovalConfirmedEventHandler {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderApprovalConfirmedEvent event) {
        try {
            orderService.confirmOrderFromApproval(
                    event.orderId(),
                    resolveApprovalPrincipal(event.approverUserId())
            );
        } catch (Throwable t) {
            log.error("Failed to confirm order after approval commit. event={}", event, t);
            throw t instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(t);
        }
    }

    private CustomUserDetails resolveApprovalPrincipal(Long approverUserId) {
        if (approverUserId == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "주문 승인 사용자 정보가 없습니다.");
        }
        User user = userRepository.findById(approverUserId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
        return new CustomUserDetails(user);
    }
}
