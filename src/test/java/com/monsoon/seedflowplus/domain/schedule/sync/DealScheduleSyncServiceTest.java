package com.monsoon.seedflowplus.domain.schedule.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleSource;
import com.monsoon.seedflowplus.domain.schedule.repository.DealScheduleRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DealScheduleSyncServiceTest {

    @Mock
    private DealScheduleRepository dealScheduleRepository;

    @Mock
    private DealScheduleReferenceReader dealScheduleReferenceReader;

    @InjectMocks
    private DealScheduleSyncService dealScheduleSyncService;

    @Test
    @DisplayName("externalKey가 없으면 신규 거래 일정을 생성한다")
    void createsWhenExternalKeyNotExists() {
        Fixture fixture = fixture(100L, 200L, 300L);
        DealScheduleUpsertCommand command = command("ext-new", fixture.deal.getId(), fixture.client.getId(), fixture.assignee.getId());

        mockReferences(fixture);
        when(dealScheduleRepository.findByExternalKey("ext-new")).thenReturn(Optional.empty());
        when(dealScheduleRepository.saveAndFlush(any(DealSchedule.class))).thenAnswer(invocation -> {
            DealSchedule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 900L);
            return saved;
        });

        Long id = dealScheduleSyncService.upsertFromEvent(command);

        assertThat(id).isEqualTo(900L);
    }

    @Test
    @DisplayName("externalKey가 있으면 기존 거래 일정을 syncUpdate한다")
    void updatesWhenExternalKeyExists() {
        Fixture fixture = fixture(101L, 201L, 301L);
        DealSchedule existing = existingSchedule("ext-exists", fixture);
        DealScheduleUpsertCommand command = command("ext-exists", fixture.deal.getId(), fixture.client.getId(), fixture.assignee.getId());

        mockReferences(fixture);
        when(dealScheduleRepository.findByExternalKey("ext-exists")).thenReturn(Optional.of(existing));
        when(dealScheduleRepository.saveAndFlush(existing)).thenReturn(existing);

        Long id = dealScheduleSyncService.upsertFromEvent(command);

        assertThat(id).isEqualTo(777L);
        assertThat(existing.getTitle()).isEqualTo("방문 일정");
        assertThat(existing.getSource()).isEqualTo(ScheduleSource.AUTO_SYNC);
    }

    @Test
    @DisplayName("신규 insert 충돌(DataIntegrityViolationException) 시 재조회 후 update로 재시도한다")
    void retriesOnDataIntegrityViolation() {
        Fixture fixture = fixture(102L, 202L, 302L);
        DealSchedule existing = existingSchedule("ext-race", fixture);
        DealScheduleUpsertCommand command = command("ext-race", fixture.deal.getId(), fixture.client.getId(), fixture.assignee.getId());

        mockReferences(fixture);
        when(dealScheduleRepository.findByExternalKey("ext-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(dealScheduleRepository.saveAndFlush(any(DealSchedule.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate external_key"));
        when(dealScheduleRepository.save(existing)).thenReturn(existing);

        Long id = dealScheduleSyncService.upsertFromEvent(command);

        assertThat(id).isEqualTo(777L);
        verify(dealScheduleRepository).save(existing);
    }

    @Test
    @DisplayName("deal.client.id와 command.clientId가 다르면 INVALID_INPUT_VALUE")
    void throwsWhenDealClientMismatch() {
        Fixture fixture = fixture(103L, 203L, 303L);
        Client anotherClient = client(999L, fixture.owner);
        DealScheduleUpsertCommand command = command("ext-mismatch", fixture.deal.getId(), anotherClient.getId(), fixture.assignee.getId());

        when(dealScheduleReferenceReader.loadForSync(fixture.deal.getId(), anotherClient.getId(), fixture.assignee.getId()))
                .thenReturn(new DealScheduleReferenceReader.DealScheduleReferences(fixture.deal, anotherClient, fixture.assignee));

        assertThatThrownBy(() -> dealScheduleSyncService.upsertFromEvent(command))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);
    }

    private DealScheduleUpsertCommand command(String externalKey, Long dealId, Long clientId, Long assigneeUserId) {
        return new DealScheduleUpsertCommand(
                externalKey,
                dealId,
                clientId,
                assigneeUserId,
                DealScheduleEventType.DOC_SUBMITTED,
                DealDocType.QUOTATION,
                12L,
                34L,
                "방문 일정",
                "설명",
                LocalDateTime.of(2026, 3, 12, 10, 0),
                LocalDateTime.of(2026, 3, 12, 11, 0),
                LocalDateTime.of(2026, 3, 12, 9, 0)
        );
    }

    private DealSchedule existingSchedule(String externalKey, Fixture fixture) {
        DealSchedule schedule = DealSchedule.builder()
                .deal(fixture.deal)
                .client(fixture.client)
                .assigneeUser(fixture.assignee)
                .title("old title")
                .description("old")
                .startAt(LocalDateTime.of(2026, 3, 11, 10, 0))
                .endAt(LocalDateTime.of(2026, 3, 11, 11, 0))
                .eventType(DealScheduleEventType.DOC_CREATED)
                .docType(DealDocType.QUOTATION)
                .refDocId(1L)
                .refDealLogId(2L)
                .source(ScheduleSource.AUTO_SYNC)
                .externalKey(externalKey)
                .lastSyncedAt(LocalDateTime.of(2026, 3, 11, 9, 0))
                .build();
        ReflectionTestUtils.setField(schedule, "id", 777L);
        return schedule;
    }

    private Fixture fixture(Long dealId, Long clientId, Long userId) {
        Employee owner = employee(10L);
        Client client = client(clientId, owner);

        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.QUO)
                .latestRefId(1L)
                .latestTargetCode("QUO-1")
                .lastActivityAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                .build();
        ReflectionTestUtils.setField(deal, "id", dealId);

        User assignee = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(owner)
                .build();
        ReflectionTestUtils.setField(assignee, "id", userId);

        return new Fixture(deal, client, assignee, owner);
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

    private Client client(Long id, Employee owner) {
        Client client = Client.builder()
                .clientCode("C" + id)
                .clientName("client")
                .clientBrn("123-45-6789" + (id % 10))
                .ceoName("ceo")
                .companyPhone("02")
                .address("seoul")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("manager")
                .managerPhone("010")
                .managerEmail("manager@test.com")
                .managerEmployee(owner)
                .build();
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    private void mockReferences(Fixture fixture) {
        when(dealScheduleReferenceReader.loadForSync(fixture.deal.getId(), fixture.client.getId(), fixture.assignee.getId()))
                .thenReturn(new DealScheduleReferenceReader.DealScheduleReferences(fixture.deal, fixture.client, fixture.assignee));
    }

    private record Fixture(SalesDeal deal, Client client, User assignee, Employee owner) {
    }
}
