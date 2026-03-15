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

    Optional<Statement> findByIdAndOrderHeader_Client_Id(Long statementId, Long clientId);

    List<Statement> findAllByStatus(StatementStatus status);

    boolean existsByStatementCode(String statementCode);

    @Query("SELECT MAX(CAST(SUBSTRING(s.statementCode, LENGTH(:prefix) + 1) AS integer)) " +
            "FROM Statement s WHERE s.statementCode LIKE CONCAT(:prefix, '%')")
    Optional<Integer> findMaxSuffixByPrefix(@Param("prefix") String prefix);
}
