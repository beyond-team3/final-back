package com.monsoon.seedflowplus.domain.schedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleStatus;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleSource;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import(DealScheduleRepositoryTest.QuerydslTestConfig.class)
class DealScheduleRepositoryTest {

    @Autowired
    private DealScheduleRepository dealScheduleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsCntDocType() {
        Employee owner = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("담당 영업")
                .employeeEmail("owner@test.com")
                .employeePhone("010-1111-1111")
                .address("서울")
                .build();
        markTimestamps(owner);
        entityManager.persist(owner);

        Client client = Client.builder()
                .clientCode("C-1")
                .clientName("테스트 거래처")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("client@test.com")
                .managerEmployee(owner)
                .build();
        markTimestamps(client);
        entityManager.persist(client);

        User assignee = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(owner)
                .build();
        markTimestamps(assignee);
        entityManager.persist(assignee);

        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.CNT)
                .latestRefId(1L)
                .latestTargetCode("CNT-1")
                .lastActivityAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .build();
        markTimestamps(deal);
        entityManager.persist(deal);

        DealSchedule schedule = DealSchedule.builder()
                .deal(deal)
                .client(client)
                .assigneeUser(assignee)
                .title("계약 시작일: 테스트 거래처")
                .description(null)
                .startAt(LocalDateTime.of(2026, 3, 15, 0, 0))
                .endAt(LocalDateTime.of(2026, 3, 16, 0, 0))
                .eventType(DealScheduleEventType.DOC_APPROVED)
                .docType(DealDocType.CNT)
                .refDocId(1L)
                .refDealLogId(null)
                .source(ScheduleSource.AUTO_SYNC)
                .status(DealScheduleStatus.ACTIVE)
                .externalKey("CNT_1_DOC_APPROVED_2026-03-15")
                .lastSyncedAt(LocalDateTime.of(2026, 3, 10, 10, 5))
                .build();
        markTimestamps(schedule);
        dealScheduleRepository.saveAndFlush(schedule);

        entityManager.clear();

        DealSchedule found = dealScheduleRepository.findByExternalKey("CNT_1_DOC_APPROVED_2026-03-15")
                .orElseThrow();

        assertThat(found.getDocType()).isEqualTo(DealDocType.CNT);
        assertThat(found.getEventType()).isEqualTo(DealScheduleEventType.DOC_APPROVED);
        assertThat(found.getTitle()).isEqualTo("계약 시작일: 테스트 거래처");
    }

    private void markTimestamps(Object entity) {
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 3, 10, 10, 0));
        ReflectionTestUtils.setField(entity, "updatedAt", LocalDateTime.of(2026, 3, 10, 10, 0));
    }

    @TestConfiguration
    static class QuerydslTestConfig {

        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }
}
