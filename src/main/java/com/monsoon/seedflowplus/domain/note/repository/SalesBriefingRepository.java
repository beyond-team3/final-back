package com.monsoon.seedflowplus.domain.note.repository;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SalesBriefingRepository extends JpaRepository<SalesBriefing, Long> {

    // 고객사 ID로 최신 브리핑 결과 조회 (NoteBriefingView.vue 연동)
    Optional<SalesBriefing> findByClientId(Long clientId);

    // 고객사별 브리핑 존재 여부 확인
    boolean existsByClientId(Long clientId);
}