package com.monsoon.seedflowplus.domain.account.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.dto.request.UserStatusUpdateRequest;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientCropRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.notification.event.AccountActivatedEvent;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.infra.kakao.GeocodingService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ClientCropRepository clientCropRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisTokenStore tokenStore;
    @Mock private GeocodingService geocodingService;
    @Mock private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("DEACTIVATE에서 ACTIVATE로 전환될 때만 계정 활성화 알림 이벤트를 발행한다")
    void updateUserStatusPublishesEventOnlyOnDeactivateToActivate() {
        User user = user(10L, Status.DEACTIVATE, Role.CLIENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        accountService.updateUserStatus(new UserStatusUpdateRequest(10L, Status.ACTIVATE));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        AccountActivatedEvent event = (AccountActivatedEvent) eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.userId()).isEqualTo(10L);
        org.assertj.core.api.Assertions.assertThat(event.role()).isEqualTo(Role.CLIENT);
    }

    @Test
    @DisplayName("이미 활성 상태이거나 비활성으로 전환하는 경우 계정 활성화 알림을 발행하지 않는다")
    void updateUserStatusDoesNotPublishEventForOtherTransitions() {
        User activeUser = user(11L, Status.ACTIVATE, Role.SALES_REP);
        when(userRepository.findById(11L)).thenReturn(Optional.of(activeUser));

        accountService.updateUserStatus(new UserStatusUpdateRequest(11L, Status.ACTIVATE));

        verify(notificationEventPublisher, never()).publishAfterCommit(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 상태 변경은 예외를 던진다")
    void updateUserStatusThrowsWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateUserStatus(new UserStatusUpdateRequest(99L, Status.ACTIVATE)))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    private User user(Long id, Status status, Role role) {
        User user = User.builder()
                .loginId("user-" + id)
                .loginPw("pw")
                .status(status)
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
