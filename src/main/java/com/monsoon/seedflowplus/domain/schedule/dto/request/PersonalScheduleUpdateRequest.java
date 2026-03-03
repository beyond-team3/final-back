package com.monsoon.seedflowplus.domain.schedule.dto.request;

import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record PersonalScheduleUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        boolean allDay,
        ScheduleStatus status,
        ScheduleVisibility visibility
) {
}
