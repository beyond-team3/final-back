package com.monsoon.seedflowplus.domain.billing.statement.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementListResponse;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementResponse;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
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

    /**
     * 주문 확정(CONFIRMED) 시 명세서 자동 생성
     * OrderService.confirmOrder() 에서 호출
     */
    @Transactional
    public void createStatement(OrderHeader orderHeader) {
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
                Statement statement = Statement.create(orderHeader, orderHeader.getTotalAmount(), code);
                statementRepository.saveAndFlush(statement);  // flush로 즉시 제약 검사
                return;
            } catch (DataIntegrityViolationException e) {
                // statement_code 유니크 충돌만 재시도, 그 외(FK 등)는 즉시 전파
                if (!isStatementCodeViolation(e) || i == maxRetries - 1) throw e;
            }
        }
    }

    public StatementResponse getStatement(Long statementId, CustomUserDetails userDetails) {

        if (userDetails.getClientId() != null) {
            // 거래처 로그인
            Statement statement = statementRepository
                    .findByIdAndClientId(statementId, userDetails.getClientId())
                    .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

            return StatementResponse.from(statement);
        }

        if (userDetails.getEmployeeId() != null) {
            // 영업사원은 조회 허용 (또는 추가 검증)
            Statement statement = statementRepository.findById(statementId)
                    .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));

            return StatementResponse.from(statement);
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
}