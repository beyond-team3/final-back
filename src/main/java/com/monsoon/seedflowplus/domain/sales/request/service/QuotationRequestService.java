package com.monsoon.seedflowplus.domain.sales.request.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.request.dto.request.QuotationRequestCreateRequest;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestDetail;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationRequestService {

    private final QuotationRequestRepository quotationRequestRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void createQuotationRequest(QuotationRequestCreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 1. Role 검증: CLIENT만 가능
        if (userDetails.getRole() != Role.CLIENT) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Client client = clientRepository.findById(userDetails.getClientId())
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 2. Header 생성
        QuotationRequestHeader header = QuotationRequestHeader.create(client, request.requirements());

        // 3. Detail 생성 및 추가
        request.items().forEach(itemRequest -> {
            QuotationRequestDetail detail = new QuotationRequestDetail(
                    itemRequest.productId() != null ? productRepository.getReferenceById(itemRequest.productId())
                            : null,
                    itemRequest.productCategory(),
                    itemRequest.productName(),
                    itemRequest.quantity());
            header.addItem(detail);
        });

        // 4. 저장
        quotationRequestRepository.save(header);

        // 5. requestCode 업데이트: RFQ-YYYYMMDD-ID
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String requestCode = "RFQ-" + datePart + "-" + header.getId();
        header.updateRequestCode(requestCode);
    }

    @Transactional
    public void deleteQuotationRequest(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationRequestHeader header = quotationRequestRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // PENDING 상태인 경우만 삭제 가능
        if (header.getStatus() != QuotationRequestStatus.PENDING) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        // 권한 체크: 오직 본인(Client)인 경우만 삭제 가능 (Admin 포함 타인 불가)
        if (userDetails.getRole() != Role.CLIENT || !header.getClient().getId().equals(userDetails.getClientId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        header.delete();
        quotationRequestRepository.save(header);
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
