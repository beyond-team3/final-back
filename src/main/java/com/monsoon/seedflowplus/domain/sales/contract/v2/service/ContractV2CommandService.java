package com.monsoon.seedflowplus.domain.sales.contract.v2.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
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
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.contract.v2.dto.request.ContractV2CreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.v2.dto.request.ContractV2ReviseRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
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
public class ContractV2CommandService {

    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final ApprovalSubmissionService approvalSubmissionService;

    @Transactional
    public DealDocumentCommandResultDto createContract(ContractV2CreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        Employee author = requireSalesRep(userDetails);
        Client client = loadManagedClient(request.clientId(), author.getId());

        QuotationHeader quotation = loadQuotation(request.quotationId());
        validateCreateRequest(request.dealId(), quotation, client);
        SalesDeal deal = resolveDealForCreate(request.dealId(), quotation, client, author);

        ContractHeader contract = createContractDocument(
                quotation,
                client,
                deal,
                author,
                request.startDate(),
                request.endDate(),
                request.billingCycle(),
                request.specialTerms(),
                request.memo(),
                request.items().stream().map(this::toItemDraft).toList(),
                null
        );

        if (quotation != null) {
            quotation.updateStatus(QuotationStatus.WAITING_CONTRACT);
            if (quotation.getQuotationRequest() != null && quotation.getQuotationRequest().getStatus() != QuotationRequestStatus.DELETED) {
                quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.COMPLETED);
            }
        }

