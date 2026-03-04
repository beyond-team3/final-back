package com.monsoon.seedflowplus.domain.schedule.repository;

import com.monsoon.seedflowplus.domain.schedule.entity.PersonalSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalScheduleRepository extends JpaRepository<PersonalSchedule, Long> {

    @EntityGraph(attributePaths = {"owner"})
    Optional<PersonalSchedule> findByIdAndOwnerIdAndStatusNot(Long id, Long ownerId, ScheduleStatus status);

    @EntityGraph(attributePaths = {"owner"})
    List<PersonalSchedule> findByOwnerIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long ownerId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"owner"})
    List<PersonalSchedule> findByOwnerIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long ownerId,
            ScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );
}
