package com.monsoon.seedflowplus.domain.schedule.query;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ScheduleSearchCondition {

    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;

    private Long ownerId;
    private Long assigneeUserId;
    private Long clientId;
    private Long dealId;

    private Role actorRole;
    private Long actorUserId;
    private Long actorClientId;

    private boolean includePersonal;
    private boolean includeDeal;
}
