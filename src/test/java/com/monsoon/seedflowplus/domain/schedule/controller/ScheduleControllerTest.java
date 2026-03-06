package com.monsoon.seedflowplus.domain.schedule.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.schedule.command.PersonalScheduleCommandService;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleCreateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.request.PersonalScheduleUpdateRequest;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemDto;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemType;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleVisibility;
import com.monsoon.seedflowplus.domain.schedule.query.ScheduleSearchCondition;
import com.monsoon.seedflowplus.domain.schedule.query.ScheduleQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScheduleController.class)
@Import(TestSecurityConfig.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleQueryService scheduleQueryService;

    @MockBean
    private PersonalScheduleCommandService personalScheduleCommandService;

    @Test
    @DisplayName("개인 일정 생성 API는 요청 본문을 command service로 전달한다")
    void createPersonalSchedule() throws Exception {
        PersonalScheduleCreateRequest request = new PersonalScheduleCreateRequest(
                "방문 일정",
                "설명",
                LocalDateTime.of(2026, 3, 7, 10, 0),
                LocalDateTime.of(2026, 3, 7, 11, 0),
                false,
                ScheduleStatus.ACTIVE,
                ScheduleVisibility.PRIVATE
        );

        when(personalScheduleCommandService.create(any(PersonalScheduleCreateRequest.class), any(CustomUserDetails.class)))
                .thenReturn(101L);

        mockMvc.perform(post("/api/v1/schedules/personal")
                        .with(authentication(auth(adminPrincipal(1L))))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(101L));
    }

    @Test
    @DisplayName("개인 일정 생성 API는 endAt이 startAt과 같으면 400을 반환한다")
    void createPersonalScheduleRejectsEqualTimeRange() throws Exception {
        PersonalScheduleCreateRequest request = new PersonalScheduleCreateRequest(
                "방문 일정",
                "설명",
                LocalDateTime.of(2026, 3, 7, 10, 0),
                LocalDateTime.of(2026, 3, 7, 10, 0),
                false,
                ScheduleStatus.ACTIVE,
                ScheduleVisibility.PRIVATE
        );

        mockMvc.perform(post("/api/v1/schedules/personal")
                        .with(authentication(auth(adminPrincipal(1L))))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("ERROR"));

        verify(personalScheduleCommandService, never())
                .create(any(PersonalScheduleCreateRequest.class), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("개인 일정 단건 조회 API는 내 일정 DTO를 반환한다")
    void getPersonalSchedule() throws Exception {
        ScheduleItemDto dto = ScheduleItemDto.builder()
                .id(11L)
                .type(ScheduleItemType.PERSONAL)
                .title("개인 일정")
                .startAt(LocalDateTime.of(2026, 3, 7, 9, 0))
                .endAt(LocalDateTime.of(2026, 3, 7, 10, 0))
                .status(ScheduleStatus.ACTIVE.name())
                .build();

        when(personalScheduleCommandService.getMySchedule(eq(11L), any(CustomUserDetails.class))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/schedules/personal/11")
                        .with(authentication(auth(adminPrincipal(1L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(11L));
    }

    @Test
    @DisplayName("개인 일정 수정 API는 성공 응답을 반환한다")
    void updatePersonalSchedule() throws Exception {
        PersonalScheduleUpdateRequest request = new PersonalScheduleUpdateRequest(
                "수정 일정",
                "수정 설명",
                LocalDateTime.of(2026, 3, 8, 14, 0),
                LocalDateTime.of(2026, 3, 8, 15, 0),
                false,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/schedules/personal/55")
                        .with(authentication(auth(adminPrincipal(1L))))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(personalScheduleCommandService).update(any(Long.class), any(PersonalScheduleUpdateRequest.class), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("개인 일정 수정 API는 endAt이 startAt과 같으면 400을 반환한다")
    void updatePersonalScheduleRejectsEqualTimeRange() throws Exception {
        PersonalScheduleUpdateRequest request = new PersonalScheduleUpdateRequest(
                "수정 일정",
                "수정 설명",
                LocalDateTime.of(2026, 3, 8, 14, 0),
                LocalDateTime.of(2026, 3, 8, 14, 0),
                false,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/schedules/personal/55")
                        .with(authentication(auth(adminPrincipal(1L))))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("ERROR"));

        verify(personalScheduleCommandService, never())
                .update(any(Long.class), any(PersonalScheduleUpdateRequest.class), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("개인 일정 삭제 API는 성공 응답을 반환한다")
    void deletePersonalSchedule() throws Exception {
        mockMvc.perform(delete("/api/v1/schedules/personal/77")
                        .with(authentication(auth(adminPrincipal(1L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(personalScheduleCommandService).delete(eq(77L), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("통합 조회 API는 ADMIN principal로 조회 조건을 구성한다")
    void unifiedSchedulesForAdmin() throws Exception {
        when(scheduleQueryService.getUnifiedSchedules(any(ScheduleSearchCondition.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schedules")
                        .with(authentication(auth(adminPrincipal(10L))))
                        .param("from", "2026-03-01T00:00:00")
                        .param("to", "2026-03-31T23:59:59")
                        .param("assigneeUserId", "30")
                        .param("clientId", "40")
                        .param("dealId", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        ArgumentCaptor<ScheduleSearchCondition> captor = ArgumentCaptor.forClass(ScheduleSearchCondition.class);
        verify(scheduleQueryService).getUnifiedSchedules(captor.capture());

        ScheduleSearchCondition condition = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(condition.getActorRole()).isEqualTo(Role.ADMIN);
        org.assertj.core.api.Assertions.assertThat(condition.getActorUserId()).isEqualTo(10L);
        org.assertj.core.api.Assertions.assertThat(condition.isIncludePersonal()).isTrue();
        org.assertj.core.api.Assertions.assertThat(condition.isIncludeDeal()).isTrue();
    }

    @Test
    @DisplayName("통합 조회 API는 SALES_REP principal로 조회 조건을 구성한다")
    void unifiedSchedulesForSalesRep() throws Exception {
        when(scheduleQueryService.getUnifiedSchedules(any(ScheduleSearchCondition.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schedules")
                        .with(authentication(auth(salesRepPrincipal(21L, 301L))))
                        .param("from", "2026-03-01T00:00:00")
                        .param("to", "2026-03-31T23:59:59")
                        .param("includePersonal", "false")
                        .param("includeDeal", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<ScheduleSearchCondition> captor = ArgumentCaptor.forClass(ScheduleSearchCondition.class);
        verify(scheduleQueryService).getUnifiedSchedules(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getActorRole()).isEqualTo(Role.SALES_REP);
    }

    @Test
    @DisplayName("통합 조회 API는 CLIENT principal로 조회 조건을 구성한다")
    void unifiedSchedulesForClient() throws Exception {
        when(scheduleQueryService.getUnifiedSchedules(any(ScheduleSearchCondition.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schedules")
                        .with(authentication(auth(clientPrincipal(31L, 901L))))
                        .param("from", "2026-03-01T00:00:00")
                        .param("to", "2026-03-31T23:59:59"))
                .andExpect(status().isOk());

        ArgumentCaptor<ScheduleSearchCondition> captor = ArgumentCaptor.forClass(ScheduleSearchCondition.class);
        verify(scheduleQueryService).getUnifiedSchedules(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getActorRole()).isEqualTo(Role.CLIENT);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getActorClientId()).isEqualTo(901L);
    }

    private Authentication auth(CustomUserDetails principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private CustomUserDetails adminPrincipal(Long userId) {
        User user = User.builder()
                .loginId("admin")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.ADMIN)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails salesRepPrincipal(Long userId, Long employeeId) {
        Employee employee = Employee.builder()
                .employeeCode("E-1")
                .employeeName("sales")
                .employeeEmail("sales@test.com")
                .employeePhone("010")
                .address("seoul")
                .build();
        ReflectionTestUtils.setField(employee, "id", employeeId);

        User user = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return new CustomUserDetails(user);
    }

    private CustomUserDetails clientPrincipal(Long userId, Long clientId) {
        Client client = Client.builder()
                .clientCode("C-1")
                .clientName("client")
                .clientBrn("111-11-11111")
                .ceoName("ceo")
                .companyPhone("02")
                .address("seoul")
                .clientType(com.monsoon.seedflowplus.domain.account.entity.ClientType.DISTRIBUTOR)
                .managerName("m")
                .managerPhone("010")
                .managerEmail("m@test.com")
                .build();
        ReflectionTestUtils.setField(client, "id", clientId);

        User user = User.builder()
                .loginId("client")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.CLIENT)
                .client(client)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return new CustomUserDetails(user);
    }
}
