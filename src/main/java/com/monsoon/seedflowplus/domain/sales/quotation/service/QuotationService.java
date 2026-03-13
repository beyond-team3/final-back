package com.monsoon.seedflowplus.domain.sales.quotation.service;

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
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalDecisionRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.monsoon.seedflowplus.domain.approval.dto.response.ReasonDto;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationRequestRepository quotationRequestRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogWriteService dealLogWriteService;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final DealLogQueryService dealLogQueryService;

    @Transactional
    public void createQuotation(QuotationCreateRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 1. 권한 검증: SALES_REP만 가능
        if (userDetails.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Client client = clientRepository.findByIdWithLock(request.clientId())
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 2. 담당 거래처 확인: 자신이 담당한 client에 대해서만 작성 가능
        if (client.getManagerEmployee() == null
                || !client.getManagerEmployee().getId().equals(userDetails.getEmployeeId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Employee author = employeeRepository.findById(userDetails.getEmployeeId())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        QuotationRequestHeader quotationRequest = null;
        SalesDeal deal;
        if (request.requestId() != null) {
            // 3. 견적요청서 기반 작성 시 검증 (비관적 락 사용으로 동시성 문제 해결)
            quotationRequest = quotationRequestRepository.findByIdWithLock(request.requestId())
                    .orElseThrow(() -> new CoreException(ErrorType.RFQ_NOT_FOUND));

            // 상태가 PENDING이거나 REVIEWING이어야 함
            if (quotationRequest.getStatus() != QuotationRequestStatus.PENDING &&
                    quotationRequest.getStatus() != QuotationRequestStatus.REVIEWING) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
            }

            // REVIEWING인 경우, 이미 승인 대기 중인 견적서가 있는지 확인 (중복 작성 방지)
            if (quotationRequest.getStatus() == QuotationRequestStatus.REVIEWING) {
                boolean hasActiveQuotation = quotationRepository.findByQuotationRequestId(quotationRequest.getId())
                        .stream()
                        .anyMatch(q -> q.getStatus() == QuotationStatus.WAITING_ADMIN
                                || q.getStatus() == QuotationStatus.WAITING_CLIENT);
                if (hasActiveQuotation) {
                    throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
                }
            }

            // 요청된 제품 목록이 일치하는지 검증 (상품 정보만 비교)
            Set<Long> requestProductIds = quotationRequest.getItems().stream()
                    .map(item -> item.getProduct().getId())
                    .collect(Collectors.toSet());

            Set<Long> inputProductIds = request.items().stream()
                    .map(QuotationCreateRequest.QuotationItemRequest::productId)
                    .collect(Collectors.toSet());

            if (!requestProductIds.equals(inputProductIds)) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_DATA);
            }

            // 상태를 REVIEWING으로 변경
            quotationRequest.updateStatus(QuotationRequestStatus.REVIEWING);
            SalesDeal resolvedDeal = quotationRequest.getDeal() != null
                    ? quotationRequest.getDeal()
                    : resolveOrCreateOpenDeal(client, author);

            // RFQ 분기에서도 Deal 락을 획득하고 중복 활성 견적 체크 (CodeRabbit 지적사항)
            deal = salesDealRepository.findByIdWithLock(resolvedDeal.getId())
                    .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));

            boolean hasActiveQuotation = quotationRepository.findByDealId(deal.getId()).stream()
                    .anyMatch(q -> q.getStatus() == QuotationStatus.WAITING_ADMIN
                            || q.getStatus() == QuotationStatus.WAITING_CLIENT);
            if (hasActiveQuotation) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
            }
        } else {
            // 3-2. 일반 견적 작성 시 검증
            SalesDeal resolvedDeal = resolveOrCreateOpenDeal(client, author);
            // 동시성 제어를 위해 Deal에 락을 획득하여 다시 조회
            deal = salesDealRepository.findByIdWithLock(resolvedDeal.getId())
                    .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));

            // 해당 Deal에 이미 승인 대기 중인 견적서가 있는지 확인 (비관적 락으로 동시성 보호됨)
            boolean hasActiveQuotation = quotationRepository.findByDealId(deal.getId()).stream()
                    .anyMatch(q -> q.getStatus() == QuotationStatus.WAITING_ADMIN
                            || q.getStatus() == QuotationStatus.WAITING_CLIENT);
            if (hasActiveQuotation) {
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
            }
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
                deal,
                author,
                totalAmount,
                request.memo());

        // 6. 품목 추가
        request.items().forEach(itemRequest -> {
            Long productId = itemRequest.productId();
            com.monsoon.seedflowplus.domain.product.entity.Product product = null;

            if (productId != null) {
                if (!productRepository.existsById(productId)) {
                    throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
                }
                product = productRepository.getReferenceById(productId);
            }

            QuotationDetail detail = new QuotationDetail(
                    product,
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

        dealPipelineFacade.recordAndSync(
                deal,
                DealType.QUO,
                quotation.getId(),
                finalCode,
                deal.getCurrentStage(),
                DealStage.PENDING_ADMIN,
                QuotationStatus.WAITING_ADMIN.name(),
                QuotationStatus.WAITING_ADMIN.name(),
                ActionType.CREATE,
                null,
                ActorType.SALES_REP,
                userDetails.getEmployeeId(),
                null,
                List.of(
                        new DealLogWriteService.DiffField("totalAmount", "총액", null, totalAmount, "MONEY"),
                        new DealLogWriteService.DiffField("itemCount", "견적 품목 수", null, request.items().size(),
                                "COUNT")));
    }

    public QuotationResponse getQuotationDetail(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationHeader quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        if (quotation.getStatus() == QuotationStatus.DELETED) {
            throw new CoreException(ErrorType.QUOTATION_NOT_FOUND);
        }

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
                quotation.getQuotationRequest() != null ? quotation.getQuotationRequest().getId() : null,
                quotation.getClient().getId(),
                quotation.getClient().getClientName(),
                quotation.getAuthor() != null ? quotation.getAuthor().getId() : null,
                quotation.getAuthor() != null ? quotation.getAuthor().getEmployeeName() : null,
                quotation.getStatus(),
                quotation.getTotalAmount(),
                quotation.getExpiredDate(),
                memo,
                quotation.getCreatedAt(),
                items,
                dealLogQueryService.getRecentDocumentLogs(
                        quotation.getDeal() != null ? quotation.getDeal().getId() : null,
                        DealType.QUO,
                        quotation.getId()));
    }

    public List<QuotationListResponse> getApprovedQuotations() {
        CustomUserDetails user = getAuthenticatedUser();

        if (user.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        List<QuotationHeader> quotations = quotationRepository.findAllByStatusAndAuthorId(
                QuotationStatus.FINAL_APPROVED,
                user.getEmployeeId());

        Map<Long, String> reasonsMap = fetchReasonsMap(quotations);

        return quotations.stream()
                .map(q -> {
                    List<QuotationResponse.QuotationItemResponse> items = q.getItems().stream()
                            .map(item -> new QuotationResponse.QuotationItemResponse(
                                    item.getProduct() != null ? item.getProduct().getId() : null,
                                    item.getProductName(),
                                    item.getProductCategory(),
                                    item.getQuantity(),
                                    item.getUnit(),
                                    item.getUnitPrice(),
                                    item.getAmount()))
                            .toList();

                    return new QuotationListResponse(
                            q.getId(),
                            q.getQuotationCode(),
                            q.getClient().getId(),
                            q.getClient().getClientName(),
                            q.getAuthor() != null ? q.getAuthor().getEmployeeName() : null,
                            q.getAuthor() != null ? q.getAuthor().getId() : null,
                            q.getCreatedAt().toLocalDate(),
                            q.getStatus(),
                            q.getQuotationRequest() != null ? q.getQuotationRequest().getId() : null,
                            q.getDeal().getId(),
                            (q.getAuthor() != null && q.getAuthor().getId().equals(user.getEmployeeId())) ? q.getMemo() : null,
                            q.getQuotationRequest() != null ? q.getQuotationRequest().getRequirements() : null,
                            reasonsMap.get(q.getId()), // 맵에서 조회 (N+1 해결)
                            items);
                })
                .toList();
    }

    /**
     * 반려 또는 만료된 견적서 목록을 조회합니다 (데이터 복사용).
     */
    public List<QuotationListResponse> getRejectedQuotations() {
        CustomUserDetails user = getAuthenticatedUser();

        // 1. 권한 체크: 영업 담당자(SALES_REP)만 반려 목록 조회 가능
        if (user.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        if (user.getEmployeeId() == null) {
            // 인증 정보에 직원 ID가 누락된 경우 (보안/세션 이상 상태 은닉 방지)
            throw new CoreException(ErrorType.UNAUTHORIZED, "employeeId is null");
        }

        List<QuotationHeader> quotations = quotationRepository.findActiveRejectedQuotations(
                user.getEmployeeId(),
                List.of(QuotationStatus.REJECTED_ADMIN, QuotationStatus.REJECTED_CLIENT, QuotationStatus.EXPIRED),
                QuotationStatus.DELETED);

        // 2. N+1 문제 해결: 한 번의 쿼리로 반려 사유 조회
        Map<Long, String> reasonsMap = fetchReasonsMap(quotations);

        return quotations.stream()
                .map(q -> {
                    List<QuotationResponse.QuotationItemResponse> items = q.getItems().stream()
                            .map(item -> new QuotationResponse.QuotationItemResponse(
                                    item.getProduct() != null ? item.getProduct().getId() : null,
                                    item.getProductName(),
                                    item.getProductCategory(),
                                    item.getQuantity(),
                                    item.getUnit(),
                                    item.getUnitPrice(),
                                    item.getAmount()))
                            .toList();

                    return new QuotationListResponse(
                            q.getId(),
                            q.getQuotationCode(),
                            q.getClient().getId(),
                            q.getClient().getClientName(),
                            q.getAuthor() != null ? q.getAuthor().getEmployeeName()
                                    : (q.getClient().getManagerEmployee() != null
                                            ? q.getClient().getManagerEmployee().getEmployeeName()
                                            : null),
                            q.getAuthor() != null ? q.getAuthor().getId()
                                    : (q.getClient().getManagerEmployee() != null
                                            ? q.getClient().getManagerEmployee().getId()
                                            : null),
                            q.getCreatedAt().toLocalDate(),
                            q.getStatus(),
                            q.getQuotationRequest() != null ? q.getQuotationRequest().getId() : null,
                            q.getDeal().getId(),
                            (q.getAuthor() != null && q.getAuthor().getId().equals(user.getEmployeeId())) ? q.getMemo() : null,
                            q.getQuotationRequest() != null ? q.getQuotationRequest().getRequirements() : null,
                            reasonsMap.get(q.getId()), // 맵에서 미리 조회해둔 반려 사유를 가져옴 (N+1 해결)
                            items);
                })
                .toList();
    }

    @Transactional
    public void completeAfterContractApproval(
            QuotationHeader quotation,
            ActorType actorType,
            Long actorId,
            LocalDateTime actionAt) {
        if (quotation == null) {
            throw new IllegalArgumentException("quotation must not be null");
        }

        String fromStatus = quotation.getStatus().name();
        String toStatus = QuotationStatus.COMPLETED.name();
        dealPipelineFacade.validateTransitionOrThrow(DealType.QUO, fromStatus, ActionType.CONVERT, toStatus);

        quotation.updateStatus(QuotationStatus.COMPLETED);
        dealLogWriteService.write(
                quotation.getDeal(),
                DealType.QUO,
                quotation.getId(),
                quotation.getQuotationCode(),
                DealStage.APPROVED,
                DealStage.APPROVED,
                fromStatus,
                toStatus,
                ActionType.CONVERT,
                actionAt,
                actorType,
                actorId,
                null,
                List.of(new DealLogWriteService.DiffField("status", "문서 상태", fromStatus, toStatus, "STATUS")));
    }

    @Transactional
    public void deleteQuotation(Long id) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        QuotationHeader quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        // 1. 권한 체크: 오직 작성자(SALES_REP) 본인만 삭제 가능
        if (userDetails.getRole() != Role.SALES_REP ||
                quotation.getAuthor() == null ||
                !quotation.getAuthor().getId().equals(userDetails.getEmployeeId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        // 2. 상태 체크: 관리자 승인 이전인 WAITING_ADMIN 상태에서만 삭제 가능
        QuotationStatus status = quotation.getStatus();
        if (status != QuotationStatus.WAITING_ADMIN) {
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
            if (quotation.getStatus() == QuotationStatus.WAITING_ADMIN
                    || quotation.getStatus() == QuotationStatus.REJECTED_ADMIN) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }

        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    /**
     * 견적서 상태 자동 동기화
     * WAITING_ADMIN(관리자 승인 대기) 상태인 견적서 중 만료일(expiredDate)이 도래한 경우 EXPIRED로 변경
     */
    @Transactional
    public void syncQuotationStatuses() {
        LocalDate today = LocalDate.now();

        // 1. 만료 대상 처리 (승인 대기 상태인데 만료일이 오늘이거나 이전인 경우)
        int expiredQuoCount = quotationRepository.updateStatusForExpiration(
                QuotationStatus.WAITING_ADMIN, QuotationStatus.EXPIRED, today);

        // RFQ 복구 로직은 의도적으로 비활성화됨 (반려/만료 시 REVIEWING 상태 유지 정책)
        int recoveredRfqCount = 0;

        if (expiredQuoCount > 0 || recoveredRfqCount > 0) {
            log.info("[QuotationService] 상태 동기화 완료: 견적 만료 {}건, RFQ 복구 {}건",
                    expiredQuoCount, recoveredRfqCount);
        }
    }

    private Map<Long, String> fetchReasonsMap(List<QuotationHeader> quotations) {
        if (quotations == null || quotations.isEmpty()) {
            return Map.of();
        }

        List<Long> quotationIds = quotations.stream()
                .map(QuotationHeader::getId)
                .toList();

        return approvalDecisionRepository
                .findReasonsByTargets(DealType.QUO, quotationIds).stream()
                .collect(Collectors.toMap(
                        ReasonDto::targetId,
                        ReasonDto::reason,
                        (existing, replacement) -> existing));
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

        // 주의: 호출부(createQuotation)에서 이미 Client에 대해 비관적 락(findByIdWithLock)을 획득한 상태여야 함
        return salesDealRepository.findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(clientId)
                .orElseGet(() -> createDealBootstrap(client, ownerEmp));
    }

    private SalesDeal createDealBootstrap(Client client, Employee ownerEmp) {
        if (ownerEmp == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }
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
}
