package com.monsoon.seedflowplus.domain.deal.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SalesDealTest {

    @Test
    @DisplayName("deal close는 이미 닫힌 deal에 다시 호출돼도 최초 closedAt을 유지한다")
    void closeIsIdempotent() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("직원")
                .employeeEmail("emp@test.com")
                .employeePhone("010-0000-0000")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(employee, "id", 1L);

        Client client = Client.builder()
                .clientCode("CLIENT-1")
                .clientName("거래처")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(ClientType.NURSERY)
                .managerName("담당")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .managerEmployee(employee)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(client, "id", 1L);

        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.CREATED)
                .currentStatus("PENDING")
                .latestDocType(DealType.RFQ)
                .latestRefId(1L)
                .latestTargetCode("RFQ-1")
                .lastActivityAt(LocalDateTime.now())
                .build();

        LocalDateTime firstClosedAt = LocalDateTime.of(2026, 3, 13, 10, 0);
        LocalDateTime secondClosedAt = firstClosedAt.plusHours(2);

        deal.close(firstClosedAt);
        deal.close(secondClosedAt);

        assertThat(deal.getClosedAt()).isEqualTo(firstClosedAt);
    }
}
