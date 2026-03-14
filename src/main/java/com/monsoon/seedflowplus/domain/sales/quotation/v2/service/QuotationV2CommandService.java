package com.monsoon.seedflowplus.domain.sales.quotation.v2.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.approval.service.ApprovalCancellationService;
import com.monsoon.seedflowplus.domain.approval.service.ApprovalSubmissionService;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDocumentCommandResultDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.RevisionInfoDto;
import com.monsoon.seedflowplus.domain.deal.v2.service.DealV2SnapshotSyncService;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationDetail;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.v2.dto.request.QuotationV2CreateRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.v2.dto.request.QuotationV2ReviseRequest;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationV2CommandService {

    private final QuotationRepository quotationRepository;
    private final QuotationRequestRepository quotationRequestRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealV2SnapshotSyncService dealV2SnapshotSyncService;
    private final ApprovalCancellationService approvalCancellationService;
    private final ApprovalSubmissionService approvalSubmissionService;

    @Transactional
    public DealDocumentCommandResultDto createQuotation(QuotationV2CreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        Employee author = requireSalesRep(userDetails);
        Client client = loadManagedClient(request.clientId(), author.getId());

        QuotationRequestHeader quotationRequest = loadQuotationRequest(request.requestId());
        validateRequestDealClient(request.dealId(), quotationRequest, client);

        SalesDeal deal = resolveDealForCreate(request.dealId(), quotationRequest, client, author);
        QuotationHeader quotation = createQuotationDocument(
                quotationRequest,
                client,
                deal,
                author,
                request.items().stream().map(this::toItemDraft).toList(),
                request.memo(),
                null
        );

        if (quotationRequest != null) {
            quotationRequest.updateStatus(QuotationRequestStatus.REVIEWING);
        }

        return finalizeCreation(quotation, deal, userDetails, null, request.items().size());
    }

    @Transactional
    public DealDocumentCommandResultDto reviseQuotation(Long quotationId, QuotationV2ReviseRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        Employee author = requireSalesRep(userDetails);

        QuotationHeader source = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));
        validateQuotationAuthorAccess(source, author.getId());
        validateRevisionSource(source.getStatus());

        QuotationHeader revised = createQuotationDocument(
                source.getQuotationRequest(),
                source.getClient(),
                source.getDeal(),
                author,
                request.items().stream().map(this::toItemDraft).toList(),
                request.memo(),
                buildRevisionSeed(source)
        );

        if (source.getQuotationRequest() != null && source.getQuotationRequest().getStatus() != QuotationRequestStatus.DELETED) {
            source.getQuotationRequest().updateStatus(QuotationRequestStatus.REVIEWING);
        }

        return finalizeCreation(revised, source.getDeal(), userDetails, revisedRevisionInfo(revised), request.items().size());
    }

    @Transactional
    public DealDocumentCommandResultDto cancelQuotation(Long quotationId) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        Employee author = requireSalesRep(userDetails);

        QuotationHeader quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));
        validateQuotationAuthorAccess(quotation, author.getId());

        if (quotation.getStatus() != QuotationStatus.WAITING_ADMIN) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS, "v2 취소는 관리자 승인 이전 견적서만 허용합니다.");
        }

        String fromStatus = quotation.getStatus().name();
        quotation.updateStatus(QuotationStatus.DELETED);
        approvalCancellationService.cancelPendingRequest(DealType.QUO, quotation.getId());

        if (quotation.getQuotationRequest() != null && quotation.getQuotationRequest().getStatus() == QuotationRequestStatus.REVIEWING) {
            quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.PENDING);
        }

        dealPipelineFacade.recordAndSync(
                quotation.getDeal(),
                DealType.QUO,
                quotation.getId(),
                quotation.getQuotationCode(),
                DealStage.PENDING_ADMIN,
                DealStage.CANCELED,
                fromStatus,
                QuotationStatus.DELETED.name(),
                ActionType.CANCEL,
                null,
                ActorType.SALES_REP,
                userDetails.getEmployeeId(),
                "v2 cancel",
                List.of(new DealLogWriteService.DiffField("status", "문서 상태", fromStatus, QuotationStatus.DELETED.name(), "STATUS"))
        );
        dealV2SnapshotSyncService.recalculate(quotation.getDeal());

        return DealDocumentCommandResultDto.builder()
                .dealId(quotation.getDeal() != null ? quotation.getDeal().getId() : null)
                .documentType(DealType.QUO)
                .documentId(quotation.getId())
                .documentCode(quotation.getQuotationCode())
                .revisionInfo(null)
                .build();
    }

    private DealDocumentCommandResultDto finalizeCreation(
            QuotationHeader quotation,
            SalesDeal deal,
            CustomUserDetails userDetails,
            RevisionInfoDto revisionInfo,
            int itemCount
    ) {
        quotationRepository.save(quotation);
        String finalCode = "QUO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + quotation.getId();
        quotation.updateQuotationCode(finalCode);

        dealPipelineFacade.recordAndSync(
                deal,
                DealType.QUO,
                quotation.getId(),
                finalCode,
                deal.getCurrentStage(),
                DealStage.PENDING_ADMIN,
                quotation.getStatus().name(),
                quotation.getStatus().name(),
                ActionType.CREATE,
                null,
                ActorType.SALES_REP,
                userDetails.getEmployeeId(),
                null,
                List.of(
                        new DealLogWriteService.DiffField("totalAmount", "총액", null, quotation.getTotalAmount(), "MONEY"),
                        new DealLogWriteService.DiffField("itemCount", "견적 품목 수", null, itemCount, "COUNT")
                )
        );

        approvalSubmissionService.submitFromDocumentCreation(DealType.QUO, quotation.getId(), finalCode, userDetails);

        return DealDocumentCommandResultDto.builder()
                .dealId(deal.getId())
                .documentType(DealType.QUO)
                .documentId(quotation.getId())
                .documentCode(finalCode)
                .revisionInfo(revisionInfo)
                .build();
    }

    private QuotationHeader createQuotationDocument(
            QuotationRequestHeader quotationRequest,
            Client client,
            SalesDeal deal,
            Employee author,
            List<QuotationItemDraft> itemDrafts,
            String memo,
            RevisionSeed revisionSeed
    ) {
        BigDecimal totalAmount = itemDrafts.stream()
                .map(QuotationItemDraft::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        QuotationHeader quotation = QuotationHeader.create(
                quotationRequest,
                temporaryCode(),
                client,
                deal,
                author,
                totalAmount,
                memo
        );

        for (QuotationItemDraft itemDraft : itemDrafts) {
            QuotationDetail detail = new QuotationDetail(
                    itemDraft.product(),
                    itemDraft.productCategory(),
                    itemDraft.productName(),
                    itemDraft.quantity(),
                    itemDraft.unit(),
                    itemDraft.unitPrice(),
                    itemDraft.amount()
            );
            quotation.addItem(detail);
        }

        if (revisionSeed != null) {
            quotation.assignRevisionLineage(
                    revisionSeed.sourceDocumentId(),
                    revisionSeed.revisionGroupKey(),
                    revisionSeed.revisionNo()
            );
        }

        return quotation;
    }

    private SalesDeal resolveDealForCreate(Long dealId, QuotationRequestHeader quotationRequest, Client client, Employee author) {
        if (quotationRequest != null) {
            if (quotationRequest.getDeal() == null) {
                throw new CoreException(ErrorType.DEAL_NOT_FOUND);
            }
            return quotationRequest.getDeal();
        }

        if (dealId != null) {
            SalesDeal deal = salesDealRepository.findById(dealId)
                    .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));
            validateDealOwnership(deal, client.getId(), author.getId());
            return deal;
        }

        return createDealBootstrap(client, author);
    }

    private QuotationRequestHeader loadQuotationRequest(Long requestId) {
        if (requestId == null) {
            return null;
        }

        QuotationRequestHeader quotationRequest = quotationRequestRepository.findByIdWithLock(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.RFQ_NOT_FOUND));

        if (quotationRequest.getStatus() != QuotationRequestStatus.PENDING
                && quotationRequest.getStatus() != QuotationRequestStatus.REVIEWING) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        return quotationRequest;
    }

    private void validateRequestDealClient(Long dealId, QuotationRequestHeader quotationRequest, Client client) {
        if (quotationRequest == null) {
            return;
        }
        if (!quotationRequest.getClient().getId().equals(client.getId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        if (dealId != null && quotationRequest.getDeal() != null && !quotationRequest.getDeal().getId().equals(dealId)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "상위 문서가 있으면 dealId를 임의로 변경할 수 없습니다.");
        }
    }

    private void validateDealOwnership(SalesDeal deal, Long clientId, Long employeeId) {
        if (deal.getClient() == null || !deal.getClient().getId().equals(clientId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        if (deal.getOwnerEmp() == null || !deal.getOwnerEmp().getId().equals(employeeId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    private void validateQuotationAuthorAccess(QuotationHeader quotation, Long employeeId) {
        if (quotation.getAuthor() == null || !quotation.getAuthor().getId().equals(employeeId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    private void validateRevisionSource(QuotationStatus status) {
        if (status != QuotationStatus.REJECTED_ADMIN
                && status != QuotationStatus.REJECTED_CLIENT
                && status != QuotationStatus.EXPIRED) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS, "반려 또는 만료된 견적서만 재작성할 수 있습니다.");
        }
    }

    private RevisionSeed buildRevisionSeed(QuotationHeader source) {
        String revisionGroupKey = source.getRevisionGroupKey() != null
                ? source.getRevisionGroupKey()
                : "QUO-" + source.getId();
        int nextRevisionNo = quotationRepository.findTopByRevisionGroupKeyOrderByRevisionNoDesc(revisionGroupKey)
                .map(QuotationHeader::getRevisionNo)
                .orElse(source.getRevisionNo() != null ? source.getRevisionNo() : 0) + 1;
        return new RevisionSeed(source.getId(), revisionGroupKey, nextRevisionNo);
    }

    private RevisionInfoDto revisedRevisionInfo(QuotationHeader quotation) {
        return RevisionInfoDto.builder()
                .sourceDocumentId(quotation.getSourceDocumentId())
                .revisionGroupKey(quotation.getRevisionGroupKey())
                .revisionNo(quotation.getRevisionNo())
                .latestRevisionNo(quotation.getRevisionNo())
                .revisionStartable(false)
                .build();
    }

    private QuotationItemDraft toItemDraft(QuotationV2CreateRequest.Item itemRequest) {
        return toItemDraft(
                itemRequest.productId(),
                itemRequest.productCategory(),
                itemRequest.productName(),
                itemRequest.quantity(),
                itemRequest.unit(),
                itemRequest.unitPrice()
        );
    }

    private QuotationItemDraft toItemDraft(QuotationV2ReviseRequest.Item itemRequest) {
        return toItemDraft(
                itemRequest.productId(),
                itemRequest.productCategory(),
                itemRequest.productName(),
                itemRequest.quantity(),
                itemRequest.unit(),
                itemRequest.unitPrice()
        );
    }

    private QuotationItemDraft toItemDraft(
            Long productId,
            String productCategory,
            String productName,
            Integer quantity,
            String unit,
            BigDecimal unitPrice
    ) {
        if (productId != null && !productRepository.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        return new QuotationItemDraft(
                productId != null ? productRepository.getReferenceById(productId) : null,
                productCategory,
                productName,
                quantity,
                unit,
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity))
        );
    }

    private SalesDeal createDealBootstrap(Client client, Employee ownerEmp) {
        SalesDeal newDeal = SalesDeal.builder()
                .client(client)
                .ownerEmp(ownerEmp)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(QuotationStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.QUO)
                .latestRefId(0L)
                .latestTargetCode(null)
                .lastActivityAt(LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
        return salesDealRepository.save(newDeal);
    }

    private String temporaryCode() {
        return "TEMP-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
    }

    private Employee requireSalesRep(CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        return employeeRepository.findById(userDetails.getEmployeeId())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
    }

    private Client loadManagedClient(Long clientId, Long employeeId) {
        Client client = clientRepository.findByIdWithLock(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        if (client.getManagerEmployee() == null || !client.getManagerEmployee().getId().equals(employeeId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        return client;
    }

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return userDetails;
    }

    private record RevisionSeed(Long sourceDocumentId, String revisionGroupKey, Integer revisionNo) {
    }

    private record QuotationItemDraft(
            com.monsoon.seedflowplus.domain.product.entity.Product product,
            String productCategory,
            String productName,
            Integer quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }
}
