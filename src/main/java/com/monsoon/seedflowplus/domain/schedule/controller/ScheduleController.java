package com.monsoon.seedflowplus.domain.schedule.controller;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleCreateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleUpdateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.request.ScheduleSearchCondition;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemDto;
import com.monsoon.seedflowplus.domain.schedule.command.PersonalScheduleCommandService;
import com.monsoon.seedflowplus.domain.schedule.query.ScheduleQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleQueryService scheduleQueryService;
    private final PersonalScheduleCommandService personalScheduleCommandService;

    @GetMapping
    public ApiResult<List<ScheduleItemDto>> getSchedules(
            @RequestParam("from") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long assigneeUserId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long dealId,
            @RequestParam(defaultValue = "true") boolean includePersonal,
            @RequestParam(defaultValue = "true") boolean includeDeal,
            @AuthenticationPrincipal CustomUserDetails actor
    ) {
        CustomUserDetails requiredActor = requireActor(actor);

        ScheduleSearchCondition condition = ScheduleSearchCondition.builder()
                .rangeStart(from)
                .rangeEnd(to)
                .ownerId(requiredActor.getUserId())
                .assigneeUserId(assigneeUserId)
                .clientId(clientId)
                .dealId(dealId)
                .actorRole(requiredActor.getRole())
                .actorUserId(requiredActor.getUserId())
                .actorClientId(requiredActor.getClientId())
                .includePersonal(includePersonal)
                .includeDeal(includeDeal)
                .build();

        return ApiResult.success(scheduleQueryService.getUnifiedSchedules(condition));
    }

    @PostMapping("/personal")
    public ApiResult<Long> createPersonalSchedule(
            @RequestBody @Valid PersonalScheduleCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails actor
    ) {
        return ApiResult.success(personalScheduleCommandService.create(request, requireActor(actor)));
    }

    @GetMapping("/personal/{id}")
    public ApiResult<ScheduleItemDto> getPersonalSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails actor
    ) {
        return ApiResult.success(personalScheduleCommandService.getMySchedule(id, requireActor(actor)));
    }

    @PutMapping("/personal/{id}")
    public ApiResult<?> updatePersonalSchedule(
            @PathVariable Long id,
            @RequestBody @Valid PersonalScheduleUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails actor
    ) {
        personalScheduleCommandService.update(id, request, requireActor(actor));
        return ApiResult.success();
    }

    @DeleteMapping("/personal/{id}")
    public ApiResult<?> deletePersonalSchedule(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails actor
    ) {
        personalScheduleCommandService.delete(id, requireActor(actor));
        return ApiResult.success();
    }

    private CustomUserDetails requireActor(CustomUserDetails actor) {
        if (actor == null || actor.getUserId() == null || actor.getRole() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return actor;
    }
}
