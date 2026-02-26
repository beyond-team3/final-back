package com.monsoon.seedflowplus.domain.billing.statement.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementListResponse;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementResponse;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.repository.StatementRepository;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import lombok.RequiredArgsConstructor;
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

        String code = generateCode("STMT");
        Statement statement = Statement.create(orderHeader, orderHeader.getTotalAmount(), code);
        statementRepository.save(statement);
    }

    /**
     * 명세서 단건 조회
     */
    public StatementResponse getStatement(Long statementId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));
        return StatementResponse.from(statement);
    }

    /**
     * 명세서 목록 조회
     */
    public List<StatementListResponse> getStatements() {
        return statementRepository.findAll().stream()
                .map(StatementListResponse::from)
                .toList();
    }

    // STMT-20260223-001 형식으로 코드 생성 (OrderService와 동일한 패턴)
    private String generateCode(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayPrefix = prefix + "-" + date + "-";

        boolean exists = statementRepository.existsByStatementCode(todayPrefix + "001");
        if (!exists) return todayPrefix + "001";

        long count = statementRepository.countByStatementCodeStartingWith(todayPrefix);
        return todayPrefix + String.format("%03d", count + 1);
    }
}