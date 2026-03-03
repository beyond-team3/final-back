package com.monsoon.seedflowplus.domain.note.repository;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import java.util.Map;
import java.util.stream.Collectors;

public interface SalesNoteRepository extends JpaRepository<SalesNote, Long>, SalesNoteRepositoryCustom {

    // 거래처별 최근 방문일 일괄 조회
    @Query("SELECT s.clientId, MAX(s.activityDate) FROM SalesNote s GROUP BY s.clientId")
    List<Object[]> findAllLastActivityDatesRaw();

    default Map<Long, LocalDate> findAllLastActivityDates() {
        return findAllLastActivityDatesRaw().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (LocalDate) row[1]
                ));
    }

    // 최신 방문일 조회 (스코어링용)
    Optional<SalesNote> findTopByClientIdOrderByActivityDateDesc(Long clientId);

    // 특정 고객의 노트를 최신순으로 조회 (NoteBriefingView.vue의 분석 근거 데이터 확보용)
    List<SalesNote> findTop3ByClientIdOrderByActivityDateDescIdDesc(Long clientId);
    // 특정 고객의 전체 노트 개수 조회 (3개 이상인지 확인하는 비즈니스 로직용)
    long countByClientId(Long clientId);
}