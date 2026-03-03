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

    Optional<Statement> findByIdAndClientId(Long statementId, Long clientId);

    List<Statement> findAllByStatus(StatementStatus status);

    // 코드 채번용
    boolean existsByStatementCode(String statementCode);

    // 오늘 날짜 prefix 기준 suffix 최대값 조회 (숫자 기준 MAX → 999 초과 문제 없음)
    @Query("SELECT MAX(CAST(SUBSTRING(s.statementCode, LENGTH(:prefix) + 1) AS integer)) " +
            "FROM Statement s WHERE s.statementCode LIKE CONCAT(:prefix, '%')")
    Optional<Integer> findMaxSuffixByPrefix(@Param("prefix") String prefix);
}
