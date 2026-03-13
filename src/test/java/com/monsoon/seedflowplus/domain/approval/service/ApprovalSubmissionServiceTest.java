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
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
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
import org.mockito.Spy;
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
    private OrderHeaderRepository orderHeaderRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @Spy
    private ApprovalFlowPolicy approvalFlowPolicy = new ApprovalFlowPolicy();

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

    @Test
    @DisplayName("주문 생성 자동 제출은 client snapshot과 SALES_REP 1단계 step을 생성한다")
    void submitFromDocumentCreationCreatesApprovalForOrder() {
        OrderHeader order = order(800L, OrderStatus.PENDING, 77L, 501L);
        ArgumentCaptor<ApprovalRequest> captor = ArgumentCaptor.forClass(ApprovalRequest.class);

        when(orderHeaderRepository.findById(800L)).thenReturn(Optional.of(order));
        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.ORD, 800L, ApprovalStatus.PENDING))
                .thenReturn(false);
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 980L);
            return saved;
        });
        when(userRepository.findByEmployeeId(501L)).thenReturn(Optional.of(salesRepEntityUser(7001L, 501L)));

        CreateApprovalRequestResponse response = approvalSubmissionService.submitFromDocumentCreation(
                DealType.ORD,
                800L,
                "ORD-800",
                clientUser(77L)
        );

        assertThat(response.approvalId()).isEqualTo(980L);
        verify(approvalRequestRepository).save(captor.capture());
        ApprovalRequest saved = captor.getValue();
        assertThat(saved.getClientIdSnapshot()).isEqualTo(77L);
        assertThat(saved.getTargetCodeSnapshot()).isEqualTo("ORD-800");
        assertThat(saved.getSteps()).hasSize(1);
        assertThat(saved.getSteps().get(0).getActorType()).isEqualTo(ActorType.SALES_REP);
    }

    @Test
    @DisplayName("PENDING이 아닌 주문은 자동 승인 요청을 만들 수 없다")
    void submitFromDocumentCreationRejectsNonPendingOrder() {
        when(orderHeaderRepository.findById(801L)).thenReturn(Optional.of(order(801L, OrderStatus.CONFIRMED, 77L, 501L)));

        assertThatThrownBy(() -> approvalSubmissionService.submitFromDocumentCreation(
                DealType.ORD,
                801L,
                "ORD-801",
                clientUser(77L)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_DOCUMENT_STATUS);
    }

    @Test
    @DisplayName("본인 거래처 주문이 아니면 자동 승인 요청을 만들 수 없다")
    void submitFromDocumentCreationRejectsOrderOfAnotherClient() {
        when(orderHeaderRepository.findById(802L)).thenReturn(Optional.of(order(802L, OrderStatus.PENDING, 77L, 501L)));
        when(approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(DealType.ORD, 802L, ApprovalStatus.PENDING))
                .thenReturn(false);

        assertThatThrownBy(() -> approvalSubmissionService.submitFromDocumentCreation(
                DealType.ORD,
                802L,
                "ORD-802",
                clientUser(88L)
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.APPROVAL_CLIENT_MISMATCH);
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

    private CustomUserDetails clientUser(Long clientId) {
        return new CustomUserDetails(User.builder()
                .loginId("client-" + clientId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.CLIENT)
                .client(client(clientId, employee(501L)))
                .build());
    }

    private User salesRepEntityUser(Long userId, Long employeeId) {
        User user = User.builder()
                .loginId("sales-entity-" + employeeId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee(employeeId))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
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

    private OrderHeader order(Long orderId, OrderStatus status, Long clientId, Long ownerEmployeeId) {
        Employee owner = employee(ownerEmployeeId);
        Client client = client(clientId, owner);
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.IN_PROGRESS)
                .currentStatus(OrderStatus.PENDING.name())
                .latestDocType(DealType.ORD)
                .latestRefId(orderId)
                .latestTargetCode("ORD-" + orderId)
                .lastActivityAt(java.time.LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
        OrderHeader order = OrderHeader.create(
                org.mockito.Mockito.mock(ContractHeader.class),
                client,
                deal,
                owner,
                "ORD-" + orderId
        );
        ReflectionTestUtils.setField(order, "id", orderId);
        if (status == OrderStatus.CONFIRMED) {
            order.confirm();
        }
        return order;
    }
}
