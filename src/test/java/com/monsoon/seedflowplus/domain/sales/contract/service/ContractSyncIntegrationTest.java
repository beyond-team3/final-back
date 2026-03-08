package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
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
        com.monsoon.seedflowplus.domain.account.entity.Employee employee = employeeRepository
                .save(com.monsoon.seedflowplus.domain.account.entity.Employee.builder()
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
                .lastActivityAt(java.time.LocalDateTime.now())
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

        System.out.println("\n[통합 테스트] 시작일/종료일 모두 과거인 엣지 케이스 검증");
        System.out.println("시작일: " + startDate + ", 종료일: " + endDate);
        System.out.println("최종 상태: " + updatedContract.getStatus());

        assertEquals(ContractStatus.EXPIRED, updatedContract.getStatus());
    }
}
