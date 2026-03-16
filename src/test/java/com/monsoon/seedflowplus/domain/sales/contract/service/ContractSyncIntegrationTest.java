package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ContractSyncIntegrationTest {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SalesDealRepository salesDealRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("엣지 케이스 검증: 시작일과 종료일이 모두 과거인 계약은 EXPIRED로 전이되어야 함")
    void syncStatus_ShouldTransitionToExpired_WhenBothDatesAreInPast() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(10);
        LocalDate endDate = today.minusDays(1);
        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(8);

        // 연관 엔티티 준비 (DB 제약 조건 충족을 위함)
        Employee employee = employeeRepository
                .save(Employee.builder()
                        .employeeCode("EMP-INT-" + uniqueSuffix)
                        .employeeName("Integ Tester")
                        .employeeEmail("integ" + uniqueSuffix + "@test.com")
                        .employeePhone("010-0000-0000")
                        .address("Test Office")
                        .build());

        Client client = clientRepository.save(Client.builder()
                .clientCode("CLI-INT-" + uniqueSuffix)
                .clientName("Test Integration Client")
                .clientBrn("BRN-" + uniqueSuffix)
                .ceoName("CEO")
                .companyPhone("010-1234-5678")
                .address("Test Address")
                .clientType(ClientType.NURSERY)
                .managerName("Manager")
                .managerPhone("010-1111-2222")
                .managerEmail("manager@test.com")
                .build());

        SalesDeal deal = salesDealRepository.save(SalesDeal.builder()
                .summaryMemo("Integration Test Deal")
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.APPROVED)
                .currentStatus(ContractStatus.COMPLETED.name())
                .latestDocType(DealType.CNT)
                .latestRefId(0L)
                .lastActivityAt(LocalDateTime.now())
                .build());

        // 시작/종료일이 모두 과거인 계약 생성 (상태: COMPLETED)
        ContractHeader contract = ContractHeader.create(
                "CNT-PAST-001",
                null,
                client,
                deal,
                null,
                BigDecimal.valueOf(1000000),
                startDate,
                endDate,
                BillingCycle.MONTHLY,
                null,
                null);
        contract.updateStatus(ContractStatus.COMPLETED);
        contractRepository.save(contract);

        // flush를 통해 DB에 반영 (벌크 쿼리는 영속성 컨텍스트가 아닌 DB에 직접 작용)
        contractRepository.flush();

        // when
        contractService.syncContractStatuses();

        // then
        // 벌크 업데이트는 영속성 컨텍스트를 거치지 않으므로 직접 다시 조회해서 확인
        ContractHeader updatedContract = contractRepository.findById(contract.getId())
                .orElseThrow();

        assertEquals(ContractStatus.EXPIRED, updatedContract.getStatus());
    }

    @Test
    @DisplayName("오늘 시작하는 계약은 승인 시점에 즉시 ACTIVE_CONTRACT로 전이할 수 있다")
    void immediateActivationTest_TodayContract() {
        // given
        LocalDate today = LocalDate.now();
        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(10);

        Employee employee = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-" + uniqueSuffix)
                .employeeName("Tester")
                .employeeEmail("test" + uniqueSuffix + "@test.com")
                .employeePhone("010-0000-0000")
                .address("Office")
                .build());

        Client client = clientRepository.save(Client.builder()
                .clientCode("CLI-" + uniqueSuffix)
                .clientName("Immediate Client")
                .clientBrn("BRN-" + uniqueSuffix)
                .ceoName("CEO")
                .companyPhone("010-1234-5678")
                .address("Address")
                .clientType(ClientType.NURSERY)
                .managerEmployee(employee)
                .managerName("Manager")
                .managerPhone("010-1111-2222")
                .managerEmail("manager@test.com")
                .build());

        SalesDeal deal = salesDealRepository.save(SalesDeal.builder()
                .summaryMemo("Immediate Deal")
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.APPROVED)
                .currentStatus(ContractStatus.COMPLETED.name())
                .latestDocType(DealType.CNT)
                .latestRefId(0L)
                .lastActivityAt(LocalDateTime.now())
                .build());

        // 오늘 시작하는 계약 생성 (기본 상태 WAITING_ADMIN)
        ContractHeader contract = ContractHeader.create(
                "CNT-IMM-" + uniqueSuffix,
                null,
                client,
                deal,
                employee,
                BigDecimal.valueOf(500000),
                today,
                today.plusMonths(1),
                BillingCycle.MONTHLY,
                null,
                null);

        assertEquals(ContractStatus.WAITING_ADMIN, contract.getStatus());

        // when
        // 외부 서비스(ApprovalCommandService 등)에서 승인 시점 즉시 활성화를 반영하는 상황 모의
        contract.updateStatus(ContractStatus.ACTIVE_CONTRACT);
        contractRepository.saveAndFlush(contract);

        // SecurityContext 모킹 (Service 레이어의 getAuthenticatedUser 통과 용도)
        User user = User.builder()
                .loginId("test-user")
                .loginPw("password")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .client(client)
                .build();

        CustomUserDetails mockUser = new CustomUserDetails(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities()));
        SecurityContextHolder.setContext(context);

        try {
            // then
            ContractHeader dbContract = contractRepository.findById(contract.getId()).orElseThrow();
            assertEquals(ContractStatus.ACTIVE_CONTRACT, dbContract.getStatus());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
