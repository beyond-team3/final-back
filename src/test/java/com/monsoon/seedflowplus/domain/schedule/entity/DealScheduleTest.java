package com.monsoon.seedflowplus.domain.schedule.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DealScheduleTest {

    @Test
    @DisplayName("DealSchedule은 title이 비어 있으면 IllegalArgumentException(title)을 던진다")
    void throwsIllegalArgumentExceptionWhenTitleBlank() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> DealSchedule.builder()
                .deal(fixture.deal)
                .client(fixture.client)
                .assigneeUser(fixture.assignee)
                .title(" ")
                .description("설명")
                .startAt(LocalDateTime.of(2026, 3, 12, 10, 0))
                .endAt(LocalDateTime.of(2026, 3, 12, 11, 0))
                .eventType(DealScheduleEventType.DOC_SUBMITTED)
                .docType(DealDocType.QUO)
                .source(ScheduleSource.AUTO_SYNC)
                .status(DealScheduleStatus.ACTIVE)
                .externalKey("ext-1")
                .lastSyncedAt(LocalDateTime.of(2026, 3, 12, 9, 0))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title");
    }

    @Test
    @DisplayName("DealSchedule은 client가 null이면 IllegalArgumentException(client)을 던진다")
    void throwsIllegalArgumentExceptionWhenClientNull() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> DealSchedule.builder()
                .deal(fixture.deal)
                .client(null)
                .assigneeUser(fixture.assignee)
                .title("일정")
                .description("설명")
                .startAt(LocalDateTime.of(2026, 3, 12, 10, 0))
                .endAt(LocalDateTime.of(2026, 3, 12, 11, 0))
                .eventType(DealScheduleEventType.DOC_SUBMITTED)
                .docType(DealDocType.QUO)
                .source(ScheduleSource.AUTO_SYNC)
                .status(DealScheduleStatus.ACTIVE)
                .externalKey("ext-1")
                .lastSyncedAt(LocalDateTime.of(2026, 3, 12, 9, 0))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("client");
    }

    private Fixture fixture() {
        Employee owner = Employee.builder()
                .employeeCode("E-1")
                .employeeName("sales")
                .employeeEmail("sales@test.com")
                .employeePhone("010")
                .address("seoul")
                .build();
        ReflectionTestUtils.setField(owner, "id", 10L);

        Client client = Client.builder()
                .clientCode("C-1")
                .clientName("client")
                .clientBrn("123-45-67890")
                .ceoName("ceo")
                .companyPhone("02")
                .address("seoul")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("manager")
                .managerPhone("010")
                .managerEmail("manager@test.com")
                .managerEmployee(owner)
                .build();
        ReflectionTestUtils.setField(client, "id", 8L);

        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.QUO)
                .latestRefId(1L)
                .latestTargetCode("QUO-1")
                .lastActivityAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(deal, "id", 7L);

        User assignee = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(owner)
                .build();
        ReflectionTestUtils.setField(assignee, "id", 99L);

        return new Fixture(deal, client, assignee);
    }

    private record Fixture(SalesDeal deal, Client client, User assignee) {
    }
}
