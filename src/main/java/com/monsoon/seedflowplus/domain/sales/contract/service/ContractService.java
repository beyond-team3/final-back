package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractPrefillResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final QuotationRepository quotationRepository;

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
                quotation.getClient().getManagerName(), // UI상 '담당자'가 Client의 매니저인지 확인 필요하나 일단 매핑
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

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return userDetails;
    }
}
