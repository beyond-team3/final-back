package com.monsoon.seedflowplus.domain.billing.statement.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceStatementRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.notification.event.StatementIssuedEvent;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementListResponse;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementResponse;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.billing.statement.repository.StatementRepository;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderDetailRepository;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatementService {

    private final StatementRepository statementRepository;
    private final InvoiceStatementRepository invoiceStatementRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogQueryService dealLogQueryService;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final OrderDetailRepository orderDetailRepository;

    /**
     * 주문 확정(CONFIRMED) 시 명세서 자동 생성
     * OrderApprovalConfirmedEventHandler -> OrderService.confirmOrderFromApproval() 흐름에서 호출
     */
    @Transactional
    public void createStatement(OrderHeader orderHeader, ActorType actorType, Long actorId) {
        if (orderHeader.getDeal() == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }

        // 동일 주문에 대한 명세서 중복 생성 방지
        statementRepository.findByOrderHeader_Id(orderHeader.getId())
                .ifPresent(existing -> {
                    throw new CoreException(ErrorType.STATEMENT_ALREADY_ISSUED);
                });

        // statementCode unique 충돌 시에만 최대 3회 재시도
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                createAndRecordStatement(orderHeader, actorType, actorId);
                break;
            } catch (DataIntegrityViolationException e) {
                // statement_code 유니크 충돌만 재시도, 그 외(FK 등)는 즉시 전파
                if (!isStatementCodeViolation(e) || i == maxRetries - 1) throw e;
            }
        }
    }

    // 삭제 정책상 STMT는 별도 delete API 없이 cancel API를 삭제 의미로 사용한다.
    @Transactional
    public StatementResponse cancelStatement(Long statementId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getEmployeeId() == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));
        if (statement.getDeal() == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }

        String fromStatus = statement.getStatus().name();
        dealPipelineFacade.validateTransitionOrThrow(
                DealType.STMT,
                fromStatus,
                ActionType.CANCEL,
                StatementStatus.CANCELED.name()
        );

        statement.cancel();

        ActorType actorType = resolveActorType(userDetails);
        Long actorId = resolveActorId(actorType, userDetails);
        dealPipelineFacade.recordAndSync(
                statement.getDeal(),
                DealType.STMT,
                statement.getId(),
                statement.getStatementCode(),
                mapStatementStage(StatementStatus.valueOf(fromStatus)),
                DealStage.CANCELED,
                fromStatus,
                StatementStatus.CANCELED.name(),
                ActionType.CANCEL,
                null,
                actorType,
                actorId,
                null,
                List.of(new DealLogWriteService.DiffField("status", "명세서 상태", fromStatus, StatementStatus.CANCELED.name(), "STATUS"))
        );

        return toStatementResponse(statement);
    }

    public StatementResponse getStatement(Long statementId, CustomUserDetails userDetails) {

        if (userDetails == null)
            throw new CoreException(ErrorType.ACCESS_DENIED);

        if (userDetails.getClientId() != null) {
            // 거래처 로그인
            Statement statement = statementRepository
                    .findByIdAndOrderHeader_Client_Id(statementId, userDetails.getClientId())
                    .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

            return toStatementResponse(statement);
        }

        if (userDetails.getEmployeeId() != null) {
            // 영업사원은 조회 허용 (또는 추가 검증)
            Statement statement = statementRepository.findById(statementId)
                    .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

            return toStatementResponse(statement);
        }

        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    /**
     * 명세서 목록 조회
     */
    public List<StatementListResponse> getStatements() {
        return statementRepository.findAll().stream()
                .map(StatementListResponse::from)
                .toList();
    }

    // STMT-20260223-001 형식으로 코드 생성 (DB MAX suffix 방식)
    private String generateCode(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayPrefix = prefix + "-" + date + "-";

        int nextSeq = statementRepository.findMaxSuffixByPrefix(todayPrefix)
                .map(max -> max + 1)
                .orElse(1);

        return todayPrefix + String.format("%03d", nextSeq);
    }

    // statement_code 유니크 제약 위반 여부 판별
    private boolean isStatementCodeViolation(DataIntegrityViolationException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("statement_code") || msg.contains("uk_") || msg.contains("unique");
    }

    private DealStage mapStatementStage(StatementStatus status) {
        return switch (status) {
            case ISSUED -> DealStage.ISSUED;
            case CANCELED -> DealStage.CANCELED;
        };
    }

    private ActorType resolveActorType(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            return ActorType.SYSTEM;
        }
        return switch (principal.getRole()) {
            case CLIENT -> ActorType.CLIENT;
            case ADMIN -> ActorType.ADMIN;
            default -> ActorType.SALES_REP;
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

    private List<com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto> recentLogs(Statement statement) {
        return dealLogQueryService.getRecentDocumentLogs(
                statement.getDeal() != null ? statement.getDeal().getId() : null,
                DealType.STMT,
                statement.getId()
        );
    }

    private Long resolveInvoiceId(Statement statement) {
        return invoiceStatementRepository.findTopByStatementIdAndIncludedTrueOrderByIdDesc(statement.getId())
                .map(invoiceStatement -> invoiceStatement.getInvoice().getId())
                .orElse(null);
    }

    private StatementResponse toStatementResponse(Statement statement) {
        return StatementResponse.from(
                statement,
                resolveInvoiceId(statement),
                recentLogs(statement),
                orderDetailRepository.findByOrderHeader_Id(statement.getOrderHeader().getId())
        );
    }

    private Statement createAndRecordStatement(
            OrderHeader orderHeader,
            ActorType actorType,
            @Nullable Long actorId
    ) {
        String code = generateCode("STMT");
        Statement statement = Statement.create(orderHeader, orderHeader.getDeal(), orderHeader.getTotalAmount(), code);
        statementRepository.saveAndFlush(statement);

        dealPipelineFacade.recordAndSync(
                orderHeader.getDeal(),
                DealType.STMT,
                statement.getId(),
                statement.getStatementCode(),
                DealStage.ISSUED,
                DealStage.ISSUED,
                StatementStatus.ISSUED.name(),
                StatementStatus.ISSUED.name(),
                ActionType.CREATE,
                null,
                actorType,
                actorId,
                null,
                List.of(
                        new DealLogWriteService.DiffField("isInitialCreate", "초기 생성 이벤트", null, true, "BOOLEAN"),
                        new DealLogWriteService.DiffField("totalAmount", "명세서 금액", null, statement.getTotalAmount(), "MONEY"),
                        new DealLogWriteService.DiffField("orderCode", "주문 번호", null, orderHeader.getOrderCode(), "REFERENCE")
                )
        );
        publishStatementIssuedNotifications(statement, orderHeader);
        return statement;
    }

    private void publishStatementIssuedNotifications(Statement statement, OrderHeader orderHeader) {
        LocalDateTime occurredAt = statement.getCreatedAt() != null ? statement.getCreatedAt() : LocalDateTime.now();
        resolveStatementRecipientUserIds(orderHeader).forEach(userId ->
                notificationEventPublisher.publishAfterCommit(new StatementIssuedEvent(
                        userId,
                        statement.getId(),
                        statement.getStatementCode(),
                        orderHeader.getOrderCode(),
                        occurredAt
                )));
    }

    private List<Long> resolveStatementRecipientUserIds(OrderHeader orderHeader) {
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        LinkedHashSet<Long> employeeIds = new LinkedHashSet<>();
        LinkedHashSet<Long> clientIds = new LinkedHashSet<>();

        if (orderHeader.getEmployee() != null && orderHeader.getEmployee().getId() != null) {
            employeeIds.add(orderHeader.getEmployee().getId());
        }
        if (orderHeader.getDeal() != null && orderHeader.getDeal().getOwnerEmp() != null
                && orderHeader.getDeal().getOwnerEmp().getId() != null) {
            employeeIds.add(orderHeader.getDeal().getOwnerEmp().getId());
        }
        if (orderHeader.getClient() != null && orderHeader.getClient().getId() != null) {
            clientIds.add(orderHeader.getClient().getId());
        }

        LinkedHashMap<Long, Long> userIdByEmployeeId = employeeIds.isEmpty()
                ? new LinkedHashMap<>()
                : userRepository.findAllByEmployeeIdIn(employeeIds.stream().toList()).stream()
                        .filter(user -> user.getEmployee() != null && user.getEmployee().getId() != null)
                        .collect(LinkedHashMap::new,
                                (map, user) -> map.put(user.getEmployee().getId(), user.getId()),
                                LinkedHashMap::putAll);
        LinkedHashMap<Long, Long> userIdByClientId = clientIds.isEmpty()
                ? new LinkedHashMap<>()
                : userRepository.findAllByClientIdIn(clientIds.stream().toList()).stream()
                        .filter(user -> user.getClient() != null && user.getClient().getId() != null)
                        .collect(LinkedHashMap::new,
                                (map, user) -> map.put(user.getClient().getId(), user.getId()),
                                LinkedHashMap::putAll);

        addResolvedUserId(userIds, userIdByEmployeeId, orderHeader.getEmployee() == null ? null : orderHeader.getEmployee().getId(),
                "order employee", "employeeId");
        addResolvedUserId(userIds, userIdByEmployeeId,
                orderHeader.getDeal() != null && orderHeader.getDeal().getOwnerEmp() != null
                        ? orderHeader.getDeal().getOwnerEmp().getId()
                        : null,
                "deal owner employee", "employeeId");
        addResolvedUserId(userIds, userIdByClientId,
                orderHeader.getClient() == null ? null : orderHeader.getClient().getId(),
                "statement client", "clientId");

        return userIds.stream().toList();
    }

    private void addResolvedUserId(
            Set<Long> userIds,
            LinkedHashMap<Long, Long> resolvedUserIds,
            Long sourceId,
            String sourceLabel,
            String idLabel
    ) {
        if (sourceId == null) {
            return;
        }

        Long userId = resolvedUserIds.get(sourceId);
        if (userId == null) {
            log.debug("No user found for {}. {}={}", sourceLabel, idLabel, sourceId);
            return;
        }
        userIds.add(userId);
    }
}
