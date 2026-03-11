package com.monsoon.seedflowplus.domain.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContractApprovalSchedulesSyncEventHandlerTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DealScheduleSyncService dealScheduleSyncService;

    @InjectMocks
    private ContractApprovalSchedulesSyncEventHandler handler;

    @Test
    @DisplayName("계약 승인 이벤트 처리 시 계약을 조회해 시작일/만료일 일정을 upsert한다")
    void handleBuildsCommandsFromDeferredLookup() {
        ContractHeader contract = contract(8001L, 101L, salesDeal(501L));
        User ownerUser = user(9301L, contract.getDeal().getOwnerEmp());
        ContractApprovalSchedulesSyncEvent event = new ContractApprovalSchedulesSyncEvent(
                8001L,
                DealType.CNT,
                2,
                ActorType.CLIENT,
                DecisionType.APPROVE,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                9101L
        );

        when(contractRepository.findById(8001L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmployeeId(501L)).thenReturn(Optional.of(ownerUser));

        handler.handle(event);

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor =
                ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService, times(2)).upsertFromEvent(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(DealScheduleUpsertCommand::externalKey)
                .containsExactly("CNT_8001_DOC_APPROVED_START", "CNT_8001_DOC_APPROVED_END");
        assertThat(commandCaptor.getAllValues())
                .extracting(DealScheduleUpsertCommand::assigneeUserId)
                .containsOnly(9301L);
    }

    @Test
    @DisplayName("담당자 해석이 필요 없고 계약일이 없으면 stable key로 삭제만 수행한다")
    void handleDeletesStableKeysWhenDatesAreNull() {
        ContractHeader contract = contract(8002L, 101L, salesDeal(501L));
        ReflectionTestUtils.setField(contract, "startDate", null);
        ReflectionTestUtils.setField(contract, "endDate", null);
        ContractApprovalSchedulesSyncEvent event = new ContractApprovalSchedulesSyncEvent(
                8002L,
                DealType.CNT,
                2,
                ActorType.CLIENT,
                DecisionType.APPROVE,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                null
        );

        when(contractRepository.findById(8002L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmployeeId(501L)).thenReturn(Optional.empty());
        when(userRepository.findByClientId(101L)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(dealScheduleSyncService).deleteByExternalKey("CNT_8002_DOC_APPROVED_START");
        verify(dealScheduleSyncService).deleteByExternalKey("CNT_8002_DOC_APPROVED_END");
        verify(dealScheduleSyncService, never()).upsertFromEvent(org.mockito.ArgumentMatchers.any());
    }

    private ContractHeader contract(Long id, Long clientId, SalesDeal deal) {
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
        ReflectionTestUtils.setField(contract, "startDate", LocalDate.of(2026, 3, 11));
        ReflectionTestUtils.setField(contract, "endDate", LocalDate.of(2026, 3, 20));
        contract.updateStatus(ContractStatus.WAITING_CLIENT);
        return contract;
    }

    private SalesDeal salesDeal(Long ownerEmployeeId) {
        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        Employee owner = employee(ownerEmployeeId);
        when(deal.getId()).thenReturn(9900L + ownerEmployeeId);
        when(deal.getOwnerEmp()).thenReturn(owner);
        return deal;
    }

    private Employee employee(Long id) {
        Employee employee = Employee.builder()
                .employeeCode("E" + id)
                .employeeName("sales")
                .employeeEmail("sales@test.com")
                .employeePhone("010")
                .address("seoul")
                .build();
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private User user(Long id, Employee employee) {
        User user = User.builder()
                .loginId("sales-" + id)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
