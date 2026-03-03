package com.monsoon.seedflowplus.domain.schedule.repository;

import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealScheduleRepository extends JpaRepository<DealSchedule, Long> {

    Optional<DealSchedule> findByExternalKey(String externalKey);

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long clientId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long dealId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long clientId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long dealId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByClientIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long clientId,
            Long dealId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndClientIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long clientId,
            Long dealId,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByClientIdInAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            List<Long> clientIds,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndClientIdInAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            List<Long> clientIds,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByDealIdAndClientIdInAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long dealId,
            List<Long> clientIds,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );

    @EntityGraph(attributePaths = {"assigneeUser", "client", "deal"})
    List<DealSchedule> findByAssigneeUserIdAndDealIdAndClientIdInAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
            Long assigneeUserId,
            Long dealId,
            List<Long> clientIds,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );
}
