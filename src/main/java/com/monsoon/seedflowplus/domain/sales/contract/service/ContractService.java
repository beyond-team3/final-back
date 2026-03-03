package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.contract.dto.request.ContractCreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractPrefillResponse;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractDetailRepository;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final QuotationRepository quotationRepository; // typo in codebase probably, using what's there if needed
    // or fixing
    private final ContractRepository contractRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final ProductRepository productRepository;

    public ContractPrefillResponse getPrefillData(Long quotationId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        QuotationHeader quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 1. 상태 검증: 최종 승인된 견적서만 가능
        if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        // 2. 권한 검증: 본인이 작성한 영업사원만 가능 (관리자, 거래처 등 차단)
        if (userDetails.getRole() != Role.SALES_REP ||
                quotation.getAuthor() == null ||
                !quotation.getAuthor().getId().equals(userDetails.getEmployeeId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

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

    @Transactional
    public void createContract(ContractCreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 1. 견적서 조회 및 검증
        QuotationHeader quotation = quotationRepository.findById(request.quotationId())
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        if (quotation.getStatus() != QuotationStatus.FINAL_APPROVED) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        if (userDetails.getRole() != Role.SALES_REP ||
                quotation.getAuthor() == null ||
                !quotation.getAuthor().getId().equals(userDetails.getEmployeeId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        // 2. 계약서 헤더 생성
        String contractCode = "CT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));

        ContractHeader contract = ContractHeader.create(
                contractCode,
                quotation,
                quotation.getClient(),
                quotation.getAuthor(),
                request.startDate(),
                request.endDate(),
                request.billingCycle(),
                request.specialTerms(),
                request.memo(),
                quotation.getTotalAmount());

        // 3. 계약 상세 품목 생성 및 추가
        request.items().forEach(itemRequest -> {
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

        // 4. 저장
        contractRepository.save(contract);
    }

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return userDetails;
    }
}
