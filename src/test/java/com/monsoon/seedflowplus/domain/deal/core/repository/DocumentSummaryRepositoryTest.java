package com.monsoon.seedflowplus.domain.deal.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.config.QuerydslConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(QuerydslConfig.class)
class DocumentSummaryRepositoryTest {

    @Autowired
    private DocumentSummaryRepository documentSummaryRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUpView() {
        entityManager.createNativeQuery("DROP VIEW IF EXISTS v_document_summary").executeUpdate();
        entityManager.createNativeQuery("""
                CREATE VIEW v_document_summary AS
                SELECT CONCAT('STMT-', s.statement_id) AS surrogate_id,
                       'STMT' AS doc_type,
                       s.statement_id AS doc_id,
                       s.deal_id AS deal_id,
                       d.client_id AS client_id,
                       s.statement_code AS doc_code,
                       s.total_amount AS amount,
                       NULL AS expired_date,
                       s.status AS status,
                       s.created_at AS created_at
                FROM tbl_statement s
                JOIN tbl_sales_deal d ON d.deal_id = s.deal_id
                """).executeUpdate();
    }

    @Test
    @DisplayName("CLIENT 조회는 STMT 문서도 client 범위에 포함한다")
    void searchDocumentsIncludesStatementForClientRole() {
        Employee owner = persistEmployee("EMP-STMT");
        Client client = persistClient("CLI-STMT", "111-11-11111", owner);
        SalesDeal deal = persistDeal(client, owner, 101L, LocalDateTime.of(2026, 3, 10, 10, 0));
        persistStatementGraph(deal, client, owner, "CNT-STMT-1", "ORD-STMT-1", "STMT-STMT-1",
                new BigDecimal("11000"), LocalDateTime.of(2026, 3, 10, 10, 0));
        flushAndClear();

        CustomUserDetails principal = clientPrincipal(client.getId());

        Page<DocumentSummary> result = documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder()
                        .docType(DealType.STMT)
                        .build(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))),
                principal
        );

        assertThat(result.getContent())
                .extracting(DocumentSummary::getDocCode)
                .containsExactly("STMT-STMT-1");
    }

    @Test
    @DisplayName("searchDocuments는 createdAt 정렬 방향을 Pageable대로 반영한다")
    void searchDocumentsAppliesCreatedAtSortDirection() {
        Employee owner = persistEmployee("EMP-SORT");
        Client client = persistClient("CLI-SORT", "222-22-22222", owner);
        SalesDeal deal = persistDeal(client, owner, 201L, LocalDateTime.of(2026, 3, 10, 8, 0));
        persistStatementGraph(deal, client, owner, "CNT-SORT-1", "ORD-SORT-1", "STMT-SORT-1",
                new BigDecimal("11000"), LocalDateTime.of(2026, 3, 10, 8, 0));
        persistStatementGraph(deal, client, owner, "CNT-SORT-2", "ORD-SORT-2", "STMT-SORT-2",
                new BigDecimal("22000"), LocalDateTime.of(2026, 3, 10, 9, 0));
        flushAndClear();

        CustomUserDetails principal = adminPrincipal();

        Page<DocumentSummary> ascending = documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder()
                        .docType(DealType.STMT)
                        .build(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("createdAt"))),
                principal
        );
        Page<DocumentSummary> descending = documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder()
                        .docType(DealType.STMT)
                        .build(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))),
                principal
        );

        assertThat(ascending.getContent())
                .extracting(DocumentSummary::getDocCode)
                .containsExactly("STMT-SORT-1", "STMT-SORT-2");
        assertThat(descending.getContent())
                .extracting(DocumentSummary::getDocCode)
                .containsExactly("STMT-SORT-2", "STMT-SORT-1");
    }

    private Employee persistEmployee(String code) {
        Employee employee = Employee.builder()
                .employeeCode(code)
                .employeeName(code)
                .employeeEmail(code + "@seedflow.test")
                .employeePhone("010-0000-0000")
                .address("Seoul")
                .build();
        setModifiedAuditFields(employee, LocalDateTime.of(2026, 3, 1, 0, 0));
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
        setModifiedAuditFields(client, LocalDateTime.of(2026, 3, 1, 0, 0));
        entityManager.persist(client);
        return client;
    }

    private SalesDeal persistDeal(Client client, Employee owner, Long refId, LocalDateTime lastActivityAt) {
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.CNT)
                .latestRefId(refId)
                .latestTargetCode("STMT-" + refId)
                .lastActivityAt(lastActivityAt)
                .build();
        setModifiedAuditFields(deal, lastActivityAt.minusDays(1));
        entityManager.persist(deal);
        return deal;
    }

    private void persistStatementGraph(
            SalesDeal deal,
            Client client,
            Employee owner,
            String contractCode,
            String orderCode,
            String statementCode,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
        ContractHeader contract = ContractHeader.create(
                contractCode,
                null,
                client,
                deal,
                owner,
                totalAmount,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31),
                BillingCycle.MONTHLY,
                null,
                null
        );
        setModifiedAuditFields(contract, createdAt.minusDays(2));
        entityManager.persist(contract);

        OrderHeader order = OrderHeader.create(contract, client, deal, owner, orderCode);
        order.updateTotalAmount(totalAmount);
        setCreatedAuditField(order, createdAt.minusDays(1));
        entityManager.persist(order);

        Statement statement = Statement.create(order, deal, totalAmount, statementCode);
        setCreatedAuditField(statement, createdAt);
        entityManager.persist(statement);
    }

    private CustomUserDetails clientPrincipal(Long clientId) {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        when(principal.getClientId()).thenReturn(clientId);
        return principal;
    }

    private CustomUserDetails adminPrincipal() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.ADMIN);
        return principal;
    }

    private void setModifiedAuditFields(Object entity, LocalDateTime timestamp) {
        ReflectionTestUtils.setField(entity, "createdAt", timestamp);
        ReflectionTestUtils.setField(entity, "updatedAt", timestamp);
    }

    private void setCreatedAuditField(Object entity, LocalDateTime timestamp) {
        ReflectionTestUtils.setField(entity, "createdAt", timestamp);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
