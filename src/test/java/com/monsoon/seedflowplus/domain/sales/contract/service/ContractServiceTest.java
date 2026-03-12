package com.monsoon.seedflowplus.domain.sales.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.monsoon.seedflowplus.domain.approval.service.ApprovalCancellationService;
import com.monsoon.seedflowplus.domain.approval.service.ApprovalSubmissionService;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SalesDealRepository salesDealRepository;
    @Mock
    private DealPipelineFacade dealPipelineFacade;
    @Mock
    private DealLogQueryService dealLogQueryService;
    @Mock
    private ApprovalSubmissionService approvalSubmissionService;
    @Mock
    private ApprovalCancellationService approvalCancellationService;

    @InjectMocks
    private ContractService contractService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("계약 삭제 시 진행 중 승인 요청을 취소하고 상위 문서 상태를 복구한다")
    void deleteContractCancelsPendingApprovalRequest() {
        Employee author = employee(10L);
        Client client = client(30L, author);
        SalesDeal deal = deal(client, author);
        QuotationRequestHeader rfq = QuotationRequestHeader.create(client, "req", deal);
        rfq.updateStatus(QuotationRequestStatus.COMPLETED);
        QuotationHeader quotation = QuotationHeader.create(rfq, "QUO-1", client, deal, author, BigDecimal.TEN, null);
        quotation.updateStatus(QuotationStatus.WAITING_CONTRACT);
        ContractHeader contract = ContractHeader.create(
                "CNT-1",
                quotation,
                client,
                deal,
                author,
                BigDecimal.TEN,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BillingCycle.MONTHLY,
                null,
                null
        );
        ReflectionTestUtils.setField(contract, "id", 200L);

        when(contractRepository.findById(200L)).thenReturn(Optional.of(contract));
        setAuthentication(salesRepUser(author));

        contractService.deleteContract(200L);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DELETED);
        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.FINAL_APPROVED);
        assertThat(rfq.getStatus()).isEqualTo(QuotationRequestStatus.REVIEWING);
        verify(approvalCancellationService).cancelPendingRequest(DealType.CNT, 200L);
    }

    private void setAuthentication(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private CustomUserDetails salesRepUser(Employee employee) {
        return new CustomUserDetails(User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .client(null)
                .build());
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
        return SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(ContractStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.CNT)
                .latestRefId(1L)
                .latestTargetCode("CNT-1")
                .lastActivityAt(java.time.LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
    }
}
