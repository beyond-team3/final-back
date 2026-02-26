package com.monsoon.seedflowplus.domain.billing.statement.repository;

import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    Optional<Statement> findByOrderHeader_Id(Long orderId);

    List<Statement> findAllByStatus(StatementStatus status);

    // 코드 채번용
    boolean existsByStatementCode(String statementCode);

    Optional<Statement> findTopByStatementCodeStartingWithOrderByStatementCodeDesc(String prefix);
}
