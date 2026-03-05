package com.monsoon.seedflowplus.domain.scoring.repository;

import com.monsoon.seedflowplus.domain.scoring.entity.AccountScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccountScoreRepository extends JpaRepository<AccountScore, Long> {
    Optional<AccountScore> findByClient_Id(Long clientId);

    @Modifying
    @Query(value = "INSERT INTO tbl_clientscore (client_id, total_score, contract_score, order_score, visit_score, primary_reason, detail_description, created_at, updated_at) " +
            "VALUES (:clientId, :total, :contract, :order, :visit, :reason, :detail, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_score = :total, contract_score = :contract, order_score = :order, visit_score = :visit, " +
            "primary_reason = :reason, detail_description = :detail, updated_at = NOW()", nativeQuery = true)
    void upsert(@Param("clientId") Long clientId,
                @Param("total") double total,
                @Param("contract") double contract,
                @Param("order") double order,
                @Param("visit") double visit,
                @Param("reason") String reason,
                @Param("detail") String detail);
}
