package com.monsoon.seedflowplus.domain.schedule.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DealScheduleUpsertCommandTest {

    @Test
    @DisplayName("canonical constructor는 title/externalKey를 trim 후 저장한다")
    void trimsTitleAndExternalKey() {
        DealScheduleUpsertCommand command = new DealScheduleUpsertCommand(
                "  ext-key  ",
                1L,
                2L,
                3L,
                DealScheduleEventType.DOC_SUBMITTED,
                DealDocType.QUO,
                10L,
                20L,
                "  방문 일정  ",
                "설명",
                LocalDateTime.of(2026, 3, 12, 10, 0),
                LocalDateTime.of(2026, 3, 12, 11, 0),
                LocalDateTime.of(2026, 3, 12, 9, 0)
        );

        assertThat(command.externalKey()).isEqualTo("ext-key");
        assertThat(command.title()).isEqualTo("방문 일정");
    }

    @Test
    @DisplayName("externalKey가 trim 후 공백이면 INVALID_INPUT_VALUE")
    void rejectsBlankExternalKeyAfterTrim() {
        assertThatThrownBy(() -> new DealScheduleUpsertCommand(
                "   ",
                1L,
                2L,
                3L,
                DealScheduleEventType.DOC_SUBMITTED,
                DealDocType.QUO,
                10L,
                20L,
                "방문 일정",
                "설명",
                LocalDateTime.of(2026, 3, 12, 10, 0),
                LocalDateTime.of(2026, 3, 12, 11, 0),
                LocalDateTime.of(2026, 3, 12, 9, 0)
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);
    }
}
