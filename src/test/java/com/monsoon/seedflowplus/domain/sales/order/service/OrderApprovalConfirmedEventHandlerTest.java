package com.monsoon.seedflowplus.domain.sales.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.service.OrderApprovalConfirmedEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderApprovalConfirmedEventHandlerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderApprovalConfirmedEventHandler handler;

    @Test
    void handleShouldConfirmOrderFromApproval() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("담당")
                .employeeEmail("emp@test.com")
                .employeePhone("010-0000-0000")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(employee, "id", 10L);

        User user = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .build();
        ReflectionTestUtils.setField(user, "id", 501L);

        when(userRepository.findById(501L)).thenReturn(Optional.of(user));

        handler.handle(new OrderApprovalConfirmedEvent(700L, 501L, LocalDateTime.now()));

        verify(orderService).confirmOrderFromApproval(eq(700L), any());
    }
}
