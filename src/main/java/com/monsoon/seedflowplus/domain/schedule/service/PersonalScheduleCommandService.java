package com.monsoon.seedflowplus.domain.schedule.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleCreateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleUpdateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemDto;
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
                .status(request.status() == null ? ScheduleStatus.ACTIVE : request.status())
                .visibility(request.visibility() == null ? ScheduleVisibility.PRIVATE : request.visibility())
                .build();

        return personalScheduleRepository.save(schedule).getId();
    }

    public ScheduleItemDto getMySchedule(Long scheduleId, CustomUserDetails actor) {
        PersonalSchedule schedule = getOwnedScheduleOrThrow(scheduleId, requireActorUserId(actor));
        return ScheduleItemDto.fromPersonal(schedule);
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
                request.status() == null ? schedule.getStatus() : request.status(),
                request.visibility() == null ? schedule.getVisibility() : request.visibility()
        );
    }

    @Transactional
    public void delete(Long scheduleId, CustomUserDetails actor) {
        Long actorUserId = requireActorUserId(actor);
        PersonalSchedule schedule = getOwnedScheduleOrThrow(scheduleId, actorUserId);
        schedule.update(
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isAllDay(),
                ScheduleStatus.CANCELED,
                schedule.getVisibility()
        );
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
}
