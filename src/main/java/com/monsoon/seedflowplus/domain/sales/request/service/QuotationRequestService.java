package com.monsoon.seedflowplus.domain.sales.request.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.dto.DealDiffField;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.notification.event.QuotationRequestCreatedEvent;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.request.dto.request.QuotationRequestCreateRequest;
import com.monsoon.seedflowplus.domain.sales.request.dto.response.QuotationRequestListResponse;
import com.monsoon.seedflowplus.domain.sales.request.dto.response.QuotationRequestResponse;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestDetail;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationRequestService {

    private final QuotationRequestRepository quotationRequestRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogWriteService dealLogWriteService;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public void createQuotationRequest(QuotationRequestCreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        log.debug("[QuotationRequestService] createQuotationRequest role={}", userDetails.getRole());

        // 1. Role 검증: CLIENT만 가능
        if (userDetails.getRole() != Role.CLIENT) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Long clientId = userDetails.getClientId();
        if (clientId == null) {
            throw new CoreException(ErrorType.CLIENT_NOT_FOUND);
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        SalesDeal deal = createDealBootstrap(client);

        // 2. Header 생성
        QuotationRequestHeader header = QuotationRequestHeader.create(client, request.requirements(), deal);

        // 3. 품목 배치 조회 처리 (N+1 방지)
        List<Long> productIds = request.items().stream()
                .map(QuotationRequestCreateRequest.ItemRequest::productId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 4. Detail 생성 및 추가
        request.items().forEach(itemRequest -> {
            Long productId = itemRequest.productId();
            Product product = null;

            if (productId != null) {
                product = productMap.get(productId);
                if (product == null) {
                    throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
                }
            }

            QuotationRequestDetail detail = new QuotationRequestDetail(
                    product,
                    itemRequest.productCategory(),
                    itemRequest.productName(),
                    itemRequest.quantity(),
                    itemRequest.unit());
            header.addItem(detail);
        });

        // 5. 저장
        quotationRequestRepository.save(header);

        // 6. requestCode 업데이트: RFQ-YYYYMMDD-ID
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String requestCode = "RFQ-" + datePart + "-" + header.getId();
        header.updateRequestCode(requestCode);

        dealPipelineFacade.recordAndSyncWithPublicDiffs(
                deal,
                DealType.RFQ,
                header.getId(),
                requestCode,
                deal.getCurrentStage(),
                DealStage.CREATED,
                QuotationRequestStatus.PENDING.name(),
                QuotationRequestStatus.PENDING.name(),
                ActionType.CREATE,
                null,
                ActorType.CLIENT,
                clientId,
                null,
                List.of(
                        new DealDiffField(
                                "requirements",
                                "요구사항",
                                null,
                                request.requirements(),
                                "TEXT"),
                        new DealDiffField(
                                "itemCount",
                                "요청 품목 수",
                                null,
                                request.items().size(),
                                "COUNT")));

        resolveNotificationRecipientUserId(client)
                .ifPresent(userId -> notificationEventPublisher.publishAfterCommit(new QuotationRequestCreatedEvent(
                        userId,
                        header.getId(),
                        requestCode,
                        client.getClientName(),
                        now
                )));
    }

    public QuotationRequestResponse getQuotationRequest(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationRequestHeader header = quotationRequestRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 삭제된 건은 조회 불가 (직접 URL 접근 방지)
        if (header.getStatus() == QuotationRequestStatus.DELETED) {
            throw new CoreException(ErrorType.QUOTATION_NOT_FOUND);
        }

        // 권한 체크:
        // 1. ADMIN: 통과
        if (userDetails.getRole() == Role.ADMIN) {
            return QuotationRequestResponse.from(header);
        }

        // 2. CLIENT: 본인 것만 가능
        if (userDetails.getRole() == Role.CLIENT) {
            Long clientId = userDetails.getClientId();
            if (clientId == null || !header.getClient().getId().equals(clientId)) {
                throw new CoreException(ErrorType.QUOTATION_NOT_FOUND);
            }
            return QuotationRequestResponse.from(header);
        }

        // 3. SALES_REP: 담당 거래처 것만 가능
        if (userDetails.getRole() == Role.SALES_REP) {
            Long employeeId = userDetails.getEmployeeId();
            if (employeeId == null) {
                throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
            }

            Client client = header.getClient();
            boolean isManager = client.getManagerEmployee() != null
                    && client.getManagerEmployee().getId().equals(employeeId);
            boolean isDealOwner = header.getDeal() != null
                    && header.getDeal().getOwnerEmp() != null
                    && header.getDeal().getOwnerEmp().getId().equals(employeeId);

            if (!isManager && !isDealOwner) {
                throw new CoreException(ErrorType.QUOTATION_NOT_FOUND);
            }
            return QuotationRequestResponse.from(header);
        }

        throw new CoreException(ErrorType.QUOTATION_NOT_FOUND);
    }

    public List<QuotationRequestListResponse> getPendingQuotationRequests() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        List<QuotationRequestHeader> requests;

        if (userDetails.getRole() == Role.SALES_REP) {
            Long employeeId = userDetails.getEmployeeId();
            if (employeeId == null) {
                throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
            }

            requests = quotationRequestRepository.findByStatusAndClientManagerEmployeeId(QuotationRequestStatus.PENDING,
                    employeeId);
        } else {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        return requests.stream()
                .map(QuotationRequestListResponse::from)
                .toList();
    }

    /**
     * 반려 또는 만료된 견적서만 존재하는 견적요청서 목록을 조회합니다.
     * (재작성 대상)
     */
    public List<QuotationRequestListResponse> getRejectedQuotationRequests() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        if (userDetails.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Long employeeId = userDetails.getEmployeeId();
        if (employeeId == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }

        List<QuotationStatus> rejectedStatuses = List.of(
                QuotationStatus.REJECTED_ADMIN,
                QuotationStatus.REJECTED_CLIENT,
                QuotationStatus.EXPIRED
        );

        List<QuotationRequestHeader> requests = quotationRequestRepository.findRejectedRequests(
                QuotationRequestStatus.REVIEWING,
                employeeId,
                rejectedStatuses
        );

        return requests.stream()
                .map(QuotationRequestListResponse::from)
                .toList();
    }

    @Transactional
    public void completeAfterContractApproval(
            QuotationRequestHeader quotationRequest,
            ActorType actorType,
            Long actorId,
            LocalDateTime actionAt
    ) {
        if (quotationRequest == null) {
            throw new IllegalArgumentException("quotationRequest must not be null");
        }

        String fromStatus = quotationRequest.getStatus().name();
        String toStatus = QuotationRequestStatus.COMPLETED.name();
        dealPipelineFacade.validateTransitionOrThrow(DealType.RFQ, fromStatus, ActionType.APPROVE, toStatus);

        quotationRequest.updateStatus(QuotationRequestStatus.COMPLETED);
        dealLogWriteService.write(
                quotationRequest.getDeal(),
                DealType.RFQ,
                quotationRequest.getId(),
                quotationRequest.getRequestCode(),
                DealStage.IN_PROGRESS,
                DealStage.APPROVED,
                fromStatus,
                toStatus,
                ActionType.APPROVE,
                actionAt,
                actorType,
                actorId,
                null,
                List.of(new DealLogWriteService.DiffField("status", "문서 상태", fromStatus, toStatus, "STATUS"))
        );
    }

    @Transactional
    public void deleteQuotationRequest(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationRequestHeader header = quotationRequestRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 1. 권한 체크: 오직 본인(Client)인 경우만 삭제 가능
        Long clientId = userDetails.getClientId();
        if (userDetails.getRole() != Role.CLIENT || clientId == null
                || !header.getClient().getId().equals(clientId)) {
            throw new CoreException(ErrorType.QUOTATION_NOT_FOUND);
        }

        // 2. 상태 체크: PENDING 상태인 경우만 삭제 가능
        if (header.getStatus() != QuotationRequestStatus.PENDING) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        SalesDeal deal = header.getDeal();
        if (deal == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }

        String fromStatus = header.getStatus().name();
        LocalDateTime actionAt = LocalDateTime.now();
        header.delete();

        dealLogWriteService.write(
                deal,
                DealType.RFQ,
                header.getId(),
                header.getRequestCode(),
                deal.getCurrentStage(),
                DealStage.CANCELED,
                fromStatus,
                QuotationRequestStatus.DELETED.name(),
                ActionType.CANCEL,
                actionAt,
                ActorType.CLIENT,
                clientId,
                null,
                List.of(new DealLogWriteService.DiffField(
                        "status",
                        "문서 상태",
                        fromStatus,
                        QuotationRequestStatus.DELETED.name(),
                        "STATUS"))
        );
        syncDealSnapshot(
                deal,
                DealStage.CANCELED,
                QuotationRequestStatus.DELETED.name(),
                DealType.RFQ,
                header.getId(),
                header.getRequestCode(),
                actionAt
        );

        closeDealIfOpen(deal, actionAt);
    }

    private SalesDeal createDealBootstrap(Client client) {
        Employee ownerEmp = client.getManagerEmployee();
        if (ownerEmp == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }
        SalesDeal newDeal = SalesDeal.builder()
                .client(client)
                .ownerEmp(ownerEmp)
                .currentStage(DealStage.CREATED)
                .currentStatus(QuotationRequestStatus.PENDING.name())
                .latestDocType(DealType.RFQ)
                .latestRefId(0L)
                .latestTargetCode(null)
                .lastActivityAt(LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
        return salesDealRepository.save(newDeal);
    }

    private void closeDealIfOpen(SalesDeal deal, LocalDateTime actionAt) {
        if (deal == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }
        deal.close(actionAt);
    }

    private void syncDealSnapshot(
            SalesDeal deal,
            DealStage stage,
            String status,
            DealType dealType,
            Long refId,
            String targetCode,
            LocalDateTime actionAt
    ) {
        deal.updateSnapshot(stage, status, dealType, refId, targetCode, actionAt);
    }

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return userDetails;
    }

    private java.util.Optional<Long> resolveNotificationRecipientUserId(Client client) {
        if (client.getManagerEmployee() == null || client.getManagerEmployee().getId() == null) {
            return java.util.Optional.empty();
        }
        return userRepository.findByEmployeeId(client.getManagerEmployee().getId())
                .map(user -> user.getId());
    }
}
