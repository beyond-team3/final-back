package com.monsoon.seedflowplus.domain.schedule.repository;

import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealScheduleRepository extends JpaRepository<DealSchedule, Long> {

    Optional<DealSchedule> findByExternalKey(String externalKey);

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByClientIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long clientId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByDealIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long dealId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndClientIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long clientId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndDealIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long dealId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByClientIdAndDealIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long clientId,
            Long dealId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndClientIdAndDealIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long clientId,
            Long dealId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByClientManagerEmployeeIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long managerEmployeeId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByDealIdAndClientManagerEmployeeIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long dealId,
            Long managerEmployeeId,
            DealScheduleStatus status,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );
}
