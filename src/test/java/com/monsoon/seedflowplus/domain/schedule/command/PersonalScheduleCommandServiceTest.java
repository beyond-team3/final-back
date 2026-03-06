package com.monsoon.seedflowplus.domain.schedule.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleCreateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleUpdateRequest;
import com.monsoon.seedflowplus.domain.schedule.entity.PersonalSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleVisibility;
import com.monsoon.seedflowplus.domain.schedule.repository.PersonalScheduleRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PersonalScheduleCommandServiceTest {

    @Mock
    private PersonalScheduleRepository personalScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PersonalScheduleCommandService personalScheduleCommandService;

    @Test
    @DisplayName("create는 status/visibility 미지정 시 ACTIVE/PRIVATE로 저장한다")
    void createDefaultsStatusAndVisibility() {
        CustomUserDetails actor = actor(1L);
        User owner = user(1L);
        PersonalScheduleCreateRequest request = new PersonalScheduleCreateRequest(
                "제목",
                "설명",
                LocalDateTime.of(2026, 3, 6, 10, 0),
                LocalDateTime.of(2026, 3, 6, 11, 0),
                false,
                null,
                null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(personalScheduleRepository.save(any(PersonalSchedule.class))).thenAnswer(invocation -> {
            PersonalSchedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 100L);
            return schedule;
        });

        Long createdId = personalScheduleCommandService.create(request, actor);

        ArgumentCaptor<PersonalSchedule> captor = ArgumentCaptor.forClass(PersonalSchedule.class);
        org.mockito.Mockito.verify(personalScheduleRepository).save(captor.capture());
        PersonalSchedule saved = captor.getValue();

        assertThat(createdId).isEqualTo(100L);
        assertThat(saved.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
        assertThat(saved.getVisibility()).isEqualTo(ScheduleVisibility.PRIVATE);
    }

    @Test
    @DisplayName("create는 owner를 찾지 못하면 USER_NOT_FOUND")
    void createThrowsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalScheduleCommandService.create(
                new PersonalScheduleCreateRequest(
                        "제목",
                        null,
                        LocalDateTime.of(2026, 3, 6, 10, 0),
                        LocalDateTime.of(2026, 3, 6, 11, 0),
                        false,
                        ScheduleStatus.ACTIVE,
                        ScheduleVisibility.PRIVATE
                ),
                actor(1L)
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("create는 시간 범위가 잘못되면 INVALID_INPUT_VALUE")
    void createThrowsWhenRangeInvalid() {
        assertThatThrownBy(() -> personalScheduleCommandService.create(
                new PersonalScheduleCreateRequest(
                        "제목",
                        null,
                        LocalDateTime.of(2026, 3, 6, 11, 0),
                        LocalDateTime.of(2026, 3, 6, 10, 0),
                        false,
                        ScheduleStatus.ACTIVE,
                        ScheduleVisibility.PRIVATE
                ),
                actor(1L)
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("update는 status/visibility null이면 기존 값을 유지한다")
    void updateKeepsCurrentOptionalFields() {
        PersonalSchedule existing = PersonalSchedule.builder()
                .owner(user(1L))
                .title("기존")
                .description("기존설명")
                .startAt(LocalDateTime.of(2026, 3, 6, 9, 0))
                .endAt(LocalDateTime.of(2026, 3, 6, 10, 0))
                .allDay(false)
                .status(ScheduleStatus.ACTIVE)
                .visibility(ScheduleVisibility.PRIVATE)
                .build();
        ReflectionTestUtils.setField(existing, "id", 20L);

        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(20L, 1L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.of(existing));

        personalScheduleCommandService.update(
                20L,
                new PersonalScheduleUpdateRequest(
                        "수정",
                        "수정설명",
                        LocalDateTime.of(2026, 3, 6, 12, 0),
                        LocalDateTime.of(2026, 3, 6, 13, 0),
                        true,
                        null,
                        null
                ),
                actor(1L)
        );

        assertThat(existing.getTitle()).isEqualTo("수정");
        assertThat(existing.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
        assertThat(existing.getVisibility()).isEqualTo(ScheduleVisibility.PRIVATE);
    }

    @Test
    @DisplayName("delete는 물리 삭제 없이 status를 CANCELED로 변경한다")
    void deleteSoftCancelsSchedule() {
        PersonalSchedule existing = PersonalSchedule.builder()
                .owner(user(1L))
                .title("기존")
                .description("기존설명")
                .startAt(LocalDateTime.of(2026, 3, 6, 9, 0))
                .endAt(LocalDateTime.of(2026, 3, 6, 10, 0))
                .allDay(false)
                .status(ScheduleStatus.ACTIVE)
                .visibility(ScheduleVisibility.PRIVATE)
                .build();
        ReflectionTestUtils.setField(existing, "id", 30L);

        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(30L, 1L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.of(existing));

        personalScheduleCommandService.delete(30L, actor(1L));

        assertThat(existing.getStatus()).isEqualTo(ScheduleStatus.CANCELED);
        assertThat(ReflectionTestUtils.getField(existing, "isDeleted")).isEqualTo(true);
    }

    @Test
    @DisplayName("update는 본인 일정이 없으면 PERSONAL_SCHEDULE_NOT_FOUND")
    void updateThrowsWhenScheduleNotFound() {
        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(404L, 1L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalScheduleCommandService.update(
                404L,
                new PersonalScheduleUpdateRequest(
                        "수정",
                        "수정설명",
                        LocalDateTime.of(2026, 3, 6, 12, 0),
                        LocalDateTime.of(2026, 3, 6, 13, 0),
                        false,
                        null,
                        null
                ),
                actor(1L)
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.PERSONAL_SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("create는 legacy 상태 DONE 요청을 거부한다")
    void createRejectsLegacyDoneStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> personalScheduleCommandService.create(
                new PersonalScheduleCreateRequest(
                        "제목",
                        null,
                        LocalDateTime.of(2026, 3, 6, 10, 0),
                        LocalDateTime.of(2026, 3, 6, 11, 0),
                        false,
                        ScheduleStatus.DONE,
                        ScheduleVisibility.PRIVATE
                ),
                actor(1L)
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("update는 legacy 가시성 TEAM 요청을 거부한다")
    void updateRejectsLegacyTeamVisibility() {
        PersonalSchedule existing = PersonalSchedule.builder()
                .owner(user(1L))
                .title("기존")
                .description("기존설명")
                .startAt(LocalDateTime.of(2026, 3, 6, 9, 0))
                .endAt(LocalDateTime.of(2026, 3, 6, 10, 0))
                .allDay(false)
                .status(ScheduleStatus.ACTIVE)
                .visibility(ScheduleVisibility.PRIVATE)
                .build();
        ReflectionTestUtils.setField(existing, "id", 30L);

        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(30L, 1L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> personalScheduleCommandService.update(
                30L,
                new PersonalScheduleUpdateRequest(
                        "수정",
                        "수정설명",
                        LocalDateTime.of(2026, 3, 6, 12, 0),
                        LocalDateTime.of(2026, 3, 6, 13, 0),
                        false,
                        ScheduleStatus.ACTIVE,
                        ScheduleVisibility.TEAM
                ),
                actor(1L)
        )).isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("delete는 본인 일정이 없으면 PERSONAL_SCHEDULE_NOT_FOUND")
    void deleteThrowsWhenScheduleNotFound() {
        when(personalScheduleRepository.findByIdAndOwnerIdAndStatusNot(404L, 1L, ScheduleStatus.CANCELED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalScheduleCommandService.delete(404L, actor(1L)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.PERSONAL_SCHEDULE_NOT_FOUND);
    }

    private CustomUserDetails actor(Long userId) {
        User user = user(userId);
        return new CustomUserDetails(user);
    }

    private User user(Long id) {
        User user = User.builder()
                .loginId("u" + id)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.ADMIN)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
