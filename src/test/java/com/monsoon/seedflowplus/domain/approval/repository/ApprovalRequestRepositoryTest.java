package com.monsoon.seedflowplus.domain.approval.repository;

import com.monsoon.seedflowplus.core.config.QuerydslConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class ApprovalRequestRepositoryTest {

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("searchForClient는 legacy row라도 최신 SalesDealLog 기준 거래처만 노출한다")
    void searchForClientUsesLatestSalesDealLogOnly() {
        Employee oldOwner = persistEmployee("EMP-OLD");
        Employee newOwner = persistEmployee("EMP-NEW");
        Client oldClient = persistClient("CLI-OLD", "111-11-11111", oldOwner);
        Client newClient = persistClient("CLI-NEW", "222-22-22222", newOwner);
        SalesDeal oldDeal = persistDeal(oldClient, oldOwner, 100L, LocalDateTime.of(2026, 3, 1, 9, 0));
        SalesDeal newDeal = persistDeal(newClient, newOwner, 100L, LocalDateTime.of(2026, 3, 2, 9, 0));
        ApprovalRequest legacyRequest = persistApprovalRequest(100L, null);

        persistLog(oldDeal, oldClient, 100L, LocalDateTime.of(2026, 3, 1, 9, 0));
        persistLog(newDeal, newClient, 100L, LocalDateTime.of(2026, 3, 2, 9, 0));
        flushAndClear();

        assertThat(approvalRequestRepository.searchForClient(null, DealType.QUO, null, oldClient.getId(), PageRequest.of(0, 10)))
                .isEmpty();
        assertThat(approvalRequestRepository.searchForClient(null, DealType.QUO, null, newClient.getId(), PageRequest.of(0, 10)))
                .extracting(ApprovalRequest::getId)
                .containsExactly(legacyRequest.getId());
    }

    @Test
    @DisplayName("searchForSalesRep는 최신 SalesDealLog가 가리키는 현재 owner 기준으로만 노출한다")
    void searchForSalesRepUsesLatestSalesDealLogOnly() {
        Employee oldOwner = persistEmployee("EMP-A");
        Employee newOwner = persistEmployee("EMP-B");
        Client client = persistClient("CLI-A", "333-33-33333", oldOwner);
        SalesDeal oldDeal = persistDeal(client, oldOwner, 200L, LocalDateTime.of(2026, 3, 1, 10, 0));
        SalesDeal newDeal = persistDeal(client, newOwner, 200L, LocalDateTime.of(2026, 3, 2, 10, 0));
        ApprovalRequest legacyRequest = persistApprovalRequest(200L, null);

        persistLog(oldDeal, client, 200L, LocalDateTime.of(2026, 3, 1, 10, 0));
        persistLog(newDeal, client, 200L, LocalDateTime.of(2026, 3, 2, 10, 0));
        flushAndClear();

        assertThat(approvalRequestRepository.searchForSalesRep(null, DealType.QUO, null, oldOwner.getId(), PageRequest.of(0, 10)))
                .isEmpty();
        assertThat(approvalRequestRepository.searchForSalesRep(null, DealType.QUO, null, newOwner.getId(), PageRequest.of(0, 10)))
                .extracting(ApprovalRequest::getId)
                .containsExactly(legacyRequest.getId());
    }

    @Test
    @DisplayName("searchForClient는 clientIdSnapshot이 있으면 최신 로그와 무관하게 snapshot을 우선한다")
    void searchForClientPrefersSnapshotOverLegacyLogs() {
        Employee owner = persistEmployee("EMP-SNAPSHOT");
        Client snapshotClient = persistClient("CLI-SNAP", "444-44-44444", owner);
        Client otherClient = persistClient("CLI-OTHER", "555-55-55555", owner);
        SalesDeal deal = persistDeal(otherClient, owner, 300L, LocalDateTime.of(2026, 3, 2, 11, 0));
        ApprovalRequest request = persistApprovalRequest(300L, snapshotClient.getId());

        persistLog(deal, otherClient, 300L, LocalDateTime.of(2026, 3, 2, 11, 0));
        flushAndClear();

        assertThat(approvalRequestRepository.searchForClient(null, DealType.QUO, null, snapshotClient.getId(), PageRequest.of(0, 10)))
                .extracting(ApprovalRequest::getId)
                .containsExactly(request.getId());
        assertThat(approvalRequestRepository.searchForClient(null, DealType.QUO, null, otherClient.getId(), PageRequest.of(0, 10)))
                .isEmpty();
    }

    private Employee persistEmployee(String code) {
        Employee employee = Employee.builder()
                .employeeCode(code)
                .employeeName(code)
                .employeeEmail(code + "@seedflow.test")
                .employeePhone("010-0000-0000")
                .address("Seoul")
                .build();
        setAuditFields(employee);
        entityManager.persist(employee);
        return employee;
    }

    private Client persistClient(String code, String brn, Employee managerEmployee) {
        Client client = Client.builder()
                .clientCode(code)
                .clientName(code)
                .clientBrn(brn)
                .ceoName("CEO-" + code)
                .companyPhone("02-0000-0000")
                .address("Seoul")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("Manager-" + code)
                .managerPhone("010-1111-1111")
                .managerEmail(code + "@client.test")
                .managerEmployee(managerEmployee)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        setAuditFields(client);
        entityManager.persist(client);
        return client;
    }

    private SalesDeal persistDeal(Client client, Employee owner, Long refId, LocalDateTime lastActivityAt) {
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.QUO)
                .latestRefId(refId)
                .latestTargetCode("Q-" + refId)
                .lastActivityAt(lastActivityAt)
                .build();
        setAuditFields(deal);
        entityManager.persist(deal);
        return deal;
    }

    private ApprovalRequest persistApprovalRequest(Long targetId, Long clientIdSnapshot) {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(targetId)
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(clientIdSnapshot)
                .targetCodeSnapshot("Q-" + targetId)
                .build();
        setAuditFields(request);
        entityManager.persist(request);
        return request;
    }

    private void persistLog(SalesDeal deal, Client client, Long refId, LocalDateTime actionAt) {
        com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog log =
                com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog.builder()
                        .deal(deal)
                        .client(client)
                        .docType(DealType.QUO)
                        .refId(refId)
                        .targetCode("Q-" + refId)
                        .fromStage(DealStage.PENDING_ADMIN)
                        .toStage(DealStage.PENDING_ADMIN)
                        .fromStatus("WAITING_ADMIN")
                        .toStatus("WAITING_ADMIN")
                        .actionType(ActionType.SUBMIT)
                        .actionAt(actionAt)
                        .actorType(ActorType.ADMIN)
                        .actorId(deal.getOwnerEmp().getId())
                        .build();
        ReflectionTestUtils.setField(log, "createdAt", actionAt);
        entityManager.persist(log);
    }

    private void setAuditFields(Object entity) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 1, 0, 0);
        ReflectionTestUtils.setField(entity, "createdAt", now);
        ReflectionTestUtils.setField(entity, "updatedAt", now);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
