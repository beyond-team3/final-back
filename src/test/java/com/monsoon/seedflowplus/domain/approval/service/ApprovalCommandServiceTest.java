package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.approval.dto.request.CreateApprovalRequestRequest;
import com.monsoon.seedflowplus.domain.approval.dto.request.DecideApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStep;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStepStatus;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalDecisionRepository;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalRequestRepository;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalStepRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalCommandServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ApprovalStepRepository approvalStepRepository;

    @Mock
    private ApprovalDecisionRepository approvalDecisionRepository;

    @Mock
    private ApprovalDealLogWriter approvalDealLogWriter;

    @InjectMocks
    private ApprovalCommandService approvalCommandService;

    @Test
    @DisplayName("계약서 step2는 step1 승인 전 처리할 수 없다")
    void shouldFailWhenClientStepBeforeAdminApproval() {
        ApprovalRequest request = cntRequest(100L, 7L);
        ApprovalStep adminStep = step(11L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        ApprovalStep clientStep = step(12L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);

        CustomUserDetails clientUser = mockUser(Role.CLIENT, 1000L, 7L);

        when(approvalRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(12L, 100L)).thenReturn(Optional.of(clientStep));
        when(approvalDecisionRepository.existsByApprovalStepId(12L)).thenReturn(false);
        when(approvalStepRepository.findByApprovalRequestIdAndStepOrder(100L, 1)).thenReturn(Optional.of(adminStep));

        assertThatThrownBy(() -> approvalCommandService.decideStep(
                100L,
                12L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                clientUser
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_STEP_NOT_ACTIVE);
    }

    @Test
    @DisplayName("반려 사유가 비어있으면 실패한다")
    void shouldFailWhenRejectReasonBlank() {
        ApprovalRequest request = quoRequest(200L);
        ApprovalStep adminStep = step(21L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        CustomUserDetails adminUser = mockUser(Role.ADMIN, 999L, null);

        when(approvalRequestRepository.findById(200L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(21L, 200L)).thenReturn(Optional.of(adminStep));
        when(approvalDecisionRepository.existsByApprovalStepId(21L)).thenReturn(false);

        assertThatThrownBy(() -> approvalCommandService.decideStep(
                200L,
                21L,
                new DecideApprovalRequest(DecisionType.REJECT, "  "),
                adminUser
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_REASON_REQUIRED);

        verify(approvalDecisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADMIN step은 ADMIN만 처리 가능하다")
    void shouldFailWhenNonAdminTriesAdminStep() {
        ApprovalRequest request = quoRequest(300L);
        ApprovalStep adminStep = step(31L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        CustomUserDetails salesRepUser = mockUser(Role.SALES_REP, 3000L, null);

        when(approvalRequestRepository.findById(300L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(31L, 300L)).thenReturn(Optional.of(adminStep));
        when(approvalDecisionRepository.existsByApprovalStepId(31L)).thenReturn(false);

        assertThatThrownBy(() -> approvalCommandService.decideStep(
                300L,
                31L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                salesRepUser
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_ROLE_MISMATCH);
    }

    @Test
    @DisplayName("CLIENT step에서 clientIdSnapshot과 로그인 clientId가 다르면 실패한다")
    void shouldFailWhenClientIdMismatch() {
        ApprovalRequest request = cntRequest(400L, 77L);
        ApprovalStep clientStep = step(41L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        CustomUserDetails clientUser = mockUser(Role.CLIENT, 777L, 88L);

        when(approvalRequestRepository.findById(400L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(41L, 400L)).thenReturn(Optional.of(clientStep));
        when(approvalDecisionRepository.existsByApprovalStepId(41L)).thenReturn(false);

        assertThatThrownBy(() -> approvalCommandService.decideStep(
                400L,
                41L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                clientUser
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_CLIENT_MISMATCH);
    }

    @Test
    @DisplayName("같은 문서의 PENDING 승인요청 중복 생성은 차단된다")
    void shouldBlockDuplicatedPendingApprovalRequest() {
        CreateApprovalRequestRequest request = new CreateApprovalRequestRequest(
                DealType.QUO,
                500L,
                null,
                "Q-500"
        );

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(
                DealType.QUO,
                500L,
                ApprovalStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(request, mockUser(Role.ADMIN, 1L, null)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_REQUEST_DUPLICATED);

        verify(approvalRequestRepository, never()).save(any());
    }

    private ApprovalRequest quoRequest(Long id) {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(9000L)
                .status(ApprovalStatus.PENDING)
                .targetCodeSnapshot("Q-9000")
                .build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private ApprovalRequest cntRequest(Long id, Long clientIdSnapshot) {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.CNT)
                .targetId(9100L)
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(clientIdSnapshot)
                .targetCodeSnapshot("C-9100")
                .build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private ApprovalStep step(Long id, ApprovalRequest request, int order, ActorType actorType, ApprovalStepStatus status) {
        ApprovalStep step = ApprovalStep.builder()
                .approvalRequest(request)
                .stepOrder(order)
                .actorType(actorType)
                .status(status)
                .build();
        ReflectionTestUtils.setField(step, "id", id);
        return step;
    }

    private CustomUserDetails mockUser(Role role, Long userId, Long clientId) {
        CustomUserDetails user = org.mockito.Mockito.mock(CustomUserDetails.class);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(user.getClientId()).thenReturn(clientId);
        return user;
    }
}
