package com.monsoon.seedflowplus.domain.schedule.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemDto;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import com.monsoon.seedflowplus.domain.schedule.entity.PersonalSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleVisibility;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleSource;
import com.monsoon.seedflowplus.domain.schedule.repository.DealScheduleRepository;
import com.monsoon.seedflowplus.domain.schedule.repository.PersonalScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduleQueryServiceTest {

    @Mock
    private PersonalScheduleRepository personalScheduleRepository;

    @Mock
    private DealScheduleRepository dealScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ScheduleQueryService scheduleQueryService;

    @Test
    @DisplayName("ADMIN은 assignee/client/deal 조합으로 거래 일정을 조회한다")
    void adminCanQueryDealSchedulesByFilters() {
        User admin = user(1L, Role.ADMIN, null, null);
        DealSchedule dealSchedule = dealSchedule(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(dealScheduleRepository
                .findByAssigneeUserIdAndClientIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                        11L, 22L, 33L,
                        LocalDateTime.of(2026, 3, 31, 23, 59),
                        LocalDateTime.of(2026, 3, 1, 0, 0)
                ))
                .thenReturn(List.of(dealSchedule));

        List<ScheduleItemDto> result = scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(1L)
                        .actorRole(Role.ADMIN)
                        .ownerId(1L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .assigneeUserId(11L)
                        .clientId(22L)
                        .dealId(33L)
                        .includePersonal(false)
                        .includeDeal(true)
                        .build()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDealId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("includePersonal/includeDeal이 모두 false면 INVALID_INPUT_VALUE")
    void throwsWhenBothIncludeFlagsFalse() {
        assertThatThrownBy(() -> scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(1L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(false)
                        .includeDeal(false)
                        .build()
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("ownerId가 actorUserId와 다르면 ACCESS_DENIED")
    void throwsAccessDeniedWhenOwnerDiffers() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN, null, null)));

        assertThatThrownBy(() -> scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(1L)
                        .actorRole(Role.ADMIN)
                        .ownerId(2L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(true)
                        .includeDeal(false)
                        .build()
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("SALES_REP에서 employee가 없으면 EMPLOYEE_NOT_LINKED")
    void throwsEmployeeNotLinkedForSalesRep() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.SALES_REP, null, null)));

        assertThatThrownBy(() -> scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(2L)
                        .actorRole(Role.SALES_REP)
                        .ownerId(2L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(false)
                        .includeDeal(true)
                        .build()
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.EMPLOYEE_NOT_LINKED);
    }

    @Test
    @DisplayName("개인 일정 단건 조회는 본인 ACTIVE 일정만 반환한다")
    void getMyScheduleReturnsPersonalItem() {
        PersonalSchedule personalSchedule = PersonalSchedule.builder()
                .owner(user(10L, Role.ADMIN, null, null))
                .title("개인 일정")
                .description("설명")
                .startAt(LocalDateTime.of(2026, 3, 5, 10, 0))
                .endAt(LocalDateTime.of(2026, 3, 5, 11, 0))
                .allDay(false)
                .status(ScheduleStatus.ACTIVE)
                .visibility(ScheduleVisibility.PRIVATE)
                .build();
        ReflectionTestUtils.setField(personalSchedule, "id", 300L);

        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(300L, 10L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.of(personalSchedule));

        ScheduleItemDto result = scheduleQueryService.getMySchedule(300L, 10L);

        assertThat(result.getId()).isEqualTo(300L);
        assertThat(result.getType().name()).isEqualTo("PERSONAL");
        assertThat(result.getOwnerUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("개인 일정 단건 조회는 일정이 없으면 PERSONAL_SCHEDULE_NOT_FOUND")
    void getMyScheduleThrowsWhenScheduleNotFound() {
        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(404L, 10L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleQueryService.getMySchedule(404L, 10L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.PERSONAL_SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("SALES_REP는 assigneeUserId 필터를 전달하면 ACCESS_DENIED")
    void salesRepCannotUseAssigneeFilter() {
        Employee employee = employee(300L);
        User salesRep = user(3L, Role.SALES_REP, employee, null);

        when(userRepository.findById(3L)).thenReturn(Optional.of(salesRep));

        assertThatThrownBy(() -> scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(3L)
                        .actorRole(Role.SALES_REP)
                        .ownerId(3L)
                        .assigneeUserId(999L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(false)
                        .includeDeal(true)
                        .build()
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("CLIENT는 임의 필터를 전달하면 ACCESS_DENIED")
    void clientCannotUseScopedFilters() {
        Client client = client(700L, null);
        User actor = user(4L, Role.CLIENT, null, client);
        when(userRepository.findById(4L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(4L)
                        .actorRole(Role.CLIENT)
                        .ownerId(4L)
                        .clientId(700L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(false)
                        .includeDeal(true)
                        .build()
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("CLIENT는 본인 client scope로 거래 일정을 조회한다")
    void clientQueriesOnlyOwnClientSchedules() {
        Client client = client(701L, null);
        User actor = user(5L, Role.CLIENT, null, client);
        DealSchedule dealSchedule = dealSchedule(501L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));
        when(dealScheduleRepository.findByClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                701L,
                LocalDateTime.of(2026, 3, 31, 23, 59),
                LocalDateTime.of(2026, 3, 1, 0, 0)
        )).thenReturn(List.of(dealSchedule));

        List<ScheduleItemDto> result = scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(5L)
                        .actorRole(Role.CLIENT)
                        .ownerId(5L)
                        .actorClientId(701L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(false)
                        .includeDeal(true)
                        .build()
        );

        assertThat(result).hasSize(1);
        verify(dealScheduleRepository).findByClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                701L,
                LocalDateTime.of(2026, 3, 31, 23, 59),
                LocalDateTime.of(2026, 3, 1, 0, 0)
        );
    }

    @Test
    @DisplayName("SALES_REP는 managerEmployeeId 기준으로 거래 일정을 조회한다")
    void salesRepQueriesByManagerEmployeeId() {
        Employee employee = employee(300L);
        User salesRep = user(3L, Role.SALES_REP, employee, null);
        DealSchedule dealSchedule = dealSchedule(120L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(salesRep));
        when(dealScheduleRepository.findByClientManagerEmployeeIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                300L,
                LocalDateTime.of(2026, 3, 31, 23, 59),
                LocalDateTime.of(2026, 3, 1, 0, 0)
        )).thenReturn(List.of(dealSchedule));

        List<ScheduleItemDto> result = scheduleQueryService.getUnifiedSchedules(
                ScheduleSearchCondition.builder()
                        .actorUserId(3L)
                        .actorRole(Role.SALES_REP)
                        .ownerId(3L)
                        .rangeStart(LocalDateTime.of(2026, 3, 1, 0, 0))
                        .rangeEnd(LocalDateTime.of(2026, 3, 31, 23, 59))
                        .includePersonal(false)
                        .includeDeal(true)
                        .build()
        );

        assertThat(result).hasSize(1);
    }

    private DealSchedule dealSchedule(Long id) {
        Employee owner = employee(120L);
        Client client = client(8L, owner);
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

        User assignee = user(99L, Role.SALES_REP, owner, null);

        DealSchedule schedule = DealSchedule.builder()
                .deal(deal)
                .client(client)
                .assigneeUser(assignee)
                .title("딜 일정")
                .description("설명")
                .startAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .endAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .eventType(DealScheduleEventType.DOC_SUBMITTED)
                .docType(DealDocType.QUO)
                .refDocId(1L)
                .refDealLogId(2L)
                .source(ScheduleSource.AUTO_SYNC)
                .externalKey("ext-key-1")
                .lastSyncedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                .build();
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private User user(Long id, Role role, Employee employee, Client client) {
        User user = User.builder()
                .loginId("u" + id)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(role)
                .employee(employee)
                .client(client)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Employee employee(Long id) {
        Employee employee = Employee.builder()
                .employeeCode("E" + id)
                .employeeName("emp")
                .employeeEmail("emp@test.com")
                .employeePhone("010")
                .address("seoul")
                .build();
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private Client client(Long id, Employee managerEmployee) {
        Client client = Client.builder()
                .clientCode("C" + id)
                .clientName("client")
                .clientBrn("123-45-67890")
                .ceoName("ceo")
                .companyPhone("02")
                .address("seoul")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("manager")
                .managerPhone("010")
                .managerEmail("manager@test.com")
                .managerEmployee(managerEmployee)
                .build();
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }
}
