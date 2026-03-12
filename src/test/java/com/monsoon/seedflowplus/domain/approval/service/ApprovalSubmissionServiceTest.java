package com.monsoon.seedflowplus.domain.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.dto.request.CreateApprovalRequestRequest;
import com.monsoon.seedflowplus.domain.approval.dto.response.CreateApprovalRequestResponse;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalRequestRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApprovalSubmissionServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ApprovalDealLogWriter approvalDealLogWriter;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ApprovalSubmissionService approvalSubmissionService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-03-12T00:00:00Z"));
        lenient().when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(adminUser(1000L)));
    }

    @Test
    @DisplayName("견적 생성 자동 제출은 SALES_REP principal로 승인 요청을 생성한다")
    void submitFromDocumentCreationCreatesApprovalForQuotation() {
        QuotationHeader quotation = quotation(500L, QuotationStatus.WAITING_ADMIN, 77L, 501L);
        CustomUserDetails principal = salesRepUser(501L);
        ArgumentCaptor<ApprovalRequest> captor = ArgumentCaptor.forClass(ApprovalRequest.class);

        when(quotationRepository.findById(500L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 500L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 900L);
            return saved;
        });

        CreateApprovalRequestResponse response = approvalSubmissionService.submitFromDocumentCreation(
                DealType.QUO,
                500L,
                "QUO-20260312-500",
                principal
        );

        assertThat(response.approvalId()).isEqualTo(900L);
        verify(approvalRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getClientIdSnapshot()).isEqualTo(77L);
        verify(approvalDealLogWriter).writeSubmit(any(ApprovalRequest.class), eq(ActorType.SALES_REP), eq(501L));
    }

    @Test
    @DisplayName("수동 승인 요청 생성은 ADMIN만 허용한다")
    void manualCreateRequiresAdmin() {
        CreateApprovalRequestRequest dto = new CreateApprovalRequestRequest(DealType.QUO, 500L, 77L, "Q-500");

        assertThatThrownBy(() -> approvalSubmissionService.createApprovalRequest(dto, salesRepUser(501L)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("WAITING_ADMIN이 아닌 문서는 자동 승인 요청을 만들 수 없다")
    void submitFromDocumentCreationRejectsNonWaitingAdminDocument() {
        QuotationHeader quotation = quotation(500L, QuotationStatus.REJECTED_ADMIN, 77L, 501L);

        when(quotationRepository.findById(500L)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> approvalSubmissionService.submitFromDocumentCreation(
                DealType.QUO,
                500L,
                "QUO-20260312-500",
                salesRepUser(501L)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_DOCUMENT_STATUS);

        verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
    }

    @Test
    @DisplayName("진행 중 승인 요청이 있으면 자동 승인 요청을 중복 생성하지 않는다")
    void submitFromDocumentCreationRejectsPendingDuplicate() {
        QuotationHeader quotation = quotation(500L, QuotationStatus.WAITING_ADMIN, 77L, 501L);

        when(quotationRepository.findById(500L)).thenReturn(Optional.of(quotation));
        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.QUO, 500L, ApprovalStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> approvalSubmissionService.submitFromDocumentCreation(
                DealType.QUO,
                500L,
                "QUO-20260312-500",
                salesRepUser(501L)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_REQUEST_DUPLICATED);
    }

    private QuotationHeader quotation(Long quotationId, QuotationStatus status, Long clientId, Long ownerEmployeeId) {
        Employee owner = employee(ownerEmployeeId);
        Client client = client(clientId, owner);
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(QuotationStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.QUO)
                .latestRefId(quotationId)
                .latestTargetCode("QUO-" + quotationId)
                .lastActivityAt(java.time.LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
        QuotationHeader quotation = QuotationHeader.create(
                null,
                "QUO-" + quotationId,
                client,
                deal,
                owner,
                BigDecimal.TEN,
                null
        );
        ReflectionTestUtils.setField(quotation, "id", quotationId);
        quotation.updateStatus(status);
        return quotation;
    }

    private Employee employee(Long employeeId) {
        Employee employee = Employee.builder()
                .employeeCode("E-" + employeeId)
                .employeeName("emp-" + employeeId)
                .employeeEmail("emp" + employeeId + "@seedflow.test")
                .employeePhone("010-0000-0000")
                .address("Seoul")
                .build();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        return employee;
    }

    private Client client(Long clientId, Employee manager) {
        Client client = Client.builder()
                .clientCode("C-" + clientId)
                .clientName("client-" + clientId)
                .clientBrn("123-45-67890")
                .ceoName("ceo")
                .companyPhone("02-0000-0000")
                .address("Seoul")
                .latitude(null)
                .longitude(null)
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("manager")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@seedflow.test")
                .managerEmployee(manager)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(client, "id", clientId);
        return client;
    }

    private CustomUserDetails salesRepUser(Long employeeId) {
        return new CustomUserDetails(User.builder()
                .loginId("sales-" + employeeId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee(employeeId))
                .client(null)
                .build());
    }

    private User adminUser(Long userId) {
        User user = User.builder()
                .loginId("admin-" + userId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.ADMIN)
                .employee(employee(999L))
                .client(null)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
