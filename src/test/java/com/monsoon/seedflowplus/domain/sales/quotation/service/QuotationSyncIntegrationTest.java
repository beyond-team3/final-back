package com.monsoon.seedflowplus.domain.sales.quotation.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class QuotationSyncIntegrationTest {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRequestRepository quotationRequestRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SalesDealRepository salesDealRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("RFQ 상태 유지 테스트: 모든 견적이 만료되어도 재작성을 위해 REVIEWING 상태가 유지되어야 함")
    void persistStatus_Reviewing() {
        // given
        String uniqueSuffix = "REC1-" + System.currentTimeMillis() % 10000;

        Employee employee = createEmployee(uniqueSuffix);
        Client client = createClient(uniqueSuffix);
        SalesDeal deal = createDeal(client, employee);

        // RFQ 생성
        QuotationRequestHeader rfq = QuotationRequestHeader.create(client, "Test Requirements 1", deal);
        rfq.updateStatus(QuotationRequestStatus.REVIEWING);
        quotationRequestRepository.save(rfq);

        // 만료될 예정인 승인 대기 견적 생성
        QuotationHeader expiredQuo = QuotationHeader.create(rfq, "QUO-EXP-" + uniqueSuffix, client, deal, employee,
                BigDecimal.valueOf(100000), "Expired Memo");
        // 만료일을 어제로 설정하여 만료 대상이 되도록 함
        ReflectionTestUtils.setField(expiredQuo, "expiredDate", LocalDate.now().minusDays(1));
        quotationRepository.save(expiredQuo);
        quotationRepository.flush();

        // when
        quotationService.syncQuotationStatuses();

        // then
        // 1. 견적서 상태가 EXPIRED로 변경되었는지 확인
        QuotationHeader updatedQuo = quotationRepository.findById(expiredQuo.getId()).orElseThrow();
        assertEquals(QuotationStatus.EXPIRED, updatedQuo.getStatus());

        // 2. RFQ 상태가 여전히 REVIEWING인지 확인
        QuotationRequestHeader updatedRfq = quotationRequestRepository.findById(rfq.getId()).orElseThrow();
        assertEquals(QuotationRequestStatus.REVIEWING, updatedRfq.getStatus());
    }

    @Test
    @DisplayName("RFQ 상태 유지 테스트: 진행 중인 견적이 하나라도 있으면 REVIEWING 상태가 그대로 유지되어야 함")
    void persistStatus_NoChange() {
        // given
        String uniqueSuffix = "REC2-" + System.currentTimeMillis() % 10000;

        Employee employee = createEmployee(uniqueSuffix);
        Client client = createClient(uniqueSuffix);
        SalesDeal deal = createDeal(client, employee);

        // RFQ 생성
        QuotationRequestHeader rfq = QuotationRequestHeader.create(client, "Test Requirements 2", deal);
        rfq.updateStatus(QuotationRequestStatus.REVIEWING);
        quotationRequestRepository.save(rfq);

        // 1. 만료된 견적 생성
        QuotationHeader expiredQuo = QuotationHeader.create(rfq, "QUO-EXP-" + uniqueSuffix, client, deal, employee,
                BigDecimal.valueOf(100000), "Expired Memo");
        expiredQuo.updateStatus(QuotationStatus.EXPIRED);
        quotationRepository.save(expiredQuo);

        // 2. 진행 중인 견적 생성 (WAITING_ADMIN)
        QuotationHeader activeQuo = QuotationHeader.create(rfq, "QUO-ACT-" + uniqueSuffix, client, deal, employee,
                BigDecimal.valueOf(200000), "Active Memo");
        activeQuo.updateStatus(QuotationStatus.WAITING_ADMIN);
        quotationRepository.save(activeQuo);

        quotationRepository.flush();

        // when
        quotationService.syncQuotationStatuses();

        // then
        QuotationRequestHeader updatedRfq = quotationRequestRepository.findById(rfq.getId()).orElseThrow();
        // 진행 중인 견적이 있으므로 REVIEWING 상태를 유지해야 함
        assertEquals(QuotationRequestStatus.REVIEWING, updatedRfq.getStatus());
    }

    private Employee createEmployee(String suffix) {
        return employeeRepository.save(Employee.builder()
                .employeeCode("EMP-" + suffix)
                .employeeName("Tester")
                .employeeEmail("test" + suffix + "@test.com")
                .employeePhone("010-0000-0000")
                .address("Office")
                .build());
    }

    private Client createClient(String suffix) {
        return clientRepository.save(Client.builder()
                .clientCode("CLI-" + suffix)
                .clientName("Client-" + suffix)
                .clientBrn("BRN-" + suffix)
                .ceoName("CEO")
                .companyPhone("010-1234-5678")
                .address("Address")
                .clientType(ClientType.NURSERY)
                .managerName("Manager")
                .managerPhone("010-1111-2222")
                .managerEmail("manager@test.com")
                .build());
    }

    private SalesDeal createDeal(Client client, Employee employee) {
        return salesDealRepository.save(SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(QuotationStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.QUO)
                .latestRefId(0L)
                .lastActivityAt(LocalDateTime.now())
                .build());
    }
}
