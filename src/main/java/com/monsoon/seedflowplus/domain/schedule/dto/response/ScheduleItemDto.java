package com.monsoon.seedflowplus.domain.schedule.dto.response;

import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.PersonalSchedule;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleItemDto {

    private Long id;
    private ScheduleItemType type;

    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean allDay;
    private String status;

    private Long ownerUserId;
    private Long assigneeUserId;
    private Long clientId;
    private Long dealId;

    private String eventType;
    private String docType;

    public static ScheduleItemDto fromPersonal(PersonalSchedule schedule) {
        return ScheduleItemDto.builder()
                .id(schedule.getId())
                .type(ScheduleItemType.PERSONAL)
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startAt(schedule.getStartAt())
                .endAt(schedule.getEndAt())
                .allDay(schedule.isAllDay())
                .status(schedule.getStatus().name())
                .ownerUserId(schedule.getOwner().getId())
                .build();
    }

    public static ScheduleItemDto fromDeal(DealSchedule schedule) {
        return ScheduleItemDto.builder()
                .id(schedule.getId())
                .type(ScheduleItemType.DEAL)
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startAt(schedule.getStartAt())
                .endAt(schedule.getEndAt())
                .allDay(false)
                .status(schedule.getStatus().name())
                .assigneeUserId(schedule.getAssigneeUser().getId())
                .clientId(schedule.getClient().getId())
                .dealId(schedule.getDeal().getId())
                .eventType(schedule.getEventType().name())
                .docType(schedule.getDocType().name())
                .build();
    }
}
