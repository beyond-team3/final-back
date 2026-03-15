package com.monsoon.seedflowplus.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.event.ContractCompletedEvent;
import com.monsoon.seedflowplus.domain.notification.event.InvoiceIssuedEvent;
import com.monsoon.seedflowplus.domain.notification.event.QuotationRequestCreatedEvent;
import com.monsoon.seedflowplus.domain.notification.event.StatementIssuedEvent;
import com.monsoon.seedflowplus.domain.notification.event.AccountActivatedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ProductCreatedEvent;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DocumentNotificationService documentNotificationService;

    @Test
    @DisplayName("견적요청서 생성 알림은 담당 영업사원 1명 대상 문서 알림으로 저장된다")
    void createQuotationRequestCreatedNotification() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 12, 13, 0);
        QuotationRequestCreatedEvent event = new QuotationRequestCreatedEvent(
                100L,
                31L,
                "RFQ-20260312-31",
                "새봄농산",
                occurredAt
        );
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(100L),
                eq(NotificationType.QUOTATION_REQUEST_CREATED),
                eq(NotificationTargetType.QUOTATION_REQUEST),
                eq(31L),
                any(),
                any()
        )).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 801L);
            ReflectionTestUtils.setField(notification, "createdAt", occurredAt);
            return notification;
        });

        Notification saved = documentNotificationService.createQuotationRequestCreatedNotification(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(notificationDeliveryRepository).save(any(NotificationDelivery.class));

        Notification notification = notificationCaptor.getValue();
        assertThat(saved).isNotNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.QUOTATION_REQUEST_CREATED);
        assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.QUOTATION_REQUEST);
        assertThat(notification.getTargetId()).isEqualTo(31L);
        assertThat(notification.getTitle()).isEqualTo("견적요청서가 접수되었습니다");
        assertThat(notification.getContent()).contains("새봄농산");
        assertThat(notification.getContent()).contains("RFQ-20260312-31");
    }

    @Test
    @DisplayName("계약 체결 알림은 계약 문서 기준으로 저장된다")
    void createContractCompletedNotification() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 12, 16, 0);
        ContractCompletedEvent event = new ContractCompletedEvent(101L, 71L, "CNT-20260312-71", "새봄농산", occurredAt);
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 101L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(101L), eq(NotificationType.CONTRACT_COMPLETED), eq(NotificationTargetType.CONTRACT), eq(71L), any(), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = documentNotificationService.createContractCompletedNotification(event);

        assertThat(saved).isNotNull();
        assertThat(saved.getType()).isEqualTo(NotificationType.CONTRACT_COMPLETED);
        assertThat(saved.getTargetType()).isEqualTo(NotificationTargetType.CONTRACT);
    }

    @Test
    @DisplayName("명세서 발급 알림과 청구서 발행 알림은 기존 SSE payload와 호환되는 targetType을 사용한다")
    void createStatementAndInvoiceNotifications() {
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 201L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(entityManager.find(User.class, 202L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(201L), eq(NotificationType.STATEMENT_ISSUED), eq(NotificationTargetType.STATEMENT), eq(81L), any(), any()))
                .thenReturn(false);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(202L), eq(NotificationType.INVOICE_ISSUED), eq(NotificationTargetType.INVOICE), eq(91L), any(), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification statementNotification = documentNotificationService.createStatementIssuedNotification(
                new StatementIssuedEvent(201L, 81L, "STMT-20260312-81", "ORD-20260312-41", LocalDateTime.now()));
        Notification invoiceNotification = documentNotificationService.createInvoiceIssuedNotification(
                new InvoiceIssuedEvent(202L, 91L, "INV-20260312-91", "새봄농산", LocalDateTime.now()));

        assertThat(statementNotification.getTargetType()).isEqualTo(NotificationTargetType.STATEMENT);
        assertThat(invoiceNotification.getTargetType()).isEqualTo(NotificationTargetType.INVOICE);
    }

    @Test
    @DisplayName("계정 활성화 알림은 ACCOUNT targetType으로 즉시 저장된다")
    void createAccountActivatedNotification() {
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 301L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(301L), eq(NotificationType.ACCOUNT_ACTIVATED), eq(NotificationTargetType.ACCOUNT), eq(301L), any(), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = documentNotificationService.createAccountActivatedNotification(
                new AccountActivatedEvent(301L, com.monsoon.seedflowplus.domain.account.entity.Role.SALES_REP, LocalDateTime.now()));

        assertThat(notification.getType()).isEqualTo(NotificationType.ACCOUNT_ACTIVATED);
        assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.ACCOUNT);
        assertThat(notification.getTargetId()).isEqualTo(301L);
    }

    @Test
    @DisplayName("상품 등록 알림은 PRODUCT targetType으로 저장된다")
    void createProductCreatedNotification() {
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 401L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(401L), eq(NotificationType.PRODUCT_CREATED), eq(NotificationTargetType.PRODUCT), eq(91L), any(), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = documentNotificationService.createProductCreatedNotification(
                new ProductCreatedEvent(401L, 91L, "VEG-26-01", "신품종 상추", LocalDateTime.now()));

        assertThat(notification.getType()).isEqualTo(NotificationType.PRODUCT_CREATED);
        assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.PRODUCT);
        assertThat(notification.getTargetId()).isEqualTo(91L);
    }
}
