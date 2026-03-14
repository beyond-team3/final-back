package com.monsoon.seedflowplus.domain.billing.invoice.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import com.monsoon.seedflowplus.domain.billing.payment.repository.PaymentRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.request.InvoiceCreateRequest;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceDetailResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceListResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoicePublishResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceStatementRepository;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.notification.event.InvoiceIssuedEvent;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatementRepository invoiceStatementRepository;
    private final ContractRepository contractHeaderRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogQueryService dealLogQueryService;
    private final DealScheduleSyncService dealScheduleSyncService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final PaymentRepository paymentRepository;

    /**
     * 청구서 수동 생성 (영업사원)
     * - 기간 내 청구 가능한 명세서 자동 조회 후 연결
     * - 이미 DRAFT/PUBLISHED 청구서가 있으면 생성 불가
     */
    @Transactional
    public InvoiceDetailResponse createInvoice(InvoiceCreateRequest request, Long employeeId) {
        // 1. 계약 조회
        ContractHeader contract = contractHeaderRepository.findById(request.getContractId())
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

        // 2. 중복 청구서 생성 방지 (DRAFT 또는 PUBLISHED 상태가 이미 존재하면 불가)
        if (invoiceRepository.existsByContractIdAndStatusIn(
                request.getContractId(),
                List.of(InvoiceStatus.DRAFT, InvoiceStatus.PUBLISHED))) {
            throw new CoreException(ErrorType.INVOICE_ALREADY_EXISTS);
        }

        // 3. 영업사원 / 거래처 조회
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CoreException(ErrorType.EMPLOYEE_NOT_FOUND));
        Client client = contract.getClient();
        if (contract.getDeal() == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }

        // 4. 청구서 생성
        String invoiceCode = generateCode("INV");
        Invoice invoice = Invoice.create(
                contract.getId(), client, contract.getDeal(), employee,
                LocalDate.now(), request.getStartDate(), request.getEndDate(),
                invoiceCode, request.getMemo()
        );
        invoiceRepository.save(invoice);

        // 5. 청구 가능한 명세서 조회 후 InvoiceStatement 연결
        List<Statement> billableStatements =
                invoiceStatementRepository.findBillableStatements(contract.getId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Statement statement : billableStatements) {
            InvoiceStatement invoiceStatement = InvoiceStatement.create(invoice, statement);
            invoiceStatementRepository.save(invoiceStatement);
            totalAmount = totalAmount.add(statement.getTotalAmount());
        }

        // 6. 청구서 금액 업데이트 후 저장 (invoiceCode unique 충돌 시 최대 3회 재시도)
        invoice.updateAmount(totalAmount);

        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                invoiceRepository.saveAndFlush(invoice);
                break;
            } catch (DataIntegrityViolationException e) {
                if (!isInvoiceCodeViolation(e) || i == maxRetries - 1) throw e;
                // 새 객체 생성X
                String newCode = generateCode("INV");
                invoice.changeCode(newCode);  // 이 메서드 추가 필요
            }
        }


        // 7. 연결된 InvoiceStatement 목록 조회 후 반환
        List<InvoiceStatement> invoiceStatements =
                invoiceStatementRepository.findAllByInvoiceId(invoice.getId());

        return InvoiceDetailResponse.of(
                invoice,
                invoiceStatements,
                dealLogQueryService.getRecentDocumentLogs(
                        invoice.getDeal() != null ? invoice.getDeal().getId() : null,
                        DealType.INV,
                        invoice.getId()
                ),
                resolveContractCode(invoice.getContractId())
        );
    }

    /**
     * 청구서 발행 확정 (DRAFT → PUBLISHED)
     */
    @Transactional
    public InvoicePublishResponse publishInvoice(Long invoiceId, CustomUserDetails principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new CoreException(ErrorType.INVOICE_ALREADY_PUBLISHED);
        }

        String fromStatus = invoice.getStatus().name();
        dealPipelineFacade.validateTransitionOrThrow(
                DealType.INV,
                fromStatus,
                ActionType.ISSUE,
                InvoiceStatus.PUBLISHED.name()
        );

        // publish 시점에 included=true 명세서 금액 합산 후 반영
        List<InvoiceStatement> includedStatements =
                invoiceStatementRepository.findAllByInvoiceId(invoiceId);
        BigDecimal publishTotalAmount = includedStatements.stream()
                .filter(InvoiceStatement::isIncluded)
                .map(is -> is.getStatement().getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.updateAmount(publishTotalAmount);

        invoice.publish();

        String paymentCode = generateCode("PAY");

        Payment payment = Payment.create(
                invoice,
                invoice.getClient(),
                invoice.getDeal(),
                null,          // 아직 결제 안했으니까 method 없음
                paymentCode
        );

        paymentRepository.save(payment);

        ActorType actorType = resolveActorType(principal);
        Long actorId = resolveActorId(actorType, principal);
        dealPipelineFacade.recordAndSync(
                invoice.getDeal(),
                DealType.INV,
                invoice.getId(),
                invoice.getInvoiceCode(),
                mapInvoiceStage(InvoiceStatus.valueOf(fromStatus)),
                DealStage.ISSUED,
                fromStatus,
                InvoiceStatus.PUBLISHED.name(),
                ActionType.ISSUE,
                null,
                actorType,
                actorId,
                null,
                List.of(new DealLogWriteService.DiffField("status", "청구서 상태", fromStatus, InvoiceStatus.PUBLISHED.name(), "STATUS"))
        );
        syncPaymentDueSchedule(invoice, principal);
        publishInvoiceIssuedNotification(invoice);
        return InvoicePublishResponse.from(invoice);
    }

    /**
     * 명세서 포함/제외 토글 (영업사원)
     * - 제외 시 included=false → 다음 청구 주기에 재포함 대상
     * - tbl_invoice UPDATE 제거: 동시 다건 토글 시 데드락 원인이었음
     * - 금액 재계산은 publishInvoice() 시점에 일괄 처리
     */
    @Transactional
    public InvoiceDetailResponse toggleStatement(Long invoiceId, Long statementId, CustomUserDetails principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new CoreException(ErrorType.INVOICE_ALREADY_PUBLISHED);
        }

        InvoiceStatement invoiceStatement = invoiceStatementRepository
                .findAllByInvoiceId(invoiceId).stream()
                .filter(is -> is.getStatement().getId().equals(statementId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

        // 포함/제외 토글 (tbl_invoice_statement.is_included 만 변경)
        if (invoiceStatement.isIncluded()) {
            invoiceStatement.exclude();
        } else {
            invoiceStatement.include();
        }

        // tbl_invoice UPDATE 제거 (데드락 원인)
        // dealPipelineFacade.recordAndSync 제거 (toggle마다 딜로그 불필요)
        // → 금액 반영 및 로그는 publishInvoice() 에서 일괄 처리

        List<InvoiceStatement> allStatements =
                invoiceStatementRepository.findAllByInvoiceId(invoiceId);

        return InvoiceDetailResponse.of(
                invoice,
                allStatements,
                dealLogQueryService.getRecentDocumentLogs(
                        invoice.getDeal() != null ? invoice.getDeal().getId() : null,
                        DealType.INV,
                        invoice.getId()
                ),
                resolveContractCode(invoice.getContractId())
        );
    }

    /**
     * 첨구서 단건 조회 (공통 - memo 없음)
     */
    public InvoiceResponse getInvoice(Long invoiceId) {
        return getInvoice(invoiceId, null);
    }

    public InvoiceResponse getInvoice(Long invoiceId, CustomUserDetails principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));
        validateInvoiceReadPermission(invoice, principal);
        List<InvoiceStatement> invoiceStatements =
                invoiceStatementRepository.findAllByInvoiceId(invoiceId);
        return InvoiceResponse.of(
                invoice,
                invoiceStatements,
                dealLogQueryService.getRecentDocumentLogs(
                        invoice.getDeal() != null ? invoice.getDeal().getId() : null,
                        DealType.INV,
                        invoice.getId()
                )
        );
    }

    /**
     * 청구서 단건 조회 (영업사원 전용 - memo 포함)
     */
    public InvoiceDetailResponse getInvoiceDetail(Long invoiceId) {
        return getInvoiceDetail(invoiceId, null);
    }

    public InvoiceDetailResponse getInvoiceDetail(Long invoiceId, CustomUserDetails principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));
        validateInvoiceReadPermission(invoice, principal);
        List<InvoiceStatement> invoiceStatements =
                invoiceStatementRepository.findAllByInvoiceId(invoiceId);
        String contractCode = resolveContractCode(invoice.getContractId());
        return InvoiceDetailResponse.of(
                invoice,
                invoiceStatements,
                dealLogQueryService.getRecentDocumentLogs(
                        invoice.getDeal() != null ? invoice.getDeal().getId() : null,
                        DealType.INV,
                        invoice.getId()
                ),
                contractCode
        );
    }

    /** contractId -> contractCode 헬퍼 */
    private String resolveContractCode(Long contractId) {
        if (contractId == null) return null;
        return contractHeaderRepository.findById(contractId)
                .map(ContractHeader::getContractCode)
                .orElse(null);
    }

    /**
     * 청구서 목록 조회 (전체)
     */
    public List<InvoiceListResponse> getInvoices() {
        return invoiceRepository.findAll().stream()
                .map(invoice -> InvoiceListResponse.from(
                        invoice,
                        resolveContractCode(invoice.getContractId())  // ← 이 줄이 핵심
                ))
                .toList();
    }

    /**
     * 청구서 목록 조회 (거래처별)
     */
    public List<InvoiceListResponse> getInvoicesByClient(Long clientId) {
        return invoiceRepository.findAllByClientId(clientId).stream()
                .map(InvoiceListResponse::from)
                .toList();
    }

    /**
     * 스케줄러용 자동 생성 (billing_cycle 기준)
     * InvoiceScheduler에서 호출
     */
    @Transactional
    public void createDraftInvoice(ContractHeader contract) {

        if (invoiceRepository.existsByContractIdAndStatusIn(
                contract.getId(),
                List.of(InvoiceStatus.DRAFT, InvoiceStatus.PUBLISHED))) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = today.withDayOfMonth(today.lengthOfMonth());

        String invoiceCode = generateCode("INV");
        if (contract.getDeal() == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND, "contractId=" + contract.getId());
        }

        Invoice invoice = Invoice.create(
                contract.getId(),
                contract.getClient(),
                contract.getDeal(),
                null,
                today,
                startDate,
                endDate,
                invoiceCode,
                null
        );

        invoiceRepository.save(invoice);

        List<Statement> billableStatements =
                invoiceStatementRepository.findBillableStatements(contract.getId());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Statement statement : billableStatements) {
            invoiceStatementRepository.save(
                    InvoiceStatement.create(invoice, statement)
            );
            totalAmount = totalAmount.add(statement.getTotalAmount());
        }

        invoice.updateAmount(totalAmount);

        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                invoiceRepository.saveAndFlush(invoice);
                break;
            } catch (DataIntegrityViolationException e) {
                if (!isInvoiceCodeViolation(e) || i == maxRetries - 1) throw e;

                String newCode = generateCode("INV");
                invoice.changeCode(newCode);
            }
        }
    }


    // invoice_code 유니크 제약 위반 여부 판별
    private boolean isInvoiceCodeViolation(DataIntegrityViolationException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("invoice_code") || msg.contains("uk_") || msg.contains("unique");
    }

    // INV-20260223-001 형식으로 코드 생성
    private String generateCode(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayPrefix = prefix + "-" + date + "-";

        int nextSeq = invoiceRepository.findMaxSuffixByPrefix(todayPrefix)
                .map(max -> max + 1)
                .orElse(1);

        return todayPrefix + String.format("%03d", nextSeq);
    }

    private ActorType resolveActorType(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            return ActorType.SYSTEM;
        }
        return switch (principal.getRole()) {
            case CLIENT -> ActorType.CLIENT;
            case ADMIN -> ActorType.ADMIN;
            case SALES_REP -> ActorType.SALES_REP;
        };
    }

    private Long resolveActorId(ActorType actorType, CustomUserDetails principal) {
        if (actorType == ActorType.SYSTEM) {
            return null;
        }
        if (principal == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        Long actorId = actorType == ActorType.CLIENT
                ? principal.getClientId()
                : principal.getEmployeeId();
        if (actorId == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return actorId;
    }

    private void syncPaymentDueSchedule(Invoice invoice, CustomUserDetails principal) {
        if (invoice.getInvoiceDate() == null) {
            return;
        }
        if (invoice.getDeal() == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }

        Long assigneeUserId = resolveScheduleAssigneeUserId(invoice.getDeal(), principal, invoice.getClient().getId());
        LocalDateTime startAt = invoice.getInvoiceDate().atStartOfDay();

        dealScheduleSyncService.upsertFromEvent(new DealScheduleUpsertCommand(
                "INV_" + invoice.getId() + "_PAYMENT_DUE",
                invoice.getDeal().getId(),
                invoice.getClient().getId(),
                assigneeUserId,
                DealScheduleEventType.PAYMENT_DUE,
                DealDocType.INV,
                invoice.getId(),
                null,
                "결제 마감: " + invoice.getClient().getClientName(),
                null,
                startAt,
                startAt.plusDays(1),
                LocalDateTime.now()
        ));
    }

    private Long resolveScheduleAssigneeUserId(SalesDeal deal, CustomUserDetails principal, Long clientId) {
        if (deal.getOwnerEmp() != null && deal.getOwnerEmp().getId() != null) {
            java.util.Optional<Long> ownerUserId = userRepository.findByEmployeeId(deal.getOwnerEmp().getId())
                    .map(User::getId);
            if (ownerUserId.isPresent()) {
                return ownerUserId.get();
            }
        }
        if (principal != null && principal.getUserId() != null) {
            return principal.getUserId();
        }
        if (clientId != null) {
            return userRepository.findByClientId(clientId)
                    .map(User::getId)
                    .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
        }
        throw new CoreException(ErrorType.USER_NOT_FOUND);
    }

    private DealStage mapInvoiceStage(InvoiceStatus status) {
        return switch (status) {
            case DRAFT -> DealStage.CREATED;
            case PUBLISHED -> DealStage.ISSUED;
            case PAID -> DealStage.PAID;
            case CANCELED -> DealStage.CANCELED;
        };
    }

    private void validateInvoiceReadPermission(Invoice invoice, CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        // ADMIN: 전체 허용
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        // CLIENT: 본인 거래처 청구서만 허용
        if (principal.getRole() == Role.CLIENT) {
            Long principalClientId = principal.getClientId();
            Long invoiceClientId = invoice.getClient() != null ? invoice.getClient().getId() : null;
            if (principalClientId == null || !principalClientId.equals(invoiceClientId)) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return;
        }
        // SALES_REP: 담당 거래처(tbl_client.employee_id)의 청구서만 허용
        Long principalEmployeeId = principal.getEmployeeId();
        if (principalEmployeeId == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        // tbl_client.employee_id = managerEmployee (Client 엔티티 필드명)
        Long clientManagerEmpId = invoice.getClient() != null && invoice.getClient().getManagerEmployee() != null
                ? invoice.getClient().getManagerEmployee().getId()
                : null;
        if (clientManagerEmpId != null && principalEmployeeId.equals(clientManagerEmpId)) {
            return;
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private void publishInvoiceIssuedNotification(Invoice invoice) {
        if (invoice.getClient() == null || invoice.getClient().getId() == null) {
            log.warn("Skipping invoice issued notification due to missing client or client id. invoiceId={}, client={}",
                    invoice.getId(), invoice.getClient());
            return;
        }
        userRepository.findByClientId(invoice.getClient().getId())
                .map(User::getId)
                .ifPresentOrElse(
                        userId -> notificationEventPublisher.publishAfterCommit(new InvoiceIssuedEvent(
                                userId,
                                invoice.getId(),
                                invoice.getInvoiceCode(),
                                invoice.getClient().getClientName(),
                                LocalDateTime.now()
                        )),
                        () -> log.warn(
                                "No client user mapping found for invoice issued notification. clientId={}, clientName={}, invoiceId={}, invoiceCode={}",
                                invoice.getClient().getId(),
                                invoice.getClient().getClientName(),
                                invoice.getId(),
                                invoice.getInvoiceCode()
                        )
                );
    }

    public List<InvoiceListResponse> getInvoicesByEmployee(Long employeeId) {
        return invoiceRepository.findAllByEmployeeId(employeeId).stream()
                .map(invoice -> InvoiceListResponse.from(
                        invoice,
                        resolveContractCode(invoice.getContractId())
                ))
                .toList();
    }
}
