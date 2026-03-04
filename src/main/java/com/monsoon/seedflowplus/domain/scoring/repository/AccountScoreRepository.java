package com.monsoon.seedflowplus.domain.scoring.repository;

import com.monsoon.seedflowplus.domain.scoring.entity.AccountScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountScoreRepository extends JpaRepository<AccountScore, Long> {
    Optional<AccountScore> findByClient_Id(Long clientId);
}
