package com.monsoon.seedflowplus.domain.sales.quotation.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
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
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationListResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationResponse;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationDetail;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalDecisionRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
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
import java.util.Optional;
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
    private final ContractRepository contractRepository;
    private final QuotationRequestRepository quotationRequestRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SalesDealRepository salesDealRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogWriteService dealLogWriteService;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final DealLogQueryService dealLogQueryService;
    private final ApprovalSubmissionService approvalSubmissionService;
    private final ApprovalCancellationService approvalCancellationService;
    private final DealScheduleSyncService dealScheduleSyncService;

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
                throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS,
                        "선택한 견적 요청서가 현재 작성 가능한 상태(대기 또는 검토 중)가 아닙니다.");
            }

            if (quotationRequest.getStatus() == QuotationRequestStatus.REVIEWING) {
                Optional<QuotationHeader> activeQuo = quotationRepository
                        .findByQuotationRequestId(quotationRequest.getId())
                        .stream()
                        .filter(q -> q.getStatus() == QuotationStatus.WAITING_ADMIN
                                || q.getStatus() == QuotationStatus.WAITING_CLIENT)
                        .findFirst();
                if (activeQuo.isPresent()) {
                    throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS,
                            String.format("해당 요청서에 대해 이미 진행 중인 견적서(%s)가 존재합니다.", activeQuo.get().getQuotationCode()));
                }
            }

            // 요청된 제품 목록이 일치하는지 검증 (상품 정보만 비교)
            Set<Long> requestProductIds = quotationRequest.getItems().stream()
                    .map(item -> item.getProduct() != null ? item.getProduct().getId() : null)
                    .collect(Collectors.toSet());

            Set<Long> inputProductIds = request.items().stream()
                    .map(QuotationCreateRequest.QuotationItemRequest::productId)
                    .collect(Collectors.toSet());

            if (!requestProductIds.equals(inputProductIds)) {
                String detail = String.format("요청된 제품 목록(%s)과 입력된 제품 목록(%s)이 일치하지 않습니다.",
                        requestProductIds, inputProductIds);
                throw new CoreException(ErrorType.INVALID_DOCUMENT_DATA, detail);
            }

            // 상태를 REVIEWING으로 변경
            quotationRequest.updateStatus(QuotationRequestStatus.REVIEWING);
            SalesDeal resolvedDeal = quotationRequest.getDeal() != null
                    ? quotationRequest.getDeal()
                    : resolveOrCreateOpenDeal(client, author);

            // RFQ 분기에서도 Deal 락을 획득하여 동시성 제어 (단, 중복 견적 체크는 RFQ 레벨에서만 수행하도록 완화)
            deal = salesDealRepository.findByIdWithLock(resolvedDeal.getId())
                    .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));
        } else {
            // 3-2. 일반 견적 작성 시 검증
            SalesDeal resolvedDeal = resolveOrCreateOpenDeal(client, author);
            // 동시성 제어를 위해 Deal에 락을 획득하여 다시 조회
            deal = salesDealRepository.findByIdWithLock(resolvedDeal.getId())
                    .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));

            // ⚠️ [정책 완화] 일반 견적 작성 시에도 Deal 레벨의 강제 중복 금지 규칙을 제거함.
            // 다양한 제안(Option)을 동시에 보낼 수 있도록 허용함.
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

        approvalSubmissionService.submitFromDocumentCreation(
                DealType.QUO,
                quotation.getId(),
                finalCode,
                userDetails
        );
        syncQuotationExpirationSchedule(quotation, userDetails);
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

                    Client client = q.getClient();
                    SalesDeal deal = q.getDeal();
                    Long authorId = q.getAuthor() != null ? q.getAuthor().getId() : null;
                    Long requestId = q.getQuotationRequest() != null ? q.getQuotationRequest().getId() : null;
                    String requirements = q.getQuotationRequest() != null ? q.getQuotationRequest().getRequirements() : null;

                    return new QuotationListResponse(
                            q.getId(),
                            q.getQuotationCode(),
                            client != null ? client.getId() : null,
                            client != null ? client.getClientName() : null,
                            client != null ? client.getManagerName() : null,
                            authorId,
                            q.getCreatedAt() != null ? q.getCreatedAt().toLocalDate() : null,
                            q.getStatus(),
                            requestId,
                            deal != null ? deal.getId() : null,
                            (authorId != null && authorId.equals(user.getEmployeeId())) ? q.getMemo() : null,
                            requirements,
                            reasonsMap.get(q.getId()),
                            items);
                })
                .toList();
    }

    /**
     * 반려 또는 만료된 견적서 목록을 조회합니다 (데이터 복사용).
     */
    public List<QuotationListResponse> getRejectedQuotations() {
        CustomUserDetails user = getAuthenticatedUser();

        // 1. 권한 체크: 영업 담당자(SALES_REP)와 관리자(ADMIN) 조회 가능
        if (user.getRole() != Role.SALES_REP && user.getRole() != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        List<QuotationStatus> targetStatuses = List.of(
                QuotationStatus.REJECTED_ADMIN,
                QuotationStatus.REJECTED_CLIENT,
                QuotationStatus.EXPIRED);

        log.info("[QuotationService] 반려/만료 견적서 조회 시작 - User: {}, Role: {}", user.getLoginId(), user.getRole());

        List<QuotationHeader> quotations;
        if (user.getRole() == Role.ADMIN) {
            // 관리자는 전역 조회
            quotations = quotationRepository.findAllActiveRejectedQuotations(
                    targetStatuses);
            log.info("[QuotationService] 관리자 전역 조회 완료 - 검색 건수: {}건", quotations.size());
        } else {
            // 영업 담당자는 본인 관련 건만 조회
            if (user.getEmployeeId() == null) {
                log.warn("[QuotationService] 영업사원 employeeId 누락 - User: {}", user.getLoginId());
                throw new CoreException(ErrorType.UNAUTHORIZED, "employeeId is null");
            }
            quotations = quotationRepository.findActiveRejectedQuotations(
                    user.getEmployeeId(),
                    targetStatuses);
            log.info("[QuotationService] 영업사원({}) 본인 건 조회 완료 - 검색 건수: {}건", user.getEmployeeId(), quotations.size());
        }

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

                    Client client = q.getClient();
                    SalesDeal deal = q.getDeal();
                    Long authorId = q.getAuthor() != null ? q.getAuthor().getId()
                                    : (client != null && client.getManagerEmployee() != null
                                            ? client.getManagerEmployee().getId()
                                            : null);
                    Long requestId = q.getQuotationRequest() != null ? q.getQuotationRequest().getId() : null;
                    String requirements = q.getQuotationRequest() != null ? q.getQuotationRequest().getRequirements() : null;

                    return new QuotationListResponse(
                            q.getId(),
                            q.getQuotationCode(),
                            client != null ? client.getId() : null,
                            client != null ? client.getClientName() : null,
                            client != null ? client.getManagerName() : null,
                            authorId,
                            q.getCreatedAt() != null ? q.getCreatedAt().toLocalDate() : null,
                            q.getStatus(),
                            requestId,
                            deal != null ? deal.getId() : null,
                            (authorId != null && authorId.equals(user.getEmployeeId())) ? q.getMemo() : null,
                            requirements,
                            reasonsMap.get(q.getId()),
                            items);
                })
                .toList();
    }

    /**
     * 계약서 재작성이 가능한 견적서 목록 조회
     * 상태가 WAITING_CONTRACT(계약 대기)이며, 연결된 모든 계약서가 반려 상태인 건만 조회.
     */
    public List<QuotationListResponse> getRejectedQuotationsForContract() {
        CustomUserDetails user = getAuthenticatedUser();

        // 1. 권한 체크: 영업 담당자(SALES_REP)와 관리자(ADMIN) 조회 가능
        if (user.getRole() != Role.SALES_REP && user.getRole() != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        List<ContractStatus> rejectedStatuses = List.of(
                ContractStatus.REJECTED_ADMIN,
                ContractStatus.REJECTED_CLIENT);

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

                    Client client = q.getClient();
                    SalesDeal deal = q.getDeal();
                    Long authorId = q.getAuthor() != null ? q.getAuthor().getId() : null;
                    Long requestId = q.getQuotationRequest() != null ? q.getQuotationRequest().getId() : null;
                    String requirements = q.getQuotationRequest() != null ? q.getQuotationRequest().getRequirements() : null;

                    return new QuotationListResponse(
                            q.getId(),
                            q.getQuotationCode(),
                            client != null ? client.getId() : null,
                            client != null ? client.getClientName() : null,
                            client != null ? client.getManagerName() : null,
                            authorId,
                            q.getCreatedAt() != null ? q.getCreatedAt().toLocalDate() : null,
                            q.getStatus(),
                            requestId,
                            deal != null ? deal.getId() : null,
                            (authorId != null && authorId.equals(user.getEmployeeId())) ? q.getMemo() : null,
                            requirements,
                            null, // 상위 견적서 자체의 반려 사유는 없음 (승인된 상태이므로)
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
        deleteQuotationExpirationSchedule(quotation.getId());
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
        String fromStatus = quotation.getStatus().name();
        DealStage fromStage = mapQuotationStage(quotation.getStatus());
        quotation.updateStatus(QuotationStatus.DELETED);
        deleteQuotationExpirationSchedule(quotation.getId());
        approvalCancellationService.cancelPendingRequest(DealType.QUO, quotation.getId());

        // 4. 관련 RFQ 상태 복구 (검토 중인 경우 다시 대기 상태로)
        DealStage toStage = DealStage.CANCELED;
        String toStatus = QuotationStatus.DELETED.name();
        if (quotation.getQuotationRequest() != null
                && quotation.getQuotationRequest().getStatus() == QuotationRequestStatus.REVIEWING) {
            quotation.getQuotationRequest().updateStatus(QuotationRequestStatus.PENDING);
            toStage = mapQuotationRequestStage(QuotationRequestStatus.PENDING);
            toStatus = QuotationRequestStatus.PENDING.name();
        }

        if (quotation.getDeal() != null) {
            dealLogWriteService.write(
                    quotation.getDeal(),
                    DealType.QUO,
                    quotation.getId(),
                    quotation.getQuotationCode(),
                    fromStage,
                    toStage,
                    fromStatus,
                    toStatus,
                    ActionType.CANCEL,
                    LocalDateTime.now(),
                    ActorType.SALES_REP,
                    userDetails.getEmployeeId(),
                    null,
                    List.of(new DealLogWriteService.DiffField(
                            "status",
                            "문서 상태",
                            fromStatus,
                            toStatus,
                            "STATUS"))
            );
        }

        restoreDealSnapshotAfterQuotationDelete(quotation, fromStage, fromStatus);
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
        List<QuotationHeader> expiringQuotations = quotationRepository
                .findByStatusAndExpiredDateLessThanEqual(QuotationStatus.WAITING_ADMIN, today);

        // 1. 만료 대상 처리 (승인 대기 상태인데 만료일이 오늘이거나 이전인 경우)
        int expiredQuoCount = quotationRepository.updateStatusForExpiration(
                QuotationStatus.WAITING_ADMIN, QuotationStatus.EXPIRED, today);

        // RFQ 복구 로직은 의도적으로 비활성화됨 (반려/만료 시 REVIEWING 상태 유지 정책)
        int recoveredRfqCount = 0;

        if (expiredQuoCount > 0 || recoveredRfqCount > 0) {
            log.info("[QuotationService] 상태 동기화 완료: 견적 만료 {}건, RFQ 복구 {}건",
                    expiredQuoCount, recoveredRfqCount);
        }

        List<QuotationHeader> updatedExpiredQuotations = expiringQuotations.isEmpty()
                ? List.of()
                : quotationRepository.findByIdInAndStatusAndExpiredDateLessThanEqual(
                        expiringQuotations.stream().map(QuotationHeader::getId).toList(),
                        QuotationStatus.EXPIRED,
                        today
                );
        Map<Long, List<ExpiringQuotationContext>> expiringContextByDealId = updatedExpiredQuotations.stream()
                .filter(quotation -> quotation.getDeal() != null && quotation.getDeal().getId() != null)
                .collect(Collectors.groupingBy(
                        quotation -> quotation.getDeal().getId(),
                        java.util.LinkedHashMap::new,
                        Collectors.mapping(
                                quotation -> new ExpiringQuotationContext(
                                        quotation.getId(),
                                        quotation.getQuotationCode(),
                                        quotation.getDeal().getId()),
                                Collectors.toList())));
        expireDeals(expiringContextByDealId, LocalDateTime.now());
    }

    private Map<Long, String> fetchReasonsMap(List<QuotationHeader> quotations) {
        if (quotations == null || quotations.isEmpty()) {
            return Map.of();
        }

        List<Long> quotationIds = quotations.stream()
                .map(QuotationHeader::getId)
                .toList();

        List<com.monsoon.seedflowplus.domain.approval.dto.response.ReasonDto> reasons = approvalDecisionRepository.findReasonsByTargets(DealType.QUO, quotationIds);
        if (reasons == null) return Map.of();

        return reasons.stream()
                .filter(dto -> dto.targetId() != null && dto.reason() != null)
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

    private void syncQuotationExpirationSchedule(QuotationHeader quotation, CustomUserDetails userDetails) {
        if (quotation.getExpiredDate() == null || quotation.getDeal() == null) {
            return;
        }

        dealScheduleSyncService.upsertFromEvent(new DealScheduleUpsertCommand(
                quotationExpirationExternalKey(quotation.getId()),
                quotation.getDeal().getId(),
                quotation.getClient().getId(),
                resolveScheduleAssigneeUserId(userDetails),
                DealScheduleEventType.FOLLOW_UP_REMINDER,
                DealDocType.QUO,
                quotation.getId(),
                null,
                "견적 만료일: " + quotation.getClient().getClientName(),
                null,
                quotation.getExpiredDate().atStartOfDay(),
                quotation.getExpiredDate().plusDays(1).atStartOfDay(),
                LocalDateTime.now()
        ));
    }

    private void deleteQuotationExpirationSchedule(Long quotationId) {
        if (quotationId == null) {
            return;
        }
        dealScheduleSyncService.deleteByExternalKey(quotationExpirationExternalKey(quotationId));
    }

    private String quotationExpirationExternalKey(Long quotationId) {
        return "QUO_" + quotationId + "_EXPIRATION";
    }

    private Long resolveScheduleAssigneeUserId(CustomUserDetails userDetails) {
        if (userDetails.getUserId() != null) {
            return userDetails.getUserId();
        }
        if (userDetails.getEmployeeId() != null) {
            return userRepository.findByEmployeeId(userDetails.getEmployeeId())
                    .map(User::getId)
                    .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
        }
        throw new CoreException(ErrorType.USER_NOT_FOUND);
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

    private DealStage mapQuotationStage(QuotationStatus status) {
        return switch (status) {
            case WAITING_ADMIN -> DealStage.PENDING_ADMIN;
            case REJECTED_ADMIN -> DealStage.REJECTED_ADMIN;
            case WAITING_CLIENT, FINAL_APPROVED -> DealStage.PENDING_CLIENT;
            case REJECTED_CLIENT -> DealStage.REJECTED_CLIENT;
            case WAITING_CONTRACT, COMPLETED -> DealStage.APPROVED;
            case EXPIRED -> DealStage.EXPIRED;
            case DELETED -> DealStage.CANCELED;
        };
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

    private void restoreDealSnapshotAfterQuotationDelete(QuotationHeader quotation, DealStage deletedFromStage, String deletedFromStatus) {
        SalesDeal deal = quotation.getDeal();
        if (deal == null) {
            return;
        }

        // 다중 견적 제안을 고려하여, 남은 문서들 중 최적의 상태로 Deal Snapshot 재계산
        recomputeDealSnapshot(deal);
    }

    private DealStage mapQuotationRequestStage(QuotationRequestStatus status) {
        return switch (status) {
            case PENDING -> DealStage.CREATED;
            case REVIEWING -> DealStage.IN_PROGRESS;
            case COMPLETED -> DealStage.APPROVED;
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
            LocalDateTime actionAt
    ) {
        deal.updateSnapshot(stage, status, dealType, refId, targetCode, actionAt);
    }

    private void expireDeals(Map<Long, List<ExpiringQuotationContext>> expiringContextByDealId, LocalDateTime actionAt) {
        if (expiringContextByDealId.isEmpty()) {
            return;
        }
        salesDealRepository.findAllById(expiringContextByDealId.keySet())
                .forEach(deal -> {
                    List<ExpiringQuotationContext> contexts = expiringContextByDealId.get(deal.getId());
                    if (contexts == null || contexts.isEmpty()) {
                        return;
                    }

                    for (ExpiringQuotationContext context : contexts) {
                        // 1. 개별 견적서 만료 로그 기록
                        dealLogWriteService.write(
                            deal,
                            DealType.QUO,
                            context.quotationId(),
                            context.quotationCode(),
                            DealStage.PENDING_ADMIN,
                            DealStage.EXPIRED,
                            QuotationStatus.WAITING_ADMIN.name(),
                            QuotationStatus.EXPIRED.name(),
                            ActionType.EXPIRE,
                            actionAt,
                            ActorType.SYSTEM,
                            null,
                            null,
                            List.of(new DealLogWriteService.DiffField(
                                    "status",
                                    "문서 상태",
                                    QuotationStatus.WAITING_ADMIN.name(),
                                    QuotationStatus.EXPIRED.name(),
                                    "STATUS"))
                    );
                    }

                    // 2. 다중 견적 제안을 고려하여 Deal Snapshot 재계산
                    recomputeDealSnapshot(deal);

                    // 3. 만약 모든 문서가 종결(EXPIRED/DELETED/REJECTED 등) 상태이면 Deal도 닫음
                    if (isAllDocumentsClosed(deal)) {
                        closeDealIfOpen(deal, actionAt);
                    }
                });
    }

    private void recomputeDealSnapshot(SalesDeal deal) {
        // 1. 계약서 확인 (계약이 있으면 견적보다 우선함)
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
        deal.updateSnapshot(DealStage.CREATED, QuotationRequestStatus.PENDING.name(), DealType.RFQ, 0L, null, LocalDateTime.now());
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
        // 모든 계약서가 만료/삭제 상태인지 확인
        boolean allCntClosed = contractRepository.findByDealId(deal.getId()).stream()
                .allMatch(c -> c.getStatus() == ContractStatus.EXPIRED || c.getStatus() == ContractStatus.DELETED);

        // 모든 견적서가 만료/삭제/반려 상태인지 확인
        boolean allQuoClosed = quotationRepository.findByDealId(deal.getId()).stream()
                .allMatch(q -> q.getStatus() == QuotationStatus.EXPIRED
                        || q.getStatus() == QuotationStatus.DELETED
                        || q.getStatus() == QuotationStatus.REJECTED_ADMIN
                        || q.getStatus() == QuotationStatus.REJECTED_CLIENT);

        // 모든 RFQ가 삭제 또는 완료 상태인지 확인
        boolean allRfqClosed = quotationRequestRepository.findByDealId(deal.getId()).stream()
                .allMatch(r -> r.getStatus() == QuotationRequestStatus.DELETED
                        || r.getStatus() == QuotationRequestStatus.COMPLETED);

        return allCntClosed && allQuoClosed && allRfqClosed;
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

    private record ExpiringQuotationContext(Long quotationId, String quotationCode, Long dealId) {
    }
}
