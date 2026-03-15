package com.monsoon.seedflowplus.domain.sales.contract.service;

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
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.contract.dto.request.ContractCreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractPrefillResponse;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractSimpleResponse;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractResponse;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.approval.dto.response.ReasonDto;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalDecisionRepository;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractListResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationListResponse;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final QuotationRequestRepository quotationRequestRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogWriteService dealLogWriteService;
    private final DealLogQueryService dealLogQueryService;
    private final ApprovalCancellationService approvalCancellationService;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final ApprovalSubmissionService approvalSubmissionService;

    public ContractPrefillResponse getPrefillData(Long quotationId, Long contractId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        if (userDetails.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        if ((quotationId == null && contractId == null) || (quotationId != null && contractId != null)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "quotationId와 contractId 중 하나만 전달해야 합니다.");
        }

        if (contractId != null) {
            ContractHeader contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

            // 본인이 작성한 반려된 계약서만 복사 가능
            if (contract.getStatus() != ContractStatus.REJECTED_ADMIN
                    && contract.getStatus() != ContractStatus.REJECTED_CLIENT) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS, "반려된 계약서만 복사할 수 있습니다.");
            }

            // 작성자 본인만 접근 가능 (보안 강화)
            if (contract.getAuthor() == null || !contract.getAuthor().getId().equals(userDetails.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }

            return new ContractPrefillResponse(
                    contract.getQuotation() != null ? contract.getQuotation().getId() : null,
                    contract.getQuotation() != null ? contract.getQuotation().getQuotationCode() : null,
                    contract.getClient().getId(),
                    contract.getClient().getClientName(),
                    contract.getClient().getManagerName(),
                    contract.getTotalAmount(),
                    contract.getDeal() != null ? contract.getDeal().getId() : null,
                    contract.getStartDate(),
                    contract.getEndDate(),
                    contract.getBillingCycle() != null ? contract.getBillingCycle().name() : null,
                    contract.getSpecialTerms(),
                    contract.getMemo(),
                    contract.getItems().stream()
                            .map(item -> new ContractPrefillResponse.Item(
                                    item.getProduct() != null ? item.getProduct().getId() : null,
                                    item.getProductName(),
                                    item.getProductCategory(),
                                    item.getTotalQuantity(),
                                    item.getUnit(),
                                    item.getUnitPrice(),
                                    item.getAmount()))
                            .toList());
        }

        if (quotationId != null) {
            QuotationHeader quotation = quotationRepository.findById(quotationId)
                    .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

            // 상태 검증: 최종 승인된 견적서만 가능
            if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
            }

            // 권한 검증: 본인이 작성한 영업사원만 가능
            validateQuotationAuthorAccess(quotation, userDetails);

            return new ContractPrefillResponse(
                    quotation.getId(),
                    quotation.getQuotationCode(),
                    quotation.getClient().getId(),
                    quotation.getClient().getClientName(),
                    quotation.getClient().getManagerName(),
                    quotation.getTotalAmount(),
                    quotation.getDeal() != null ? quotation.getDeal().getId() : null,
                    null, null, null, null, null, // 신규 작성 시 날짜/조건 등은 빈값
                    quotation.getItems().stream()
                            .map(item -> new ContractPrefillResponse.Item(
                                    item.getProduct() != null ? item.getProduct().getId() : null,
                                    item.getProductName(),
                                    item.getProductCategory(),
                                    item.getQuantity(),
                                    item.getUnit(),
                                    item.getUnitPrice(),
                                    item.getAmount()))
                            .toList());
        }

        throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
    }

    public ContractResponse getContractDetail(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        ContractHeader contract = contractRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

        // 논리 삭제된 건 조회 불가
        if (contract.getStatus() == ContractStatus.DELETED) {
            throw new CoreException(ErrorType.CONTRACT_NOT_FOUND);
        }

        // 권한 체크
        validateAccess(contract, userDetails);

        List<ContractResponse.ItemResponse> items = contract.getItems().stream()
                .map(detail -> new ContractResponse.ItemResponse(
                        detail.getId(), // reason: detail 식별자가 있어야 주문 요청에서 정확한 contractDetailId 선택 가능
                        detail.getProduct() != null ? detail.getProduct().getId() : null,
                        detail.getProductName(),
                        detail.getProductCategory(),
                        detail.getTotalQuantity(),
                        detail.getUnit(),
                        detail.getUnitPrice(),
                        detail.getAmount()))
                .toList();

        return new ContractResponse(
                contract.getId(),
                contract.getContractCode(),
                contract.getQuotation() != null ? contract.getQuotation().getId() : null,
                contract.getClient().getId(),
                contract.getClient().getClientName(),
                contract.getAuthor() != null ? contract.getAuthor().getId() : null,
                contract.getAuthor() != null ? contract.getAuthor().getEmployeeName() : null,
                contract.getStatus(),
                contract.getTotalAmount(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getBillingCycle(),
                contract.getSpecialTerms(),
                contract.getMemo(),
                contract.getCreatedAt(),
                items,
                dealLogQueryService.getRecentDocumentLogs(
                        contract.getDeal() != null ? contract.getDeal().getId() : null,
                        DealType.CNT,
                        contract.getId()));
    }

    /**
     * 특정 거래처의 모든 계약 목록 조회 (이력 관리 및 일반 조회용)
     */
    public List<ContractSimpleResponse> getContractsByClient(Long clientId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 권한 체크: 해당 거래처의 데이터에 접근할 권한이 있는지 확인
        validateClientAccess(client, userDetails);

        return contractRepository.findByClientOrderByEndDateAsc(client).stream()
                .filter(c -> c.getStatus() != ContractStatus.DELETED) // 공통: 삭제 제외
                .filter(c -> {
                    // 거래처인 경우 관리자 승인 전 단계(WAITING_ADMIN, REJECTED_ADMIN)는 노출하지 않음
                    if (userDetails.getRole() == Role.CLIENT) {
                        return c.getStatus() != ContractStatus.WAITING_ADMIN &&
                                c.getStatus() != ContractStatus.REJECTED_ADMIN;
                    }
                    return true;
                })
                .map(ContractSimpleResponse::from)
                .toList();
    }

    /**
     * 특정 거래처의 활성 계약 목록 조회 (주문서 작성 등 실무용)
     * ACTIVE_CONTRACT 상태만 반환
     */
    public List<ContractSimpleResponse> getActiveContractsByClient(Long clientId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        validateClientAccess(client, userDetails);

        return contractRepository.findActiveContractsByClient(client, LocalDate.now(),
                ContractStatus.ACTIVE_CONTRACT, ContractStatus.COMPLETED)
                .stream()
                .map(ContractSimpleResponse::from)
                .toList();
    }

    private void validateClientAccess(Client client, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.SALES_REP) {
            // 해당 거래처의 담당 영업사원인지 확인
            if (client.getManagerEmployee() == null
                    || !client.getManagerEmployee().getId().equals(user.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        if (user.getRole() == Role.CLIENT) {
            // 본인 거래처인지 확인
            if (!client.getId().equals(user.getClientId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    /**
     * 계약 상태 자동 동기화
     * 1. COMPLETED(완료) -> 시작일(startDate) 도래 시 ACTIVE_CONTRACT(진행중)로 변경
     * 2. ACTIVE_CONTRACT(진행중) -> 종료일(endDate) 경과 시 EXPIRED(만료)로 변경
     */
    @Transactional
    public void syncContractStatuses() {
        LocalDate today = LocalDate.now();
        // 1. 활성화 대상 처리 (완료 상태인데 시작일이 오늘이거나 이전인 경우)
        int activatedCount = contractRepository.updateStatusForActivation(
                ContractStatus.COMPLETED, ContractStatus.ACTIVE_CONTRACT, today);

        List<ContractHeader> expiringContracts = contractRepository
                .findByStatusAndEndDateLessThan(ContractStatus.ACTIVE_CONTRACT, today);
        Map<Long, List<ExpiringContractContext>> expiringContextByDealId = expiringContracts.stream()
                .filter(contract -> contract.getDeal() != null && contract.getDeal().getId() != null)
                .collect(Collectors.groupingBy(
                        contract -> contract.getDeal().getId(),
                        java.util.LinkedHashMap::new,
                        Collectors.mapping(
                                contract -> new ExpiringContractContext(
                                        contract.getId(),
                                        contract.getContractCode(),
                                        contract.getDeal().getId()),
                                Collectors.toList())));

        // 2. 만료 대상 처리 (진행중 상태인데 종료일이 오늘보다 이전인 경우)
        int expiredCount = contractRepository.updateStatusForExpiration(
                ContractStatus.ACTIVE_CONTRACT, ContractStatus.EXPIRED, today);

        if (activatedCount > 0 || expiredCount > 0) {
            log.info("[ContractService] 상태 동기화 완료: 활성화 {}건, 만료 {}건", activatedCount, expiredCount);
        }

        expireDeals(expiringContextByDealId, LocalDateTime.now());
    }

    private void validateAccess(ContractHeader contract, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.SALES_REP) {
            boolean isAuthor = contract.getAuthor() != null
                    && contract.getAuthor().getId().equals(user.getEmployeeId());
            boolean isManager = contract.getClient().getManagerEmployee() != null
                    && contract.getClient().getManagerEmployee().getId().equals(user.getEmployeeId());

            if (!isAuthor && !isManager) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        if (user.getRole() == Role.CLIENT) {
            if (!contract.getClient().getId().equals(user.getClientId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    @Transactional
    public void deleteContract(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        ContractHeader contract = contractRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

        // 이미 삭제된 경우 처리
        if (contract.getStatus() == ContractStatus.DELETED) {
            throw new CoreException(ErrorType.CONTRACT_NOT_FOUND);
        }

        // 권한 체크 (작성자 또는 관리자만 삭제 가능하도록 설정)
        if (userDetails.getRole() != Role.ADMIN) {
            if (contract.getAuthor() == null || !contract.getAuthor().getId().equals(userDetails.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
        }

        if (contract.getStatus() != ContractStatus.WAITING_ADMIN) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        String fromStatus = contract.getStatus().name();
        DealStage fromStage = mapContractStage(contract.getStatus());
        contract.delete();
        approvalCancellationService.cancelPendingRequest(DealType.CNT, contract.getId());

        // 연관된 견적서 및 견적요청서 상태 복구 (가드 추가: 터미널 상태 보호)
        QuotationHeader quotation = contract.getQuotation();
        DealStage toStage = DealStage.CANCELED;
        String toStatus = ContractStatus.DELETED.name();
        if (quotation != null) {
            // 견적서 상태: WAITING_CONTRACT 일 때만 FINAL_APPROVED로 복구
            if (quotation.getStatus() == QuotationStatus.WAITING_CONTRACT) {
                quotation.updateStatus(QuotationStatus.FINAL_APPROVED);
                toStage = mapQuotationStage(quotation.getStatus());
                toStatus = quotation.getStatus().name();
            }

            // 견적요청서 상태: 완료(COMPLETED) 상태일 때만 검토 중(REVIEWING)으로 복구
            if (quotation.getQuotationRequest() != null &&
                    quotation.getQuotationRequest().getStatus() == QuotationRequestStatus.COMPLETED) {
                quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.REVIEWING);
            }
        }

        if (contract.getDeal() != null) {
            dealLogWriteService.write(
                    contract.getDeal(),
                    DealType.CNT,
                    contract.getId(),
                    contract.getContractCode(),
                    fromStage,
                    toStage,
                    fromStatus,
                    toStatus,
                    ActionType.CANCEL,
                    LocalDateTime.now(),
                    resolveActorType(userDetails),
                    resolveActorId(userDetails),
                    null,
                    List.of(new DealLogWriteService.DiffField(
                            "status",
                            "문서 상태",
                            fromStatus,
                            ContractStatus.DELETED.name(),
                            "STATUS")));
        }

        restoreDealSnapshotAfterContractDelete(contract);
    }

    @Transactional
    public void createContract(@Valid ContractCreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        QuotationHeader quotation = null;
        Client client;
        Employee author;
        SalesDeal deal;

        if (request.quotationId() != null) {
            // 1-1. 견적서 기반 작성
            quotation = quotationRepository.findById(request.quotationId())
                    .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

            if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED &&
                    quotation.getStatus() != QuotationStatus.WAITING_CONTRACT) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
            }

            // WAITING_CONTRACT인 경우, 이미 진행 중인 계약이 있는지 확인 (중복 작성 방지)
            if (quotation.getStatus() == QuotationStatus.WAITING_CONTRACT) {
                boolean hasActiveContract = contractRepository.existsByQuotationIdAndStatusIn(
                        request.quotationId(),
                        List.of(ContractStatus.WAITING_ADMIN, ContractStatus.WAITING_CLIENT));
                if (hasActiveContract) {
                    throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
                }
            }

            // 요청한 거래처와 견적서의 거래처 일치 확인
            if (!quotation.getClient().getId().equals(request.clientId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }

            validateQuotationAuthorAccess(quotation, userDetails);

            client = quotation.getClient();
            author = quotation.getAuthor();
            deal = quotation.getDeal() != null
                    ? quotation.getDeal()
                    : createDealBootstrap(client, author);
        } else {
            // 1-2. 신규 작성 (견적서 없음)
            client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

            // 신규 작성은 영업사원만 가능
            if (userDetails.getRole() != Role.SALES_REP) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }

            author = employeeRepository.findById(userDetails.getEmployeeId())
                    .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
            deal = createDealBootstrap(client, author);
        }

        // 2. 계약 기간 검증
        if (request.startDate().isAfter(request.endDate())) {
            throw new CoreException(ErrorType.INVALID_CONTRACT_PERIOD);
        }

        // 3. 계약 총액 및 품목 일치 검증
        BigDecimal totalAmount = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.totalQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (quotation != null) {
            // 품목 개수 비교
            if (request.items().size() != quotation.getItems().size()) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_DATA, "품목 개수가 견적서와 일치하지 않습니다.");
            }

            // 각 품목 상세 비교 (순서 비의존 비교를 위해 정렬된 키 활용)
            List<String> requestKeys = request.items().stream()
                    .map(i -> (i.productId() == null ? "NULL" : i.productId()) + "|" +
                            i.totalQuantity() + "|" + i.unitPrice().stripTrailingZeros().toPlainString())
                    .sorted()
                    .toList();

            List<String> quotationKeys = quotation.getItems().stream()
                    .map(i -> (i.getProduct() == null ? "NULL" : i.getProduct().getId()) + "|" +
                            i.getQuantity() + "|" + i.getUnitPrice().stripTrailingZeros().toPlainString())
                    .sorted()
                    .toList();

            if (!requestKeys.equals(quotationKeys)) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_DATA, "품목 정보(상품, 수량, 단가)가 견적서와 일치하지 않습니다.");
            }

            // 총액 검증
            if (totalAmount.compareTo(quotation.getTotalAmount()) != 0) {
                throw new CoreException(ErrorType.INVALID_TOTAL_AMOUNT);
            }
        }

        // 4. 계약서 헤더 생성 (임시 코드 사용 - 충돌 방지를 위해 UUID 사용)
        String tempCode = "TEMP-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();

        ContractHeader contract = ContractHeader.create(
                tempCode,
                quotation,
                client,
                deal,
                author,
                totalAmount,
                request.startDate(),
                request.endDate(),
                request.billingCycle(),
                request.specialTerms(),
                request.memo());

        // 5. 계약 상세 품목 생성 및 추가
        request.items().forEach(itemRequest -> {
            if (itemRequest.productId() != null && !productRepository.existsById(itemRequest.productId())) {
                throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
            }

            ContractDetail detail = new ContractDetail(
                    itemRequest.productId() != null ? productRepository.getReferenceById(itemRequest.productId())
                            : null,
                    itemRequest.productName(),
                    itemRequest.productCategory(),
                    itemRequest.totalQuantity(),
                    itemRequest.unit(),
                    itemRequest.unitPrice(),
                    itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.totalQuantity())));
            contract.addItem(detail);
        });

        // 6. 저장 및 식별자 기반 코드 업데이트
        contractRepository.save(contract);
        String finalCode = "CNT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + contract.getId();
        contract.updateContractCode(finalCode);

        dealPipelineFacade.recordAndSync(
                deal,
                DealType.CNT,
                contract.getId(),
                finalCode,
                deal.getCurrentStage(),
                DealStage.PENDING_ADMIN,
                ContractStatus.WAITING_ADMIN.name(),
                ContractStatus.WAITING_ADMIN.name(),
                ActionType.CREATE,
                null,
                ActorType.SALES_REP,
                userDetails.getEmployeeId(),
                null,
                List.of(
                        new DealLogWriteService.DiffField("totalAmount", "총액", null, totalAmount, "MONEY"),
                        new DealLogWriteService.DiffField("billingCycle", "청구 주기", null, request.billingCycle().name(),
                                "ENUM"),
                        new DealLogWriteService.DiffField("itemCount", "계약 품목 수", null, request.items().size(),
                                "COUNT")));

        // 7. 문서 상태 업데이트: 견적서(WAITING_CONTRACT), 견적요청서(COMPLETED)
        if (quotation != null) {
            quotation.updateStatus(QuotationStatus.WAITING_CONTRACT);
            if (quotation.getQuotationRequest() != null &&
                    quotation.getQuotationRequest().getStatus() != QuotationRequestStatus.DELETED) {
                quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.COMPLETED);
            }
        }

        // 8. 결재 요청 제출
        log.info("[ContractService] 계약 생성 완료. 결재 요청 제출 시도 - CNT_ID: {}, Code: {}", contract.getId(), finalCode);
        try {
            approvalSubmissionService.submitFromDocumentCreation(
                    DealType.CNT,
                    contract.getId(),
                    finalCode,
                    userDetails
            );
            log.info("[ContractService] 결재 요청 제출 성공 - CNT_ID: {}", contract.getId());
        } catch (Exception e) {
            log.error("[ContractService] 결재 요청 제출 실패 - CNT_ID: {}, Error: {}", contract.getId(), e.getMessage());
            throw e;
        }
    }

    private void validateQuotationAuthorAccess(QuotationHeader quotation, CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.SALES_REP ||
                quotation.getAuthor() == null ||
                !quotation.getAuthor().getId().equals(userDetails.getEmployeeId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return userDetails;
    }

    private SalesDeal createDealBootstrap(Client client, Employee ownerEmp) {
        if (ownerEmp == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }
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

    /**
     * 계약서 재작성이 가능한 견적서 목록 조회
     */
    public List<QuotationListResponse> getRejectedQuotationsForContract() {
        CustomUserDetails user = getAuthenticatedUser();

        if (user.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        if (user.getEmployeeId() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "employeeId is null");
        }

        List<ContractStatus> rejectedStatuses = List.of(ContractStatus.REJECTED_ADMIN, ContractStatus.REJECTED_CLIENT);
        List<QuotationHeader> quotations;

        if (user.getRole() == Role.ADMIN) {
            // 관리자는 전역 조회
            quotations = quotationRepository.findAllQuotationsReadyForContractRewrite(
                    QuotationStatus.WAITING_CONTRACT,
                    rejectedStatuses);
        } else {
            // 영업 담당자는 본인 관련 건만 조회
            if (user.getEmployeeId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED, "employeeId is null");
            }
            quotations = quotationRepository.findQuotationsReadyForContractRewrite(
                    user.getEmployeeId(),
                    QuotationStatus.WAITING_CONTRACT,
                    rejectedStatuses);
        }

        return quotations.stream()
                .map(q -> {
                    Client client = q.getClient();
                    Long authorId = q.getAuthor() != null ? q.getAuthor().getId() : null;
                    String managerName = client != null ? client.getManagerName() : null;
                    Long requestId = q.getQuotationRequest() != null ? q.getQuotationRequest().getId() : null;
                    Long dealId = q.getDeal() != null ? q.getDeal().getId() : null;
                    String memo = (authorId != null && authorId.equals(user.getEmployeeId())) ? q.getMemo() : null;
                    String requirements = q.getQuotationRequest() != null ? q.getQuotationRequest().getRequirements()
                            : null;

                    return new QuotationListResponse(
                            q.getId(),
                            q.getQuotationCode(),
                            client != null ? client.getId() : null,
                            client != null ? client.getClientName() : null,
                            managerName,
                            authorId,
                            q.getCreatedAt() != null ? q.getCreatedAt().toLocalDate() : null,
                            q.getStatus(),
                            requestId,
                            dealId,
                            memo,
                            requirements,
                            null,
                            List.of());
                })
                .toList();
    }

    /**
     * 재작성을 위한 반려된 계약서 목록 조회 (데이터 복사용)
     */
    public List<ContractListResponse> getRejectedContracts() {
        CustomUserDetails user = getAuthenticatedUser();

        // 1. 권한 체크: 영업 담당자(SALES_REP)와 관리자(ADMIN) 조회 가능
        if (user.getRole() != Role.SALES_REP && user.getRole() != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        List<ContractStatus> targetStatuses = List.of(
                ContractStatus.REJECTED_ADMIN,
                ContractStatus.REJECTED_CLIENT);

        List<ContractHeader> contracts;
        if (user.getRole() == Role.ADMIN) {
            // 관리자는 전역 조회
            contracts = contractRepository.findAllActiveRejectedContracts(
                    targetStatuses);
        } else {
            // 영업 담당자는 본인 관련 건만 조회
            if (user.getEmployeeId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED, "employeeId is null");
            }
            contracts = contractRepository.findActiveRejectedContracts(
                    user.getEmployeeId(),
                    targetStatuses);
        }

        Map<Long, String> reasonsMap = fetchReasonsMap(contracts);

        return contracts.stream()
                .map(c -> {
                    List<ContractResponse.ItemResponse> items = c.getItems().stream()
                            .map(item -> new ContractResponse.ItemResponse(
                                    item.getId(),
                                    item.getProduct() != null ? item.getProduct().getId() : null,
                                    item.getProductName(),
                                    item.getProductCategory(),
                                    item.getTotalQuantity(),
                                    item.getUnit(),
                                    item.getUnitPrice(),
                                    item.getAmount()))
                            .toList();

                    Client client = c.getClient();
                    Long authorId = c.getAuthor() != null ? c.getAuthor().getId() : null;
                    String managerName = client != null ? client.getManagerName() : null;
                    Long quotationId = c.getQuotation() != null ? c.getQuotation().getId() : null;
                    Long dealId = c.getDeal() != null ? c.getDeal().getId() : null;
                    String memo = (authorId != null && authorId.equals(user.getEmployeeId())) ? c.getMemo() : null;
                    String rejectionReason = reasonsMap.get(c.getId());

                    return new ContractListResponse(
                            c.getId(),
                            c.getContractCode(),
                            client != null ? client.getId() : null,
                            client != null ? client.getClientName() : null,
                            managerName,
                            authorId,
                            c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : null,
                            c.getStatus(),
                            quotationId,
                            dealId,
                            memo,
                            rejectionReason,
                            items);
                })
                .toList();
    }

    private Map<Long, String> fetchReasonsMap(List<ContractHeader> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Map.of();
        }

        List<Long> contractIds = contracts.stream()
                .map(ContractHeader::getId)
                .toList();

        List<com.monsoon.seedflowplus.domain.approval.dto.response.ReasonDto> reasons = approvalDecisionRepository.findReasonsByTargets(DealType.CNT, contractIds);
        if (reasons == null) return Map.of();

        return reasons.stream()
                .filter(dto -> dto.targetId() != null && dto.reason() != null)
                .collect(Collectors.toMap(
                        ReasonDto::targetId,
                        ReasonDto::reason,
                        (existing, replacement) -> existing));
    }

    private DealStage mapContractStage(ContractStatus status) {
        return switch (status) {
            case WAITING_ADMIN -> DealStage.PENDING_ADMIN;
            case REJECTED_ADMIN -> DealStage.REJECTED_ADMIN;
            case WAITING_CLIENT -> DealStage.PENDING_CLIENT;
            case REJECTED_CLIENT -> DealStage.REJECTED_CLIENT;
            case COMPLETED -> DealStage.APPROVED;
            case ACTIVE_CONTRACT -> DealStage.CONFIRMED;
            case EXPIRED -> DealStage.EXPIRED;
            case DELETED -> DealStage.CANCELED;
        };
    }

    private ActorType resolveActorType(CustomUserDetails userDetails) {
        return userDetails.getRole() == Role.ADMIN ? ActorType.ADMIN : ActorType.SALES_REP;
    }

    private Long resolveActorId(CustomUserDetails userDetails) {
        Long actorId = userDetails.getEmployeeId();
        if (actorId == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return actorId;
    }

    private void closeDealIfOpen(SalesDeal deal, LocalDateTime actionAt) {
        if (deal == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }
        if (deal.getClosedAt() != null) {
            return;
        }
        deal.close(actionAt);
    }

    private void restoreDealSnapshotAfterContractDelete(ContractHeader contract) {
        SalesDeal deal = contract.getDeal();
        if (deal == null) {
            return;
        }

        // 다중 문서 제안을 고려하여, 남은 문서들 중 최적의 상태로 Deal Snapshot 재계산
        recomputeDealSnapshot(deal);
    }

    private DealStage mapQuotationStage(QuotationStatus status) {
        return switch (status) {
            case WAITING_ADMIN -> DealStage.PENDING_ADMIN;
            case REJECTED_ADMIN -> DealStage.REJECTED_ADMIN;
            case WAITING_CLIENT, FINAL_APPROVED -> DealStage.PENDING_CLIENT;
            case WAITING_CONTRACT, COMPLETED -> DealStage.APPROVED;
            case REJECTED_CLIENT -> DealStage.REJECTED_CLIENT;
            case EXPIRED -> DealStage.EXPIRED;
            case DELETED -> DealStage.CANCELED;
        };
    }

    private void syncDealSnapshot(
            SalesDeal deal,
            DealStage stage,
            String status,
            DealType dealType,
            Long refId,
            String targetCode,
            LocalDateTime actionAt) {
        deal.updateSnapshot(stage, status, dealType, refId, targetCode, actionAt);
    }

    private void expireDeals(Map<Long, List<ExpiringContractContext>> expiringContextByDealId, LocalDateTime actionAt) {
        if (expiringContextByDealId.isEmpty()) {
            return;
        }
        salesDealRepository.findAllById(expiringContextByDealId.keySet())
                .forEach(deal -> {
                    List<ExpiringContractContext> contexts = expiringContextByDealId.get(deal.getId());
                    if (contexts == null || contexts.isEmpty()) {
                        return;
                    }

                    for (ExpiringContractContext context : contexts) {
                        // 1. 개별 계약서 만료 로그 기록
                        dealLogWriteService.write(
                            deal,
                            DealType.CNT,
                            context.contractId(),
                            context.contractCode(),
                            DealStage.CONFIRMED,
                            DealStage.EXPIRED,
                            ContractStatus.ACTIVE_CONTRACT.name(),
                            ContractStatus.EXPIRED.name(),
                            ActionType.EXPIRE,
                            actionAt,
                            ActorType.SYSTEM,
                            null,
                            null,
                            List.of(new DealLogWriteService.DiffField(
                                    "status",
                                    "문서 상태",
                                    ContractStatus.ACTIVE_CONTRACT.name(),
                                    ContractStatus.EXPIRED.name(),
                                    "STATUS")));
                    }

                    // 2. 다중 문서 제안을 고려하여 Deal Snapshot 재계산
                    recomputeDealSnapshot(deal);

                    // 3. 만약 모든 문서가 종결 상태이면 Deal도 닫음
                    if (isAllDocumentsClosed(deal)) {
                        closeDealIfOpen(deal, actionAt);
                    }
                });
    }

    private void recomputeDealSnapshot(SalesDeal deal) {
        // 1. 계약서 확인
        List<ContractHeader> contracts = contractRepository.findByDealId(deal.getId()).stream()
                .filter(c -> c.getStatus() != ContractStatus.DELETED)
                .sorted((c1, c2) -> {
                    int p1 = getContractStatusPriority(c1.getStatus());
                    int p2 = getContractStatusPriority(c2.getStatus());
                    if (p1 != p2) return Integer.compare(p1, p2);
                    return c2.getId().compareTo(c1.getId());
                })
                .toList();

        if (!contracts.isEmpty()) {
            ContractHeader bestCnt = contracts.get(0);
            syncDealSnapshot(
                    deal,
                    mapContractStage(bestCnt.getStatus()),
                    bestCnt.getStatus().name(),
                    DealType.CNT,
                    bestCnt.getId(),
                    bestCnt.getContractCode(),
                    LocalDateTime.now()
            );
            return;
        }

        // 2. 견적서 확인
        List<QuotationHeader> quotations = quotationRepository.findByDealId(deal.getId()).stream()
                .filter(q -> q.getStatus() != QuotationStatus.DELETED)
                .sorted((q1, q2) -> {
                    int p1 = getQuotationStatusPriority(q1.getStatus());
                    int p2 = getQuotationStatusPriority(q2.getStatus());
                    if (p1 != p2) return Integer.compare(p1, p2);
                    return q2.getId().compareTo(q1.getId());
                })
                .toList();

        if (!quotations.isEmpty()) {
            QuotationHeader bestQuo = quotations.get(0);
            syncDealSnapshot(
                    deal,
                    mapQuotationStage(bestQuo.getStatus()),
                    bestQuo.getStatus().name(),
                    DealType.QUO,
                    bestQuo.getId(),
                    bestQuo.getQuotationCode(),
                    LocalDateTime.now()
            );
            return;
        }

        // 3. RFQ 확인
        List<QuotationRequestHeader> requests = quotationRequestRepository.findByDealId(deal.getId()).stream()
                .filter(r -> r.getStatus() != QuotationRequestStatus.DELETED)
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .toList();

        if (!requests.isEmpty()) {
            QuotationRequestHeader bestRfq = requests.get(0);
            syncDealSnapshot(
                    deal,
                    mapQuotationRequestStage(bestRfq.getStatus()),
                    bestRfq.getStatus().name(),
                    DealType.RFQ,
                    bestRfq.getId(),
                    bestRfq.getRequestCode(),
                    LocalDateTime.now()
            );
            return;
        }

        // 4. 아무 문서도 없으면 초기 상태로 중립화
        deal.updateSnapshot(DealStage.CREATED, com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus.PENDING.name(), DealType.RFQ, 0L, null, LocalDateTime.now());
    }

    private int getContractStatusPriority(ContractStatus status) {
        return switch (status) {
            case ACTIVE_CONTRACT -> 1;
            case COMPLETED -> 2;
            case WAITING_CLIENT -> 3;
            case WAITING_ADMIN -> 4;
            case REJECTED_CLIENT -> 5;
            case REJECTED_ADMIN -> 6;
            case EXPIRED -> 7;
            default -> 8;
        };
    }

    private int getQuotationStatusPriority(QuotationStatus status) {
        return switch (status) {
            case COMPLETED -> 1;
            case WAITING_CONTRACT -> 2;
            case FINAL_APPROVED -> 3;
            case WAITING_CLIENT -> 4;
            case WAITING_ADMIN -> 5;
            case REJECTED_CLIENT -> 6;
            case REJECTED_ADMIN -> 7;
            case EXPIRED -> 8;
            default -> 9;
        };
    }

    private boolean isAllDocumentsClosed(SalesDeal deal) {
        boolean allCntClosed = contractRepository.findByDealId(deal.getId()).stream()
                .allMatch(c -> c.getStatus() == ContractStatus.EXPIRED || c.getStatus() == ContractStatus.DELETED);

        boolean allQuoClosed = quotationRepository.findByDealId(deal.getId()).stream()
                .allMatch(q -> q.getStatus() == QuotationStatus.EXPIRED
                        || q.getStatus() == QuotationStatus.DELETED
                        || q.getStatus() == QuotationStatus.REJECTED_ADMIN
                        || q.getStatus() == QuotationStatus.REJECTED_CLIENT);

        boolean allRfqClosed = quotationRequestRepository.findByDealId(deal.getId()).stream()
                .allMatch(r -> r.getStatus() == QuotationRequestStatus.DELETED
                        || r.getStatus() == QuotationRequestStatus.COMPLETED);

        return allCntClosed && allQuoClosed && allRfqClosed;
    }

    private DealStage mapQuotationRequestStage(QuotationRequestStatus status) {
        return switch (status) {
            case PENDING -> DealStage.CREATED;
            case REVIEWING -> DealStage.IN_PROGRESS;
            case COMPLETED -> DealStage.APPROVED;
            case DELETED -> DealStage.CANCELED;
        };
    }

    private record ExpiringContractContext(Long contractId, String contractCode, Long dealId) {
    }

}
