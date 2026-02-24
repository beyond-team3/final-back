package com.monsoon.seedflowplus.domain.note.repository;


import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesNoteRepository extends JpaRepository<SalesNote, Long>, SalesNoteRepositoryCustom {

    // 특정 고객의 노트를 최신순으로 조회 (NoteBriefingView.vue의 분석 근거 데이터 확보용)
    @Query("SELECT n FROM SalesNote n WHERE n.clientId = :clientId ORDER BY n.activityDate DESC, n.id DESC")
    List<SalesNote> findTop3ByClientIdOrderByActivityDateDesc(@Param("clientId") Long clientId);

    // 특정 고객의 전체 노트 개수 조회 (3개 이상인지 확인하는 비즈니스 로직용)
    long countByClientId(Long clientId);
}