package com.monsoon.seedflowplus.domain.sales.quotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalDecisionRepository;
import com.monsoon.seedflowplus.domain.approval.service.ApprovalCancellationService;
import com.monsoon.seedflowplus.domain.approval.service.ApprovalSubmissionService;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestDetail;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private QuotationRequestRepository quotationRequestRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SalesDealRepository salesDealRepository;
    @Mock
    private DealPipelineFacade dealPipelineFacade;
    @Mock
    private DealLogWriteService dealLogWriteService;
    @Mock
    private ApprovalDecisionRepository approvalDecisionRepository;
    @Mock
    private DealLogQueryService dealLogQueryService;
    @Mock
    private ApprovalSubmissionService approvalSubmissionService;
    @Mock
    private ApprovalCancellationService approvalCancellationService;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private DealScheduleSyncService dealScheduleSyncService;

    @InjectMocks
    private QuotationService quotationService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        org.mockito.Mockito.lenient().when(contractRepository.findByDealId(any())).thenReturn(java.util.List.of());
        org.mockito.Mockito.lenient().when(quotationRepository.findByDealId(any())).thenReturn(java.util.List.of());
        org.mockito.Mockito.lenient().when(quotationRequestRepository.findByDealId(any())).thenReturn(java.util.List.of());
        org.mockito.Mockito.lenient().when(approvalDecisionRepository.findReasonsByTargets(any(), any())).thenReturn(java.util.List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("견적 삭제 시 진행 중 승인 요청을 취소한다")
    void deleteQuotationCancelsPendingApprovalRequest() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal deal = deal(client, author);
        QuotationRequestHeader rfq = QuotationRequestHeader.create(client, "req", deal);
        ReflectionTestUtils.setField(rfq, "id", 30L);
        rfq.updateStatus(QuotationRequestStatus.REVIEWING);
        QuotationHeader quotation = QuotationHeader.create(rfq, "QUO-1", client, deal, author, BigDecimal.TEN, null);
        ReflectionTestUtils.setField(quotation, "id", 100L);

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(quotation));
        when(quotationRequestRepository.findByDealId(any())).thenReturn(java.util.List.of(rfq));
        setAuthentication(salesRepUser(author));

        quotationService.deleteQuotation(100L);

        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.DELETED);
        assertThat(rfq.getStatus()).isEqualTo(QuotationRequestStatus.PENDING);
        assertThat(deal.getCurrentStatus()).isEqualTo(QuotationRequestStatus.PENDING.name());
        assertThat(deal.getLatestDocType()).isEqualTo(DealType.RFQ);
        assertThat(deal.getLatestRefId()).isEqualTo(rfq.getId());
        verify(approvalCancellationService).cancelPendingRequest(DealType.QUO, 100L);
        verify(dealScheduleSyncService).deleteByExternalKey("QUO_100_EXPIRATION");
        verify(dealLogWriteService).write(
                eq(deal),
                eq(DealType.QUO),
                eq(100L),
                eq("QUO-1"),
                eq(DealStage.PENDING_ADMIN),
                eq(DealStage.CREATED),
                eq(QuotationStatus.WAITING_ADMIN.name()),
                eq(QuotationRequestStatus.PENDING.name()),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActionType.CANCEL),
                any(),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActorType.SALES_REP),
                eq(10L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.<java.util.List<DealLogWriteService.DiffField>>any()
        );
    }

    @Test
    @DisplayName("견적 생성 시 만료일 deal 일정을 생성한다")
    void createQuotationCreatesExpirationSchedule() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal deal = deal(client, author);

        when(clientRepository.findByIdWithLock(30L)).thenReturn(Optional.of(client));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(author));
        when(salesDealRepository.save(any(SalesDeal.class))).thenAnswer(invocation -> {
            SalesDeal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 500L);
            return saved;
        });
        when(salesDealRepository.findByIdWithLock(500L)).thenReturn(Optional.of(deal));
        when(quotationRepository.save(any(QuotationHeader.class))).thenAnswer(invocation -> {
            QuotationHeader saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            ReflectionTestUtils.setField(saved, "expiredDate", LocalDate.now().plusDays(30));
            return saved;
        });

        setAuthentication(salesRepUser(author));

        quotationService.createQuotation(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest(
                null,
                null,
                30L,
                java.util.List.of(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest.QuotationItemRequest(
                        null,
                        "양배추",
                        "VEG",
                        2,
                        "BOX",
                        BigDecimal.valueOf(1500)
                )),
                "memo"
        ));

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService).upsertFromEvent(commandCaptor.capture());
        DealScheduleUpsertCommand command = commandCaptor.getValue();
        assertThat(command.externalKey()).isEqualTo("QUO_100_EXPIRATION");
        assertThat(command.title()).isEqualTo("견적 만료일: 거래처");
        assertThat(command.startAt()).isEqualTo(LocalDate.now().plusDays(30).atStartOfDay());
        assertThat(command.endAt()).isEqualTo(LocalDate.now().plusDays(31).atStartOfDay());
        verify(salesDealRepository, never()).findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(anyLong());
    }

    @Test
    @DisplayName("참조 RFQ 없는 신규 견적은 새 deal을 생성한다")
    void createQuotationWithoutRequestCreatesNewDeal() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal persistedDeal = deal(client, author);

        when(clientRepository.findByIdWithLock(30L)).thenReturn(Optional.of(client));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(author));
        when(salesDealRepository.save(any(SalesDeal.class))).thenAnswer(invocation -> {
            SalesDeal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 500L);
            return saved;
        });
        when(salesDealRepository.findByIdWithLock(500L)).thenReturn(Optional.of(persistedDeal));
        when(quotationRepository.save(any(QuotationHeader.class))).thenAnswer(invocation -> {
            QuotationHeader saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            ReflectionTestUtils.setField(saved, "expiredDate", LocalDate.now().plusDays(30));
            return saved;
        });

        setAuthentication(salesRepUser(author));

        quotationService.createQuotation(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest(
                null,
                null,
                30L,
                java.util.List.of(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest.QuotationItemRequest(
                        null,
                        "양배추",
                        "VEG",
                        2,
                        "BOX",
                        BigDecimal.valueOf(1500)
                )),
                "memo"
        ));

        verify(salesDealRepository).save(any(SalesDeal.class));
        verify(salesDealRepository, never()).findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(anyLong());
    }

    @Test
    @DisplayName("deal 없는 RFQ 기반 견적도 새 deal을 생성한다")
    void createQuotationFromRequestWithoutDealCreatesNewDeal() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal requestDeal = deal(client, author);
        SalesDeal persistedDeal = deal(client, author);
        QuotationRequestHeader request = QuotationRequestHeader.create(client, "req", requestDeal);
        request.addItem(new QuotationRequestDetail(null, "VEG", "양배추", 2, "BOX"));
        ReflectionTestUtils.setField(request, "id", 40L);
        ReflectionTestUtils.setField(request, "deal", null);

        when(clientRepository.findByIdWithLock(30L)).thenReturn(Optional.of(client));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(author));
        when(quotationRequestRepository.findByIdWithLock(40L)).thenReturn(Optional.of(request));
        when(salesDealRepository.save(any(SalesDeal.class))).thenAnswer(invocation -> {
            SalesDeal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 500L);
            return saved;
        });
        when(salesDealRepository.findByIdWithLock(500L)).thenReturn(Optional.of(persistedDeal));
        when(quotationRepository.save(any(QuotationHeader.class))).thenAnswer(invocation -> {
            QuotationHeader saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 102L);
            ReflectionTestUtils.setField(saved, "expiredDate", LocalDate.now().plusDays(30));
            return saved;
        });

        setAuthentication(salesRepUser(author));

        quotationService.createQuotation(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest(
                40L,
                null,
                30L,
                java.util.List.of(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest.QuotationItemRequest(
                        null,
                        "양배추",
                        "VEG",
                        2,
                        "BOX",
                        BigDecimal.valueOf(1500)
                )),
                "memo"
        ));

        verify(salesDealRepository).save(any(SalesDeal.class));
        verify(salesDealRepository, never()).findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(anyLong());
    }

    @Test
    @DisplayName("반려된 견적서 재작성은 원본 deal을 계승한다")
    void createQuotationFromRejectedQuotationUsesSameDeal() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal deal = deal(client, author);
        ReflectionTestUtils.setField(deal, "id", 500L);
        QuotationHeader source = QuotationHeader.create(null, "QUO-OLD", client, deal, author, BigDecimal.TEN, "old");
        ReflectionTestUtils.setField(source, "id", 200L);
        source.updateStatus(QuotationStatus.REJECTED_ADMIN);

        when(clientRepository.findByIdWithLock(30L)).thenReturn(Optional.of(client));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(author));
        when(quotationRepository.findById(200L)).thenReturn(Optional.of(source));
        when(quotationRepository.findTopByRevisionGroupKeyOrderByRevisionNoDesc("QUO-200")).thenReturn(Optional.empty());
        when(salesDealRepository.findByIdWithLock(500L)).thenReturn(Optional.of(deal));
        when(quotationRepository.save(any(QuotationHeader.class))).thenAnswer(invocation -> {
            QuotationHeader saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 201L);
            ReflectionTestUtils.setField(saved, "expiredDate", LocalDate.now().plusDays(30));
            return saved;
        });

        setAuthentication(salesRepUser(author));

        quotationService.createQuotation(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest(
                null,
                200L,
                30L,
                java.util.List.of(new com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest.QuotationItemRequest(
                        null,
                        "양배추",
                        "VEG",
                        2,
                        "BOX",
                        BigDecimal.valueOf(1500)
                )),
                "rewrite"
        ));

        ArgumentCaptor<QuotationHeader> quotationCaptor = ArgumentCaptor.forClass(QuotationHeader.class);
        verify(quotationRepository).save(quotationCaptor.capture());
        QuotationHeader rewritten = quotationCaptor.getValue();
        assertThat(rewritten.getDeal()).isEqualTo(deal);
        assertThat(rewritten.getSourceDocumentId()).isEqualTo(200L);
        assertThat(rewritten.getRevisionGroupKey()).isEqualTo("QUO-200");
        assertThat(rewritten.getRevisionNo()).isEqualTo(1);
        verify(salesDealRepository, never()).save(any(SalesDeal.class));
    }

    @Test
    @DisplayName("deal 없는 견적도 삭제할 수 있다")
    void deleteQuotationWithoutDealSkipsDealSideEffects() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal deal = deal(client, author);
        QuotationHeader quotation = QuotationHeader.create(null, "QUO-2", client, deal, author, BigDecimal.TEN, null);
        ReflectionTestUtils.setField(quotation, "id", 101L);
        ReflectionTestUtils.setField(quotation, "deal", null);

        when(quotationRepository.findById(101L)).thenReturn(Optional.of(quotation));
        setAuthentication(salesRepUser(author));

        quotationService.deleteQuotation(101L);

        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.DELETED);
        verify(approvalCancellationService).cancelPendingRequest(DealType.QUO, 101L);
        org.mockito.Mockito.verifyNoInteractions(dealLogWriteService);
    }

    @Test
    @DisplayName("반려된 견적서 목록 조회 시 필드가 누락되어도 NPE가 발생하지 않는다")
    void getRejectedQuotationsReturnsListEvenWithNullFields() {
        Employee author = employee(10L);
        // managerEmployee가 null인 상황 가정
        Client client = client(30L, null);
        SalesDeal deal = deal(client, author);

        // author가 null인 견적서 생성 (create 메서드 사용)
        QuotationHeader q = QuotationHeader.create(
                null, "QUO-ERR", client, deal, null, BigDecimal.ZERO, "memo"
        );
        ReflectionTestUtils.setField(q, "id", 200L);
        ReflectionTestUtils.setField(q, "createdAt", java.time.LocalDateTime.now());

        when(quotationRepository.findActiveRejectedQuotations(anyLong(), anyList()))
                .thenReturn(java.util.List.of(q));
        setAuthentication(salesRepUser(author));

        java.util.List<com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationListResponse> result =
                quotationService.getRejectedQuotations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).authorId()).isNull();
        assertThat(result.get(0).clientName()).isEqualTo("거래처");
    }

    private void setAuthentication(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private CustomUserDetails salesRepUser(Employee employee) {
        User user = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .client(null)
                .build();
        ReflectionTestUtils.setField(user, "id", 77L);
        return new CustomUserDetails(user);
    }

    private Employee employee(Long id) {
        Employee employee = Employee.builder()
                .employeeCode("EMP-" + id)
                .employeeName("직원")
                .employeeEmail("emp@test.com")
                .employeePhone("010-0000-0000")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private Client client(Long id, Employee manager) {
        Client client = Client.builder()
                .clientCode("CLIENT-" + id)
                .clientName("거래처")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(ClientType.NURSERY)
                .managerName("담당")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .managerEmployee(manager)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    private SalesDeal deal(Client client, Employee employee) {
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(QuotationStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.QUO)
                .latestRefId(1L)
                .latestTargetCode("QUO-1")
                .lastActivityAt(java.time.LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
        ReflectionTestUtils.setField(deal, "id", 500L);
        return deal;
    }
}
