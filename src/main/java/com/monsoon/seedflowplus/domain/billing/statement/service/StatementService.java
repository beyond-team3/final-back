package com.monsoon.seedflowplus.domain.billing.statement.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementListResponse;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementResponse;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.billing.statement.repository.StatementRepository;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatementService {

    private final StatementRepository statementRepository;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogQueryService dealLogQueryService;

    /**
     * 주문 확정(CONFIRMED) 시 명세서 자동 생성
     * OrderService.confirmOrder() 에서 호출
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
                String code = generateCode("STMT");
                Statement statement = Statement.create(orderHeader, orderHeader.getDeal(), orderHeader.getTotalAmount(), code);
                statementRepository.saveAndFlush(statement);  // flush로 즉시 제약 검사

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
                                new DealLogWriteService.DiffField("totalAmount", "명세서 금액", null, statement.getTotalAmount(), "MONEY"),
                                new DealLogWriteService.DiffField("orderCode", "주문 번호", null, orderHeader.getOrderCode(), "REFERENCE")
                        )
                );
                return;
            } catch (DataIntegrityViolationException e) {
                // statement_code 유니크 충돌만 재시도, 그 외(FK 등)는 즉시 전파
                if (!isStatementCodeViolation(e) || i == maxRetries - 1) throw e;
            }
        }
    }

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

        return StatementResponse.from(statement, recentLogs(statement));
    }

    public StatementResponse getStatement(Long statementId, CustomUserDetails userDetails) {

        if (userDetails == null)
            throw new CoreException(ErrorType.ACCESS_DENIED);

        if (userDetails.getClientId() != null) {
            // 거래처 로그인
            Statement statement = statementRepository
                    .findByIdAndOrderHeader_Client_Id(statementId, userDetails.getClientId())
                    .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

            return StatementResponse.from(statement, recentLogs(statement));
        }

        if (userDetails.getEmployeeId() != null) {
            // 영업사원은 조회 허용 (또는 추가 검증)
            Statement statement = statementRepository.findById(statementId)
                    .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

            return StatementResponse.from(statement, recentLogs(statement));
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
        if (actorType == ActorType.CLIENT) {
            return principal.getClientId();
        }
        return principal.getEmployeeId();
    }

    private List<com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto> recentLogs(Statement statement) {
        return dealLogQueryService.getRecentDocumentLogs(
                statement.getDeal() != null ? statement.getDeal().getId() : null,
                DealType.STMT,
                statement.getId()
        );
    }
}