        return finalizeCreation(contract, deal, userDetails, null, request.items().size());
    }

    @Transactional
    public DealDocumentCommandResultDto reviseContract(Long contractId, ContractV2ReviseRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        Employee author = requireSalesRep(userDetails);

        ContractHeader source = contractRepository.findById(contractId)
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
        validateContractAuthorAccess(source, author.getId());
        validateRevisionSource(source.getStatus());

        ContractHeader revised = createContractDocument(
                source.getQuotation(),
                source.getClient(),
                source.getDeal(),
                author,
                request.startDate(),
                request.endDate(),
                request.billingCycle(),
                request.specialTerms(),
                request.memo(),
                request.items().stream().map(this::toItemDraft).toList(),
                buildRevisionSeed(source)
        );

        if (source.getQuotation() != null) {
            source.getQuotation().updateStatus(QuotationStatus.WAITING_CONTRACT);
        }

        return finalizeCreation(revised, source.getDeal(), userDetails, revisedRevisionInfo(revised), request.items().size());
    }

    private DealDocumentCommandResultDto finalizeCreation(
            ContractHeader contract,
            SalesDeal deal,
            CustomUserDetails userDetails,
            RevisionInfoDto revisionInfo,
            int itemCount
    ) {
        contractRepository.save(contract);
        String finalCode = "CNT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + contract.getId();
        contract.updateContractCode(finalCode);

        dealPipelineFacade.recordAndSync(
                deal,
                DealType.CNT,
                contract.getId(),
                finalCode,
                deal.getCurrentStage(),
                DealStage.PENDING_ADMIN,
                contract.getStatus().name(),
                contract.getStatus().name(),
                ActionType.CREATE,
                null,
                ActorType.SALES_REP,
                userDetails.getEmployeeId(),
                null,
                List.of(
                        new DealLogWriteService.DiffField("totalAmount", "총액", null, contract.getTotalAmount(), "MONEY"),
                        new DealLogWriteService.DiffField("billingCycle", "청구 주기", null, contract.getBillingCycle().name(), "ENUM"),
                        new DealLogWriteService.DiffField("itemCount", "계약 품목 수", null, itemCount, "COUNT")
                )
        );

        approvalSubmissionService.submitFromDocumentCreation(DealType.CNT, contract.getId(), finalCode, userDetails);

        return DealDocumentCommandResultDto.builder()
                .dealId(deal.getId())
                .documentType(DealType.CNT)
                .documentId(contract.getId())
                .documentCode(finalCode)
                .revisionInfo(revisionInfo)
                .build();
    }

    private ContractHeader createContractDocument(
            QuotationHeader quotation,
            Client client,
            SalesDeal deal,
            Employee author,
            LocalDate startDate,
            LocalDate endDate,
            com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle billingCycle,
            String specialTerms,
            String memo,
            List<ContractItemDraft> itemDrafts,
            RevisionSeed revisionSeed
    ) {
        if (startDate.isAfter(endDate)) {
            throw new CoreException(ErrorType.INVALID_CONTRACT_PERIOD);
        }

        BigDecimal totalAmount = itemDrafts.stream()
                .map(ContractItemDraft::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ContractHeader contract = ContractHeader.create(
                temporaryCode(),
                quotation,
                client,
                deal,
                author,
                totalAmount,
                startDate,
                endDate,
                billingCycle,
                specialTerms,
                memo
        );

        for (ContractItemDraft itemDraft : itemDrafts) {
            ContractDetail detail = new ContractDetail(
                    itemDraft.product(),
                    itemDraft.productName(),
                    itemDraft.productCategory(),
                    itemDraft.totalQuantity(),
                    itemDraft.unit(),
                    itemDraft.unitPrice(),
                    itemDraft.amount()
            );
            contract.addItem(detail);
        }

        if (revisionSeed != null) {
            contract.assignRevisionLineage(
                    revisionSeed.sourceDocumentId(),
                    revisionSeed.revisionGroupKey(),
                    revisionSeed.revisionNo()
            );
        }

        return contract;
    }

    private SalesDeal resolveDealForCreate(Long dealId, QuotationHeader quotation, Client client, Employee author) {
        if (quotation != null) {
            if (quotation.getDeal() == null) {
                throw new CoreException(ErrorType.DEAL_NOT_FOUND);
            }
            return quotation.getDeal();
        }

        if (dealId != null) {
            SalesDeal deal = salesDealRepository.findById(dealId)
                    .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));
            validateDealOwnership(deal, client.getId(), author.getId());
            return deal;
        }

        return createDealBootstrap(client, author);
    }

    private QuotationHeader loadQuotation(Long quotationId) {
        if (quotationId == null) {
            return null;
        }

        QuotationHeader quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED
                && quotation.getStatus() != QuotationStatus.WAITING_CONTRACT) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        return quotation;
    }

    private void validateCreateRequest(Long dealId, QuotationHeader quotation, Client client) {
        if (quotation == null) {
            return;
        }
        if (!quotation.getClient().getId().equals(client.getId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        if (dealId != null && quotation.getDeal() != null && !quotation.getDeal().getId().equals(dealId)) {
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

    private void validateContractAuthorAccess(ContractHeader contract, Long employeeId) {
        if (contract.getAuthor() == null || !contract.getAuthor().getId().equals(employeeId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    private void validateRevisionSource(ContractStatus status) {
        if (status != ContractStatus.REJECTED_ADMIN
                && status != ContractStatus.REJECTED_CLIENT
                && status != ContractStatus.EXPIRED) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS, "반려 또는 만료된 계약서만 재작성할 수 있습니다.");
        }
    }

    private RevisionSeed buildRevisionSeed(ContractHeader source) {
        String revisionGroupKey = source.getRevisionGroupKey() != null
                ? source.getRevisionGroupKey()
                : "CNT-" + source.getId();
        int nextRevisionNo = contractRepository.findTopByRevisionGroupKeyOrderByRevisionNoDesc(revisionGroupKey)
                .map(ContractHeader::getRevisionNo)
                .orElse(source.getRevisionNo() != null ? source.getRevisionNo() : 0) + 1;
        return new RevisionSeed(source.getId(), revisionGroupKey, nextRevisionNo);
    }

    private RevisionInfoDto revisedRevisionInfo(ContractHeader contract) {
        return RevisionInfoDto.builder()
                .sourceDocumentId(contract.getSourceDocumentId())
                .revisionGroupKey(contract.getRevisionGroupKey())
                .revisionNo(contract.getRevisionNo())
                .latestRevisionNo(contract.getRevisionNo())
                .revisionStartable(false)
                .build();
    }

    private ContractItemDraft toItemDraft(ContractV2CreateRequest.Item itemRequest) {
        return toItemDraft(
                itemRequest.productId(),
                itemRequest.productName(),
                itemRequest.productCategory(),
                itemRequest.totalQuantity(),
                itemRequest.unit(),
                itemRequest.unitPrice()
        );
    }

    private ContractItemDraft toItemDraft(ContractV2ReviseRequest.Item itemRequest) {
        return toItemDraft(
                itemRequest.productId(),
                itemRequest.productName(),
                itemRequest.productCategory(),
                itemRequest.totalQuantity(),
                itemRequest.unit(),
                itemRequest.unitPrice()
        );
    }

    private ContractItemDraft toItemDraft(
            Long productId,
            String productName,
            String productCategory,
            Integer totalQuantity,
            String unit,
            BigDecimal unitPrice
    ) {
        if (!productRepository.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        return new ContractItemDraft(
                productRepository.getReferenceById(productId),
                productName,
                productCategory,
                totalQuantity,
                unit,
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(totalQuantity))
        );
    }

    private SalesDeal createDealBootstrap(Client client, Employee ownerEmp) {
        SalesDeal newDeal = SalesDeal.builder()
                .client(client)
                .ownerEmp(ownerEmp)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(ContractStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.CNT)
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
        Client client = clientRepository.findById(clientId)
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

    private record ContractItemDraft(
            com.monsoon.seedflowplus.domain.product.entity.Product product,
            String productName,
            String productCategory,
            Integer totalQuantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }
}
