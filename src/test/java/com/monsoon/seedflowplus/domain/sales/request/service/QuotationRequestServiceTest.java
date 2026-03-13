package com.monsoon.seedflowplus.domain.sales.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.notification.event.QuotationRequestCreatedEvent;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.request.dto.request.QuotationRequestCreateRequest;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuotationRequestServiceTest {

    @Mock
    private QuotationRequestRepository quotationRequestRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesDealRepository salesDealRepository;

    @Mock
    private DealPipelineFacade dealPipelineFacade;

    @Mock
    private DealLogWriteService dealLogWriteService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private QuotationRequestService quotationRequestService;

    @BeforeEach
    void setUp() {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        when(principal.getClientId()).thenReturn(7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("견적요청서 생성 시 담당 영업사원 1명에게 afterCommit 알림 이벤트를 발행한다")
    void createQuotationRequestPublishesNotificationEvent() {
        Employee manager = org.mockito.Mockito.mock(Employee.class);
        when(manager.getId()).thenReturn(12L);
        Client client = Client.builder()
                .clientCode("C-7")
                .clientName("새봄농산")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .managerEmployee(manager)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(client, "id", 7L);

        User salesUser = org.mockito.Mockito.mock(User.class);
        when(salesUser.getId()).thenReturn(1000L);

        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(salesDealRepository.save(any(SalesDeal.class))).thenAnswer(invocation -> {
            SalesDeal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 900L);
            return saved;
        });
        when(quotationRequestRepository.save(any(QuotationRequestHeader.class))).thenAnswer(invocation -> {
            QuotationRequestHeader header = invocation.getArgument(0);
            ReflectionTestUtils.setField(header, "id", 31L);
            return header;
        });
        when(userRepository.findByEmployeeId(12L)).thenReturn(Optional.of(salesUser));

        quotationRequestService.createQuotationRequest(new QuotationRequestCreateRequest(
                "봄 시즌 물량 요청",
                List.of(new QuotationRequestCreateRequest.ItemRequest(null, "채소", "상추", 10, "BOX"))
        ));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(QuotationRequestCreatedEvent.class);
        QuotationRequestCreatedEvent event = (QuotationRequestCreatedEvent) eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(1000L);
        assertThat(event.quotationRequestId()).isEqualTo(31L);
        assertThat(event.requestCode()).startsWith("RFQ-");
        assertThat(event.clientName()).isEqualTo("새봄농산");
    }

    @Test
    @DisplayName("견적요청서 생성은 open deal 재사용 없이 매번 새 deal을 만든다")
    void createQuotationRequestAlwaysCreatesNewDeal() {
        Employee manager = org.mockito.Mockito.mock(Employee.class);
        when(manager.getId()).thenReturn(12L);
        Client client = Client.builder()
                .clientCode("C-7")
                .clientName("새봄농산")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .managerEmployee(manager)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(client, "id", 7L);

        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(salesDealRepository.save(any(SalesDeal.class))).thenAnswer(invocation -> {
            SalesDeal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", System.nanoTime());
            return saved;
        });
        when(quotationRequestRepository.save(any(QuotationRequestHeader.class))).thenAnswer(invocation -> {
            QuotationRequestHeader header = invocation.getArgument(0);
            ReflectionTestUtils.setField(header, "id", System.nanoTime());
            return header;
        });

        QuotationRequestCreateRequest request = new QuotationRequestCreateRequest(
                "봄 시즌 물량 요청",
                List.of(new QuotationRequestCreateRequest.ItemRequest(null, "채소", "상추", 10, "BOX"))
        );

        quotationRequestService.createQuotationRequest(request);
        quotationRequestService.createQuotationRequest(request);

        verify(salesDealRepository, times(2)).save(any(SalesDeal.class));
        verify(salesDealRepository, never()).findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("견적요청서 삭제 시 삭제 로그를 남기고 deal을 닫는다")
    void deleteQuotationRequestWritesDeleteLogAndClosesDeal() {
        Employee manager = org.mockito.Mockito.mock(Employee.class);
        Client client = Client.builder()
                .clientCode("C-7")
                .clientName("새봄농산")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .managerEmployee(manager)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(client, "id", 7L);

        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(manager)
                .currentStage(DealStage.CREATED)
                .currentStatus("PENDING")
                .latestDocType(DealType.RFQ)
                .latestRefId(1L)
                .latestTargetCode("RFQ-1")
                .lastActivityAt(LocalDateTime.now())
                .build();
        QuotationRequestHeader header = QuotationRequestHeader.create(client, "req", deal);
        ReflectionTestUtils.setField(header, "id", 31L);
        header.updateRequestCode("RFQ-20260313-31");

        when(quotationRequestRepository.findById(31L)).thenReturn(Optional.of(header));

        quotationRequestService.deleteQuotationRequest(31L);

        assertThat(header.getStatus().name()).isEqualTo("DELETED");
        assertThat(deal.getClosedAt()).isNotNull();
        verify(dealLogWriteService).write(
                any(SalesDeal.class),
                eq(DealType.RFQ),
                eq(31L),
                eq("RFQ-20260313-31"),
                any(),
                eq(DealStage.CANCELED),
                eq("PENDING"),
                eq("DELETED"),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActionType.CANCEL),
                any(),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActorType.CLIENT),
                eq(7L),
                eq(null),
                any(List.class)
        );
    }
}
