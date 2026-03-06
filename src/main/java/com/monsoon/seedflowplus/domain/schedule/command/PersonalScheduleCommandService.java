package com.monsoon.seedflowplus.domain.schedule.command;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleCreateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleUpdateRequest;
import com.monsoon.seedflowplus.domain.schedule.entity.PersonalSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleVisibility;
import com.monsoon.seedflowplus.domain.schedule.repository.PersonalScheduleRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalScheduleCommandService {

    private final PersonalScheduleRepository personalScheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long create(PersonalScheduleCreateRequest request, CustomUserDetails actor) {
        Long actorUserId = requireActorUserId(actor);
        validateRange(request.startAt(), request.endAt());

        User owner = userRepository.findById(actorUserId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        PersonalSchedule schedule = PersonalSchedule.builder()
                .owner(owner)
                .title(request.title())
                .description(request.description())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .allDay(request.allDay())
                .status(resolveStatusForCreate(request.status()))
                .visibility(resolveVisibilityForCreate(request.visibility()))
                .build();

        return personalScheduleRepository.save(schedule).getId();
    }

    @Transactional
    public void update(Long scheduleId, PersonalScheduleUpdateRequest request, CustomUserDetails actor) {
        Long actorUserId = requireActorUserId(actor);
        validateRange(request.startAt(), request.endAt());

        PersonalSchedule schedule = getOwnedScheduleOrThrow(scheduleId, actorUserId);
        schedule.update(
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.allDay(),
                resolveStatusForUpdate(request.status(), schedule.getStatus()),
                resolveVisibilityForUpdate(request.visibility(), schedule.getVisibility())
        );
    }

    @Transactional
    public void delete(Long scheduleId, CustomUserDetails actor) {
        Long actorUserId = requireActorUserId(actor);
        PersonalSchedule schedule = getOwnedScheduleOrThrow(scheduleId, actorUserId);
        schedule.cancel();
    }

    private PersonalSchedule getOwnedScheduleOrThrow(Long scheduleId, Long actorUserId) {
        return personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(scheduleId, actorUserId, ScheduleStatus.CANCELED)
                .orElseThrow(() -> new CoreException(ErrorType.PERSONAL_SCHEDULE_NOT_FOUND));
    }

    private Long requireActorUserId(CustomUserDetails actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return actor.getUserId();
    }

    private void validateRange(java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }

    private ScheduleStatus resolveStatusForCreate(ScheduleStatus requestedStatus) {
        ScheduleStatus resolved = requestedStatus == null ? ScheduleStatus.ACTIVE : requestedStatus;
        if (resolved == ScheduleStatus.DONE) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private ScheduleStatus resolveStatusForUpdate(ScheduleStatus requestedStatus, ScheduleStatus currentStatus) {
        ScheduleStatus resolved = requestedStatus == null ? currentStatus : requestedStatus;
        if (resolved == ScheduleStatus.DONE) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private ScheduleVisibility resolveVisibilityForCreate(ScheduleVisibility requestedVisibility) {
        ScheduleVisibility resolved = requestedVisibility == null ? ScheduleVisibility.PRIVATE : requestedVisibility;
        if (resolved == ScheduleVisibility.TEAM) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private ScheduleVisibility resolveVisibilityForUpdate(
            ScheduleVisibility requestedVisibility,
            ScheduleVisibility currentVisibility
    ) {
        ScheduleVisibility resolved = requestedVisibility == null ? currentVisibility : requestedVisibility;
        if (resolved == ScheduleVisibility.TEAM) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        return resolved;
    }
}
