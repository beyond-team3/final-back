package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
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
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractResponse;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractSimpleResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogQueryService dealLogQueryService;

    /**
     * 특정 거래처의 계약 목록 조회 (드롭다운용)
     */
    public List<ContractSimpleResponse> getContractsByClient(Long clientId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 권한 체크: 해당 거래처의 데이터에 접근할 권한이 있는지 확인
        validateClientAccess(client, userDetails);

        return contractRepository.findByClientOrderByEndDateAsc(client).stream()
                .filter(c -> c.getStatus() != ContractStatus.DELETED) // 공통: 삭제된 계약 제외
                .filter(c -> {
                    // 거래처인 경우 ACTIVE_CONTRACT만 표시
                    if (userDetails.getRole() == Role.CLIENT) {
                        return c.getStatus() == ContractStatus.ACTIVE_CONTRACT;
                    }
                    return true;
                })
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

    public ContractPrefillResponse getPrefillData(Long quotationId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        QuotationHeader quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 1. 상태 검증: 최종 승인된 견적서만 가능
        if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        // 2. 권한 검증: 본인이 작성한 영업사원만 가능 (관리자, 거래처 등 차단)
        validateQuotationAuthorAccess(quotation, userDetails);

        return new ContractPrefillResponse(
                quotation.getId(),
                quotation.getQuotationCode(),
                quotation.getClient().getId(),
                quotation.getClient().getClientName(),
                quotation.getClient().getManagerName(),
                quotation.getTotalAmount(),
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
                contract.getClient().getClientName(),
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

    private void validateAccess(ContractHeader contract, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.SALES_REP) {
            if (contract.getAuthor() == null || !contract.getAuthor().getId().equals(user.getEmployeeId())) {
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

        contract.delete();

        // 연관된 견적서 및 견적요청서 상태 복구 (가드 추가: 터미널 상태 보호)
        QuotationHeader quotation = contract.getQuotation();
        if (quotation != null) {
            // 견적서 상태: WAITING_CONTRACT 일 때만 FINAL_APPROVED로 복구
            if (quotation.getStatus() == QuotationStatus.WAITING_CONTRACT) {
                quotation.updateStatus(QuotationStatus.FINAL_APPROVED);
            }

            // 견적요청서 상태: 완료(COMPLETED) 상태일 때만 검토 중(REVIEWING)으로 복구
            if (quotation.getQuotationRequest() != null &&
                    quotation.getQuotationRequest().getStatus() == QuotationRequestStatus.COMPLETED) {
                quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.REVIEWING);
            }
        }
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

            if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
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
                    : resolveOrCreateOpenDeal(client, author);
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
            deal = resolveOrCreateOpenDeal(client, author);
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

    private SalesDeal resolveOrCreateOpenDeal(Client client, Employee ownerEmp) {
        Long clientId = client.getId();
        if (clientId == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }
        return salesDealRepository.findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(clientId)
                .orElseGet(() -> createDealBootstrap(client, ownerEmp));
    }

    private SalesDeal createDealBootstrap(Client client, Employee ownerEmp) {
        if (ownerEmp == null) {
            // TODO(BAC-70): QUO 없이 CNT 시작 시 owner 정책 확정 필요
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
}
