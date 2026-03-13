package com.monsoon.seedflowplus.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduledNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ScheduledNotificationService scheduledNotificationService;

    @Test
    @DisplayName("계약 시작/30일전/종료 알림은 오전 9시 예약 delivery로 생성된다")
    void scheduleContractLifecycleNotificationsAtNineAm() {
        ContractHeader contract = contract(71L, "CNT-20260312-71");
        User user = mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(entityManager.find(User.class, 200L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationDeliveryRepository.existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                any(), any(), eq(NotificationTargetType.CONTRACT), eq(71L), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduledNotificationService.scheduleContractLifecycleNotifications(contract, List.of(100L, 200L));

        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepository, times(6)).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(NotificationDelivery::getScheduledAt)
                .allMatch(dateTime -> dateTime.toLocalTime().equals(java.time.LocalTime.of(9, 0)));
        assertThat(deliveryCaptor.getAllValues())
                .extracting(NotificationDelivery::getScheduledAt)
                .contains(
                        LocalDate.of(2026, 4, 1).atTime(9, 0),
                        LocalDate.of(2026, 4, 15).minusDays(30).atTime(9, 0),
                        LocalDate.of(2026, 4, 15).atTime(9, 0)
                );
    }

    @Test
    @DisplayName("같은 계약/같은 사용자/같은 예약 시각 알림이 이미 있으면 중복 생성하지 않는다")
    void scheduleContractLifecycleNotificationsDeduplicatesByScheduledAt() {
        ContractHeader contract = contract(71L, "CNT-20260312-71");
        User user = mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationDeliveryRepository.existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                any(), any(), eq(NotificationTargetType.CONTRACT), eq(71L), any()))
                .thenReturn(true);

        scheduledNotificationService.scheduleContractLifecycleNotifications(contract, List.of(100L));

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationDeliveryRepository, never()).save(any(NotificationDelivery.class));
    }

    @Test
    @DisplayName("30일 미만 남은 계약은 종료 예정 알림을 과거 시각으로 예약하지 않는다")
    void scheduleContractLifecycleNotificationsSkipsPastEndingSoonNotification() {
        ContractHeader contract = contract(
                72L,
                "CNT-20260312-72",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(10)
        );
        User user = mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationDeliveryRepository.existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                any(), any(), eq(NotificationTargetType.CONTRACT), eq(72L), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduledNotificationService.scheduleContractLifecycleNotifications(contract, List.of(100L));

        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepository, times(2)).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(delivery -> delivery.getNotification().getType())
                .doesNotContain(NotificationType.CONTRACT_ENDING_SOON);
    }

    @Test
    @DisplayName("이미 지난 시작일과 종료일은 예약 알림을 생성하지 않는다")
    void scheduleContractLifecycleNotificationsSkipsPastStartAndEndNotification() {
        ContractHeader contract = contract(
                73L,
                "CNT-20260312-73",
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1)
        );
        scheduledNotificationService.scheduleContractLifecycleNotifications(contract, List.of(100L));

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationDeliveryRepository, never()).save(any(NotificationDelivery.class));
    }

    private ContractHeader contract(Long id, String code) {
        return contract(id, code, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
    }

    private ContractHeader contract(Long id, String code, LocalDate startDate, LocalDate endDate) {
        Client client = Client.builder()
                .clientCode("C-1")
                .clientName("거래처")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("client@test.com")
                .build();
        ReflectionTestUtils.setField(client, "id", 1L);

        Employee employee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("직원")
                .employeeEmail("emp@test.com")
                .employeePhone("010-0000-0000")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(employee, "id", 11L);

        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.APPROVED)
                .currentStatus("COMPLETED")
                .latestDocType(DealType.CNT)
                .latestRefId(1L)
                .latestTargetCode(code)
                .lastActivityAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(deal, "id", 21L);

        ContractHeader contract = ContractHeader.create(
                code,
                null,
                client,
                deal,
                employee,
                BigDecimal.TEN,
                startDate,
                endDate,
                null,
                "terms",
                "memo"
        );
        ReflectionTestUtils.setField(contract, "id", id);
        return contract;
    }
}
