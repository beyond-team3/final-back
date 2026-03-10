package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.approval.dto.request.CreateApprovalRequestRequest;
import com.monsoon.seedflowplus.domain.approval.dto.request.DecideApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.dto.response.CreateApprovalRequestResponse;
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
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DocStatusTransitionValidator;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private SalesDealLogRepository salesDealLogRepository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DocStatusTransitionValidator docStatusTransitionValidator;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DealScheduleSyncService dealScheduleSyncService;

    @Mock
    private Clock clock;

    @InjectMocks
    private ApprovalCommandService approvalCommandService;

    @BeforeEach
    void setUp() {
        lenient().when(approvalDecisionRepository.findByApprovalStepId(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.findAllByRole(any())).thenReturn(List.of());
        lenient().when(userRepository.findByClientId(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByEmployeeId(any())).thenReturn(Optional.empty());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("케이스 1: QUO approval 생성 시 문서 기준 client snapshot과 step 2개를 생성한다")
    void createApprovalRequestCreatesTwoStepsForQuotation() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 500L, 77L, "Q-500");
        CustomUserDetails principal = mockUser(Role.ADMIN, 10L, null);
        ArgumentCaptor<ApprovalRequest> requestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        QuotationHeader quotation = quotation(500L, QuotationStatus.WAITING_ADMIN, 77L);

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 500L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(500L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 900L);
            return saved;
        });

        CreateApprovalRequestResponse response = approvalCommandService.createApprovalRequest(dto, principal);

        verify(approvalRequestRepository).save(requestCaptor.capture());
        ApprovalRequest savedRequest = requestCaptor.getValue();
        assertThat(response.approvalId()).isEqualTo(900L);
        assertThat(savedRequest.getClientIdSnapshot()).isEqualTo(77L);
        assertThat(savedRequest.getSteps()).hasSize(2);
        assertThat(savedRequest.getSteps().get(0).getActorType()).isEqualTo(ActorType.ADMIN);
        assertThat(savedRequest.getSteps().get(0).getStatus()).isEqualTo(ApprovalStepStatus.WAITING);
        assertThat(savedRequest.getSteps().get(1).getActorType()).isEqualTo(ActorType.CLIENT);
        assertThat(savedRequest.getSteps().get(1).getStatus()).isEqualTo(ApprovalStepStatus.WAITING);
    }

    @Test
    @DisplayName("케이스 1-1: QUO approval 생성 시 clientIdSnapshot이 없어도 문서 기준으로 채운다")
    void createApprovalRequestFillsClientIdSnapshotFromQuotation() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 501L, null, "Q-501");
        QuotationHeader quotation = quotation(501L, QuotationStatus.WAITING_ADMIN, 701L);
        ArgumentCaptor<ApprovalRequest> requestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 501L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(501L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 901L);
            return saved;
        });

        approvalCommandService.createApprovalRequest(dto, mockUser(Role.ADMIN, 10L, null));

        verify(approvalRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getClientIdSnapshot()).isEqualTo(701L);
    }

    @Test
    @DisplayName("케이스 1-1-1: QUO approval 생성 시 요청 snapshot과 문서 거래처가 다르면 예외")
    void createApprovalRequestFailsWhenSnapshotDiffersFromQuotationClient() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 501L, 999L, "Q-501");
        QuotationHeader quotation = quotation(501L, QuotationStatus.WAITING_ADMIN, 701L);

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 501L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(501L)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(dto, mockUser(Role.ADMIN, 10L, null)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_CLIENT_MISMATCH);

        verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
    }

    @Test
    @DisplayName("케이스 1-2: ADMIN 생성자는 submit log에 ADMIN actor와 employeeId가 기록된다")
    void createApprovalRequestWritesAdminSubmitLog() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 502L, 77L, "Q-502");
        CustomUserDetails principal = mockUser(Role.ADMIN, 321L, null);
        QuotationHeader quotation = quotation(502L, QuotationStatus.WAITING_ADMIN, 77L);

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 502L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(502L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 902L);
            return saved;
        });

        approvalCommandService.createApprovalRequest(dto, principal);

        verify(approvalDealLogWriter).writeSubmit(any(ApprovalRequest.class), eq(ActorType.ADMIN), eq(321L));
    }

    @Test
    @DisplayName("케이스 1-3: CLIENT 생성자는 submit log에 CLIENT actor와 clientId가 기록된다")
    void createApprovalRequestWritesClientSubmitLog() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 503L, 88L, "Q-503");
        CustomUserDetails principal = mockUser(Role.CLIENT, null, 88L);
        QuotationHeader quotation = quotation(503L, QuotationStatus.WAITING_ADMIN, 88L);

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 503L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(503L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 903L);
            return saved;
        });

        approvalCommandService.createApprovalRequest(dto, principal);

        verify(approvalDealLogWriter).writeSubmit(any(ApprovalRequest.class), eq(ActorType.CLIENT), eq(88L));
    }

    @Test
    @DisplayName("케이스 1-4: ADMIN 생성자에 employeeId가 없으면 UNAUTHORIZED")
    void createApprovalRequestFailsWhenAdminEmployeeIdMissing() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 504L, 77L, "Q-504");

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 504L, ApprovalStatus.PENDING))
                .thenReturn(false);

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(dto, mockUser(Role.ADMIN, null, null)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("케이스 1-5: CLIENT 생성자에 clientId가 없으면 UNAUTHORIZED")
    void createApprovalRequestFailsWhenClientIdMissing() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 505L, 77L, "Q-505");

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 505L, ApprovalStatus.PENDING))
                .thenReturn(false);

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(dto, mockUser(Role.CLIENT, null, null)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("케이스 1-6: SALES_REP 생성자는 employeeId가 없으면 userId가 있어도 UNAUTHORIZED")
    void createApprovalRequestFailsWhenSalesRepEmployeeIdMissing() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 506L, 77L, "Q-506");

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 506L, ApprovalStatus.PENDING))
                .thenReturn(false);

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(
                dto,
                mockUserWithIds(Role.SALES_REP, 999L, null, null)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("케이스 1-7: SALES_REP는 자신이 담당하지 않은 QUO deal이면 submit할 수 없다")
    void createApprovalRequestFailsWhenSalesRepDoesNotOwnQuotationDeal() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 507L, 77L, "Q-507");
        QuotationHeader quotation = quotation(507L, QuotationStatus.WAITING_ADMIN, 77L, salesDeal(900L));

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 507L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(507L)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(dto, mockUser(Role.SALES_REP, 501L, null)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.UNAUTHORIZED);

        verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
    }

    @Test
    @DisplayName("케이스 1-8: SALES_REP는 자신이 담당한 QUO deal이면 submit할 수 있다")
    void createApprovalRequestSucceedsWhenSalesRepOwnsQuotationDeal() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 508L, 77L, "Q-508");
        QuotationHeader quotation = quotation(508L, QuotationStatus.WAITING_ADMIN, 77L, salesDeal(501L));

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 508L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(quotationRepository.findById(508L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 904L);
            return saved;
        });

        CreateApprovalRequestResponse response = approvalCommandService.createApprovalRequest(
                dto,
                mockUser(Role.SALES_REP, 501L, null)
        );

        assertThat(response.approvalId()).isEqualTo(904L);
        verify(approvalDealLogWriter).writeSubmit(any(ApprovalRequest.class), eq(ActorType.SALES_REP), eq(501L));
    }

    @Test
    @DisplayName("케이스 1-9: SALES_REP는 deal 담당자가 없으면 CNT submit할 수 없다")
    void createApprovalRequestFailsWhenDealOwnerMissing() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.CNT, 509L, 77L, "C-509");
        ContractHeader contract = contract(509L, ContractStatus.WAITING_ADMIN, 77L, salesDeal(null));

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.CNT, 509L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(contractRepository.findById(509L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> approvalCommandService.createApprovalRequest(dto, mockUser(Role.SALES_REP, 501L, null)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.UNAUTHORIZED);

        verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
    }

    @Test
    @DisplayName("케이스 1-10: ADMIN은 deal 담당자와 무관하게 CNT submit이 가능하다")
    void createApprovalRequestAllowsAdminRegardlessOfDealOwner() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.CNT, 510L, 77L, "C-510");
        ContractHeader contract = contract(510L, ContractStatus.WAITING_ADMIN, 77L, salesDeal(999L));

        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.CNT, 510L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(contractRepository.findById(510L)).thenReturn(Optional.of(contract));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 905L);
            return saved;
        });

        CreateApprovalRequestResponse response = approvalCommandService.createApprovalRequest(
                dto,
                mockUser(Role.ADMIN, 10L, null)
        );

        assertThat(response.approvalId()).isEqualTo(905L);
        verify(approvalDealLogWriter).writeSubmit(any(ApprovalRequest.class), eq(ActorType.ADMIN), eq(10L));
    }

    @Test
    @DisplayName("케이스 2: QUO ADMIN approve")
    void approveQuotationByAdmin() {
        ApprovalRequest request = quoRequest(100L, 7000L, 77L);
        ApprovalStep step1 = step(11L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        ApprovalStep step2 = step(12L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        QuotationHeader quotation = quotation(7000L, QuotationStatus.WAITING_ADMIN);

        when(approvalRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(11L, 100L)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(11L)).thenReturn(false);
        when(quotationRepository.findById(7000L)).thenReturn(Optional.of(quotation));

        approvalCommandService.decideStep(
                100L,
                11L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                mockUser(Role.ADMIN, 501L, null)
        );

        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.WAITING_CLIENT);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(step1.getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);
        assertThat(step2.getStatus()).isEqualTo(ApprovalStepStatus.WAITING);
        verify(approvalDealLogWriter, times(1)).writeDecision(
                eq(request),
                eq(step1),
                eq(DecisionType.APPROVE),
                eq("WAITING_ADMIN"),
                eq("WAITING_CLIENT"),
                eq("PENDING_ADMIN"),
                eq("PENDING_CLIENT"),
                eq(null),
                eq(ActorType.ADMIN),
                eq(501L)
        );
    }

    @Test
    @DisplayName("케이스 3: QUO CLIENT approve")
    void approveQuotationByClient() {
        ApprovalRequest request = quoRequest(200L, 7001L, 88L);
        ApprovalStep step1 = step(21L, request, 1, ActorType.ADMIN, ApprovalStepStatus.APPROVED);
        ApprovalStep step2 = step(22L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        QuotationHeader quotation = quotation(7001L, QuotationStatus.WAITING_CLIENT);

        when(approvalRequestRepository.findById(200L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(22L, 200L)).thenReturn(Optional.of(step2));
        when(approvalStepRepository.findByApprovalRequestIdAndStepOrder(200L, 1)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(200L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(22L)).thenReturn(false);
        when(quotationRepository.findById(7001L)).thenReturn(Optional.of(quotation));

        approvalCommandService.decideStep(
                200L,
                22L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                mockUser(Role.CLIENT, null, 88L)
        );

        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.FINAL_APPROVED);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(step2.getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);
    }

    @Test
    @DisplayName("케이스 4: QUO ADMIN reject")
    void rejectQuotationByAdmin() {
        ApprovalRequest request = quoRequest(300L, 7002L, 77L);
        ApprovalStep step1 = step(31L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        ApprovalStep step2 = step(32L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        QuotationHeader quotation = quotation(7002L, QuotationStatus.WAITING_ADMIN);

        when(approvalRequestRepository.findById(300L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(31L, 300L)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(300L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(31L)).thenReturn(false);
        when(quotationRepository.findById(7002L)).thenReturn(Optional.of(quotation));

        approvalCommandService.decideStep(
                300L,
                31L,
                new DecideApprovalRequest(DecisionType.REJECT, "reject-admin"),
                mockUser(Role.ADMIN, 601L, null)
        );

        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.REJECTED_ADMIN);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    @DisplayName("케이스 5: QUO CLIENT reject")
    void rejectQuotationByClient() {
        ApprovalRequest request = quoRequest(400L, 7003L, 99L);
        ApprovalStep step1 = step(41L, request, 1, ActorType.ADMIN, ApprovalStepStatus.APPROVED);
        ApprovalStep step2 = step(42L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        QuotationHeader quotation = quotation(7003L, QuotationStatus.WAITING_CLIENT);

        when(approvalRequestRepository.findById(400L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(42L, 400L)).thenReturn(Optional.of(step2));
        when(approvalStepRepository.findByApprovalRequestIdAndStepOrder(400L, 1)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(400L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(42L)).thenReturn(false);
        when(quotationRepository.findById(7003L)).thenReturn(Optional.of(quotation));

        approvalCommandService.decideStep(
                400L,
                42L,
                new DecideApprovalRequest(DecisionType.REJECT, "reject-client"),
                mockUser(Role.CLIENT, null, 99L)
        );

        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.REJECTED_CLIENT);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    @DisplayName("케이스 6: CNT ADMIN approve")
    void approveContractByAdmin() {
        ApprovalRequest request = cntRequest(500L, 8000L, 77L);
        ApprovalStep step1 = step(51L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        ApprovalStep step2 = step(52L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        ContractHeader contract = contract(8000L, ContractStatus.WAITING_ADMIN);

        when(approvalRequestRepository.findById(500L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(51L, 500L)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(500L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(51L)).thenReturn(false);
        when(contractRepository.findById(8000L)).thenReturn(Optional.of(contract));

        approvalCommandService.decideStep(
                500L,
                51L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                mockUser(Role.ADMIN, 701L, null)
        );

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.WAITING_CLIENT);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("케이스 7: CNT CLIENT approve")
    void approveContractByClient() {
        ApprovalRequest request = cntRequest(600L, 8001L, 101L);
        ApprovalStep step1 = step(61L, request, 1, ActorType.ADMIN, ApprovalStepStatus.APPROVED);
        ApprovalStep step2 = step(62L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        ContractHeader contract = contract(8001L, ContractStatus.WAITING_CLIENT, 101L, salesDeal(501L));

        when(approvalRequestRepository.findById(600L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(62L, 600L)).thenReturn(Optional.of(step2));
        when(approvalStepRepository.findByApprovalRequestIdAndStepOrder(600L, 1)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(600L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(62L)).thenReturn(false);
        when(contractRepository.findById(8001L)).thenReturn(Optional.of(contract));

        approvalCommandService.decideStep(
                600L,
                62L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                mockUser(Role.CLIENT, null, 101L)
        );

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE_CONTRACT);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService, times(2)).upsertFromEvent(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(DealScheduleUpsertCommand::externalKey)
                .containsExactly("CNT_8001_DOC_APPROVED_2026-03-10", "CNT_8001_DOC_APPROVED_2026-03-20");
    }

    @Test
    @DisplayName("케이스 7-1: CNT CLIENT approve 시 owner user가 없어도 현재 사용자로 일정 동기화한다")
    void approveContractByClientFallsBackToPrincipalUserForScheduleAssignee() {
        ApprovalRequest request = cntRequest(601L, 8002L, 101L);
        ApprovalStep step1 = step(63L, request, 1, ActorType.ADMIN, ApprovalStepStatus.APPROVED);
        ApprovalStep step2 = step(64L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);
        request.addStep(step1);
        request.addStep(step2);
        ContractHeader contract = contract(8002L, ContractStatus.WAITING_CLIENT, 101L, salesDeal(501L));
        CustomUserDetails principal = mockUserWithIds(Role.CLIENT, 9101L, null, 101L);

        when(approvalRequestRepository.findById(601L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(64L, 601L)).thenReturn(Optional.of(step2));
        when(approvalStepRepository.findByApprovalRequestIdAndStepOrder(601L, 1)).thenReturn(Optional.of(step1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(601L)).thenReturn(List.of(step1, step2));
        when(approvalDecisionRepository.existsByApprovalStepId(64L)).thenReturn(false);
        when(contractRepository.findById(8002L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmployeeId(501L)).thenReturn(Optional.empty());

        approvalCommandService.decideStep(
                601L,
                64L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                principal
        );

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService, times(2)).upsertFromEvent(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(DealScheduleUpsertCommand::assigneeUserId)
                .containsOnly(principal.getUserId());
    }

    @Test
    @DisplayName("케이스 8: step2를 step1 승인 전에 처리하면 예외")
    void failWhenStep2HandledBeforeStep1Approval() {
        ApprovalRequest request = cntRequest(700L, 8002L, 202L);
        ApprovalStep step1 = step(71L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        ApprovalStep step2 = step(72L, request, 2, ActorType.CLIENT, ApprovalStepStatus.WAITING);

        when(approvalRequestRepository.findById(700L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(72L, 700L)).thenReturn(Optional.of(step2));
        when(approvalDecisionRepository.existsByApprovalStepId(72L)).thenReturn(false);
        when(approvalStepRepository.findByApprovalRequestIdAndStepOrder(700L, 1)).thenReturn(Optional.of(step1));

        assertThatThrownBy(() -> approvalCommandService.decideStep(
                700L,
                72L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                mockUser(Role.CLIENT, null, 202L)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_STEP_NOT_ACTIVE);
    }

    @Test
    @DisplayName("케이스 9: 잘못된 문서 상태 승인 시 validator 예외와 writeDecision 미호출")
    void failWhenDocStatusTransitionValidatorRejectsDecision() {
        ApprovalRequest request = quoRequest(800L, 7004L, 77L);
        ApprovalStep step1 = step(81L, request, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);
        QuotationHeader quotation = quotation(7004L, QuotationStatus.REJECTED_ADMIN);

        when(approvalRequestRepository.findById(800L)).thenReturn(Optional.of(request));
        when(approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(81L, 800L)).thenReturn(Optional.of(step1));
        when(approvalDecisionRepository.existsByApprovalStepId(81L)).thenReturn(false);
        when(quotationRepository.findById(7004L)).thenReturn(Optional.of(quotation));
        doThrow(new CoreException(ErrorType.INVALID_DOC_STATUS_TRANSITION))
                .when(docStatusTransitionValidator)
                .validateOrThrow(DealType.QUO, "REJECTED_ADMIN", com.monsoon.seedflowplus.domain.deal.common.ActionType.APPROVE, "WAITING_CLIENT");

        assertThatThrownBy(() -> approvalCommandService.decideStep(
                800L,
                81L,
                new DecideApprovalRequest(DecisionType.APPROVE, null),
                mockUser(Role.ADMIN, 801L, null)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_DOC_STATUS_TRANSITION);

        verify(approvalDealLogWriter, never()).writeDecision(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("케이스 10: CLIENT search는 저장소 접근 제어 쿼리 결과를 그대로 사용한다")
    void searchUsesClientScopedRepositoryQuery() {
        ApprovalRequest accessible = quoRequest(901L, 7100L, 77L);
        ApprovalStep accessibleStep = step(91L, accessible, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);

        when(approvalRequestRepository.searchForClient(null, DealType.QUO, null, 77L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(accessible), PageRequest.of(0, 10), 1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(901L)).thenReturn(List.of(accessibleStep));

        var result = approvalCommandService.search(
                null,
                DealType.QUO,
                null,
                PageRequest.of(0, 10),
                mockUser(Role.CLIENT, null, 77L)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(approvalRequestRepository).searchForClient(null, DealType.QUO, null, 77L, PageRequest.of(0, 10));
        verify(approvalRequestRepository, never()).search(null, DealType.QUO, null, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("케이스 11: SALES_REP search는 저장소 접근 제어 쿼리 결과를 그대로 사용한다")
    void searchUsesSalesRepScopedRepositoryQuery() {
        ApprovalRequest accessible = quoRequest(902L, 7200L, 88L);
        ApprovalStep accessibleStep = step(92L, accessible, 1, ActorType.ADMIN, ApprovalStepStatus.WAITING);

        when(approvalRequestRepository.searchForSalesRep(null, DealType.QUO, null, 501L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(accessible), PageRequest.of(0, 10), 1));
        when(approvalStepRepository.findByApprovalRequestIdOrderByStepOrderAsc(902L)).thenReturn(List.of(accessibleStep));

        var result = approvalCommandService.search(
                null,
                DealType.QUO,
                null,
                PageRequest.of(0, 10),
                mockUser(Role.SALES_REP, 501L, null)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(approvalRequestRepository).searchForSalesRep(null, DealType.QUO, null, 501L, PageRequest.of(0, 10));
        verify(approvalRequestRepository, never()).search(null, DealType.QUO, null, PageRequest.of(0, 10));
    }

    private ApprovalRequest quoRequest(Long id, Long targetId, Long clientIdSnapshot) {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(targetId)
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(clientIdSnapshot)
                .targetCodeSnapshot("Q-" + targetId)
                .build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private ApprovalRequest cntRequest(Long id, Long targetId, Long clientIdSnapshot) {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.CNT)
                .targetId(targetId)
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(clientIdSnapshot)
                .targetCodeSnapshot("C-" + targetId)
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

    private QuotationHeader quotation(Long id, QuotationStatus status) {
        return quotation(id, status, 1L);
    }

    private QuotationHeader quotation(Long id, QuotationStatus status, Long clientId) {
        return quotation(id, status, clientId, org.mockito.Mockito.mock(SalesDeal.class));
    }

    private QuotationHeader quotation(Long id, QuotationStatus status, Long clientId, SalesDeal deal) {
        Client client = org.mockito.Mockito.mock(Client.class);
        lenient().when(client.getId()).thenReturn(clientId);
        QuotationHeader quotation = QuotationHeader.create(
                null,
                "Q-" + id,
                client,
                deal,
                org.mockito.Mockito.mock(Employee.class),
                BigDecimal.TEN,
                "memo"
        );
        ReflectionTestUtils.setField(quotation, "id", id);
        quotation.updateStatus(status);
        return quotation;
    }

    private ContractHeader contract(Long id, ContractStatus status) {
        return contract(id, status, 1L, org.mockito.Mockito.mock(SalesDeal.class));
    }

    private ContractHeader contract(Long id, ContractStatus status, SalesDeal deal) {
        return contract(id, status, 1L, deal);
    }

    private ContractHeader contract(Long id, ContractStatus status, Long clientId, SalesDeal deal) {
        Client client = Client.builder()
                .clientCode("C-" + clientId)
                .clientName("거래처-" + clientId)
                .clientBrn("123-45-" + clientId)
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("client@test.com")
                .build();
        ReflectionTestUtils.setField(client, "id", clientId);
        ContractHeader contract = ContractHeader.create(
                "C-" + id,
                null,
                client,
                deal,
                org.mockito.Mockito.mock(Employee.class),
                BigDecimal.TEN,
                null,
                null,
                null,
                "terms",
                "memo"
        );
        ReflectionTestUtils.setField(contract, "id", id);
        ReflectionTestUtils.setField(contract, "startDate", LocalDate.of(2026, 3, 10));
        ReflectionTestUtils.setField(contract, "endDate", LocalDate.of(2026, 3, 20));
        contract.updateStatus(status);
        return contract;
    }

    private SalesDeal salesDeal(Long ownerEmployeeId) {
        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        lenient().when(deal.getId()).thenReturn(9900L + (ownerEmployeeId == null ? 0L : ownerEmployeeId));
        if (ownerEmployeeId == null) {
            lenient().when(deal.getOwnerEmp()).thenReturn(null);
            return deal;
        }

        Employee owner = org.mockito.Mockito.mock(Employee.class);
        lenient().when(owner.getId()).thenReturn(ownerEmployeeId);
        lenient().when(deal.getOwnerEmp()).thenReturn(owner);
        User assigneeUser = User.builder()
                .loginId("sales-" + ownerEmployeeId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(owner)
                .build();
        ReflectionTestUtils.setField(assigneeUser, "id", 8800L + ownerEmployeeId);
        lenient().when(userRepository.findByEmployeeId(ownerEmployeeId)).thenReturn(Optional.of(assigneeUser));
        return deal;
    }

    private CustomUserDetails mockUser(Role role, Long employeeId, Long clientId) {
        CustomUserDetails user = org.mockito.Mockito.mock(CustomUserDetails.class);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(user.getUserId()).thenReturn(employeeId);
        lenient().when(user.getEmployeeId()).thenReturn(employeeId);
        lenient().when(user.getClientId()).thenReturn(clientId);
        return user;
    }

    private CustomUserDetails mockUserWithIds(Role role, Long userId, Long employeeId, Long clientId) {
        CustomUserDetails user = org.mockito.Mockito.mock(CustomUserDetails.class);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(user.getEmployeeId()).thenReturn(employeeId);
        lenient().when(user.getClientId()).thenReturn(clientId);
        return user;
    }
}
