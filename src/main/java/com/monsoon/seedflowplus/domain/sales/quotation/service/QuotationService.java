package com.monsoon.seedflowplus.domain.sales.quotation.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationListResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationDetail;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationRequestRepository quotationRequestRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void createQuotation(QuotationCreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 1. 권한 검증: SALES_REP만 가능
        if (userDetails.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 2. 담당 거래처 확인: 자신이 담당한 client에 대해서만 작성 가능
        if (client.getManagerEmployee() == null
                || !client.getManagerEmployee().getId().equals(userDetails.getEmployeeId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Employee author = employeeRepository.findById(userDetails.getEmployeeId())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        QuotationRequestHeader quotationRequest = null;
        if (request.requestId() != null) {
            // 3. 견적요청서 기반 작성 시 검증
            quotationRequest = quotationRequestRepository.findById(request.requestId())
                    .orElseThrow(() -> new CoreException(ErrorType.RFQ_NOT_FOUND));

            // 상태가 PENDING이어야 함
            if (quotationRequest.getStatus() != QuotationRequestStatus.PENDING) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
            }

            // 요청된 제품 목록이 일치하는지 검증 (상품 정보만 비교)
            Set<Long> requestProductIds = quotationRequest.getItems().stream()
                    .map(item -> item.getProduct().getId())
                    .collect(Collectors.toSet());

            Set<Long> inputProductIds = request.items().stream()
                    .map(QuotationCreateRequest.QuotationItemRequest::productId)
                    .collect(Collectors.toSet());

            if (!requestProductIds.equals(inputProductIds)) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_DATA); // 혹은 전용 에러 타입 필요
            }

            // 상태를 REVIEWING으로 변경
            quotationRequest.updateStatus(QuotationRequestStatus.REVIEWING);
        }

        // 4. 총액 계산
        BigDecimal totalAmount = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 견적서 생성 (임시 코드)
        String tempCode = "TEMP-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        QuotationHeader quotation = QuotationHeader.create(
                quotationRequest,
                tempCode,
                client,
                author,
                totalAmount,
                request.memo());

        // 6. 품목 추가
        request.items().forEach(itemRequest -> {
            QuotationDetail detail = new QuotationDetail(
                    itemRequest.productId() != null
                            ? productRepository.getReferenceById(itemRequest.productId())
                            : null,
                    itemRequest.productCategory(),
                    itemRequest.productName(),
                    itemRequest.quantity(),
                    itemRequest.unit(),
                    itemRequest.unitPrice(),
                    itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            quotation.addItem(detail);
        });

        // 7. 저장 및 코드 업데이트
        quotationRepository.save(quotation);
        String finalCode = "QUO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + quotation.getId();
        quotation.updateQuotationCode(finalCode);
    }

    public QuotationResponse getQuotationDetail(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationHeader quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 1. 접근 권한 상위 레벨 확인 (ADMIN, SALES_REP 담당자, CLIENT 담당자)
        validateAccess(quotation, userDetails);

        // 2. 메모 가시성 처리: 본인이 작성한 SALES_REP만 확인 가능
        String memo = null;
        if (quotation.getAuthor() != null
                && quotation.getAuthor().getId().equals(userDetails.getEmployeeId())) {
            memo = quotation.getMemo();
        }

        List<QuotationResponse.QuotationItemResponse> items = quotation.getItems().stream()
                .map(item -> new QuotationResponse.QuotationItemResponse(
                        item.getProduct() != null ? item.getProduct().getId() : null,
                        item.getProductName(),
                        item.getProductCategory(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getUnitPrice(),
                        item.getAmount()))
                .toList();

        return new QuotationResponse(
                quotation.getId(),
                quotation.getQuotationCode(),
                quotation.getQuotationRequest() != null ? quotation.getQuotationRequest().getId()
                        : null,
                quotation.getClient().getClientName(),
                quotation.getAuthor() != null ? quotation.getAuthor().getEmployeeName() : null,
                quotation.getStatus(),
                quotation.getTotalAmount(),
                quotation.getExpiredDate(),
                memo,
                quotation.getCreatedAt(),
                items);
    }

    public List<QuotationListResponse> getApprovedQuotations() {
        CustomUserDetails user = getAuthenticatedUser();

        if (user.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        List<QuotationHeader> quotations = quotationRepository.findAllByStatusAndAuthorId(
                QuotationStatus.FINAL_APPROVED,
                user.getEmployeeId());

        return quotations.stream()
                .map(q -> new QuotationListResponse(
                        q.getId(),
                        q.getQuotationCode(),
                        q.getClient().getClientName(),
                        q.getAuthor() != null ? q.getAuthor().getEmployeeName() : null,
                        q.getCreatedAt().toLocalDate(),
                        q.getStatus()))
                .toList();
    }

    @Transactional
    public void deleteQuotation(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationHeader quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 1. 권한 체크: ADMIN(모두) 또는 SALES_REP(본인이 작성한 것만)
        if (userDetails.getRole() != Role.ADMIN) {
            if (userDetails.getRole() != Role.SALES_REP ||
                    quotation.getAuthor() == null ||
                    !quotation.getAuthor().getId().equals(userDetails.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
        }

        // 2. 상태 체크: FINAL_APPROVED, WAITING_CONTRACT, COMPLETED, EXPIRED인 경우 삭제 불가
        QuotationStatus status = quotation.getStatus();
        if (status == QuotationStatus.FINAL_APPROVED ||
                status == QuotationStatus.WAITING_CONTRACT ||
                status == QuotationStatus.COMPLETED ||
                status == QuotationStatus.EXPIRED) {
            throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
        }

        // 3. 논리 삭제 처리
        quotation.updateStatus(QuotationStatus.DELETED);

        // 4. 관련 RFQ 상태 복구 (검토 중인 경우 다시 대기 상태로)
        if (quotation.getQuotationRequest() != null
                && quotation.getQuotationRequest().getStatus() == QuotationRequestStatus.REVIEWING) {
            quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.PENDING);
        }
    }

    private void validateAccess(QuotationHeader quotation, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.SALES_REP) {
            if (quotation.getAuthor() == null
                    || !quotation.getAuthor().getId().equals(user.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        if (user.getRole() == Role.CLIENT) {
            // 거래처 번호가 일치하는지 확인
            if (!quotation.getClient().getId().equals(user.getClientId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            // 미승인(관리자 승인 대기/반려) 상태는 거래처가 조회할 수 없음
            if (quotation.getStatus() == com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus.WAITING_ADMIN
                    ||
                    quotation.getStatus() == com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus.REJECTED_ADMIN) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        throw new CoreException(ErrorType.ACCESS_DENIED);
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
