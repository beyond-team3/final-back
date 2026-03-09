package com.monsoon.seedflowplus.domain.schedule.query;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemDto;
import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.PersonalSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.repository.DealScheduleRepository;
import com.monsoon.seedflowplus.domain.schedule.repository.PersonalScheduleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private final PersonalScheduleRepository personalScheduleRepository;
    private final DealScheduleRepository dealScheduleRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public ScheduleItemDto getMySchedule(Long scheduleId, Long actorUserId) {
        if (scheduleId == null || actorUserId == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        PersonalSchedule schedule = personalScheduleRepository
                .findByIdAndOwnerIdAndStatusNot(scheduleId, actorUserId, ScheduleStatus.CANCELED)
                .orElseThrow(() -> new CoreException(ErrorType.PERSONAL_SCHEDULE_NOT_FOUND));
        return ScheduleItemDto.fromPersonal(schedule);
    }

    public List<ScheduleItemDto> getUnifiedSchedules(ScheduleSearchCondition condition) {
        if (condition == null || condition.getActorUserId() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (!condition.isIncludePersonal() && !condition.isIncludeDeal()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        validateRange(condition.getRangeStart(), condition.getRangeEnd());

        User actor = userRepository.findById(condition.getActorUserId())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        Role actorRole = resolveActorRole(condition.getActorRole(), actor);
        Long actorUserId = actor.getId();

        if (condition.getOwnerId() != null && !condition.getOwnerId().equals(actorUserId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        List<PersonalSchedule> personalSchedules = condition.isIncludePersonal()
                ? personalScheduleRepository.findByOwnerIdAndStatusNotAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                        actorUserId,
                        ScheduleStatus.CANCELED,
                        condition.getRangeEnd(),
                        condition.getRangeStart()
                )
                : List.of();

        List<DealSchedule> dealSchedules = condition.isIncludeDeal()
                ? findDealSchedulesByRole(condition, actorRole, actor)
                : List.of();

        List<ScheduleItemDto> result = new ArrayList<>(personalSchedules.size() + dealSchedules.size());
        personalSchedules.stream().map(ScheduleItemDto::fromPersonal).forEach(result::add);
        dealSchedules.stream().map(ScheduleItemDto::fromDeal).forEach(result::add);

        result.sort(Comparator.comparing(ScheduleItemDto::getStartAt)
                .thenComparing(ScheduleItemDto::getId));
        return result;
    }

    private List<DealSchedule> findDealSchedulesByRole(ScheduleSearchCondition condition, Role actorRole, User actor) {
        if (actorRole == Role.ADMIN) {
            return findForAdmin(condition);
        }
        if (actorRole == Role.SALES_REP) {
            return findForSalesRep(condition, actor);
        }
        if (actorRole == Role.CLIENT) {
            return findForClient(condition, actor);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private List<DealSchedule> findForAdmin(ScheduleSearchCondition condition) {
        Long assigneeUserId = condition.getAssigneeUserId();
        Long clientId = condition.getClientId();
        Long dealId = condition.getDealId();

        if (assigneeUserId != null && clientId != null && dealId != null) {
            return dealScheduleRepository
                    .findByAssigneeUserIdAndClientIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            assigneeUserId, clientId, dealId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (assigneeUserId != null && clientId != null) {
            return dealScheduleRepository
                    .findByAssigneeUserIdAndClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            assigneeUserId, clientId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (assigneeUserId != null && dealId != null) {
            return dealScheduleRepository
                    .findByAssigneeUserIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            assigneeUserId, dealId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (clientId != null && dealId != null) {
            return dealScheduleRepository
                    .findByClientIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            clientId, dealId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (assigneeUserId != null) {
            return dealScheduleRepository
                    .findByAssigneeUserIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            assigneeUserId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (clientId != null) {
            return dealScheduleRepository
                    .findByClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            clientId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (dealId != null) {
            return dealScheduleRepository
                    .findByDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            dealId, condition.getRangeEnd(), condition.getRangeStart());
        }
        return dealScheduleRepository
                .findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                        condition.getRangeEnd(), condition.getRangeStart());
    }

    private List<DealSchedule> findForSalesRep(ScheduleSearchCondition condition, User actor) {
        if (actor.getEmployee() == null || actor.getEmployee().getId() == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }
        if (condition.getAssigneeUserId() != null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        Long managerEmployeeId = actor.getEmployee().getId();

        Long clientId = condition.getClientId();
        Long dealId = condition.getDealId();

        if (clientId != null && !clientRepository.existsByIdAndManagerEmployeeId(clientId, managerEmployeeId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        if (dealId != null) {
            if (clientId != null) {
                return dealScheduleRepository
                        .findByClientIdAndDealIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                                clientId, dealId, condition.getRangeEnd(), condition.getRangeStart());
            }
            return dealScheduleRepository
                    .findByDealIdAndClientManagerEmployeeIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            dealId, managerEmployeeId, condition.getRangeEnd(), condition.getRangeStart());
        }
        if (clientId != null) {
            return dealScheduleRepository
                    .findByClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                            clientId, condition.getRangeEnd(), condition.getRangeStart());
        }
        return dealScheduleRepository
                .findByClientManagerEmployeeIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                        managerEmployeeId, condition.getRangeEnd(), condition.getRangeStart());
    }

    private List<DealSchedule> findForClient(ScheduleSearchCondition condition, User actor) {
        if (condition.getAssigneeUserId() != null || condition.getClientId() != null || condition.getDealId() != null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        Long actorClientId = actor.getClient() == null ? null : actor.getClient().getId();

        if (condition.getActorClientId() != null && !condition.getActorClientId().equals(actorClientId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        if (actorClientId == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        return dealScheduleRepository
                .findByClientIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAscIdAsc(
                        actorClientId, condition.getRangeEnd(), condition.getRangeStart());
    }

    private Role resolveActorRole(Role requestedRole, User actor) {
        if (requestedRole == null) {
            return actor.getRole();
        }
        if (actor.getRole() != requestedRole) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        return requestedRole;
    }

    private void validateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null || rangeEnd == null || !rangeStart.isBefore(rangeEnd)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }
}
