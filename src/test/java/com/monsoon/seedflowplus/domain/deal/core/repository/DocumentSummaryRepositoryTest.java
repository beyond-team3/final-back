package com.monsoon.seedflowplus.domain.deal.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.config.QuerydslConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentMethod;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

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
        entityManager.createNativeQuery(loadViewSql()).executeUpdate();
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

    @Test
    @DisplayName("문서 목록 조회는 거래처명과 담당자명을 함께 반환한다")
    void searchDocumentsReturnsClientAndOwnerNames() {
        Employee owner = persistEmployee("EMP-NAME");
        Client client = persistClient("CLI-NAME", "555-55-55555", owner);
        SalesDeal deal = persistDeal(client, owner, 501L, LocalDateTime.of(2026, 3, 10, 13, 0));
        persistStatementGraph(
                deal,
                client,
                owner,
                "CNT-NAME-1",
                "ORD-NAME-1",
                "STMT-NAME-1",
                new BigDecimal("55000"),
                LocalDateTime.of(2026, 3, 10, 13, 0)
        );
        flushAndClear();

        CustomUserDetails principal = adminPrincipal();

        Page<DocumentSummary> result = documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder()
                        .docType(DealType.STMT)
                        .build(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))),
                principal
        );

        assertThat(result.getContent())
                .singleElement()
                .satisfies(document -> {
                    assertThat(document.getClientName()).isEqualTo("CLI-NAME");
                    assertThat(document.getOwnerEmployeeName()).isEqualTo("EMP-NAME");
                });
    }

    @Test
    @DisplayName("CLIENT 문서 조회는 관리자 승인 전 또는 관리자 반려된 견적서와 계약서를 제외한다")
    void searchDocumentsExcludesAdminPendingQuotationAndContractForClient() {
        Employee owner = persistEmployee("EMP-CLIENT-DOC");
        Client client = persistClient("CLI-CLIENT-DOC", "666-66-66666", owner);
        SalesDeal deal = persistDeal(client, owner, 601L, LocalDateTime.of(2026, 3, 10, 14, 0));

        persistQuotation(deal, client, owner, "QUO-HIDDEN", QuotationStatus.REJECTED_ADMIN, LocalDateTime.of(2026, 3, 10, 14, 0));
        persistQuotation(deal, client, owner, "QUO-VISIBLE", QuotationStatus.WAITING_CLIENT, LocalDateTime.of(2026, 3, 10, 15, 0));
        persistContract(client, deal, owner, "CNT-HIDDEN", BigDecimal.valueOf(10000), ContractStatus.WAITING_ADMIN, LocalDateTime.of(2026, 3, 10, 16, 0));
        persistContract(client, deal, owner, "CNT-VISIBLE", BigDecimal.valueOf(20000), ContractStatus.COMPLETED, LocalDateTime.of(2026, 3, 10, 17, 0));
        flushAndClear();

        CustomUserDetails principal = clientPrincipal(client.getId());

        assertThat(searchDocCodes(DealType.QUO, principal)).containsExactly("QUO-VISIBLE");
        assertThat(searchDocCodes(DealType.CNT, principal)).containsExactly("CNT-VISIBLE");
    }

    @Test
    @DisplayName("문서 목록 조회는 뷰에 남아 있어도 DELETED 상태 문서를 제외한다")
    void searchDocumentsExcludesDeletedStatusEvenIfViewContainsIt() {
        Employee owner = persistEmployee("EMP-DELETED-DOC");
        Client client = persistClient("CLI-DELETED-DOC", "777-77-77777", owner);
        SalesDeal deal = persistDeal(client, owner, 701L, LocalDateTime.of(2026, 3, 10, 18, 0));
        persistQuotation(deal, client, owner, "QUO-DELETED", QuotationStatus.DELETED, LocalDateTime.of(2026, 3, 10, 18, 0));
        flushAndClear();

        entityManager.createNativeQuery("DROP VIEW IF EXISTS v_document_summary").executeUpdate();
        entityManager.createNativeQuery("""
                CREATE VIEW v_document_summary AS
                SELECT CONCAT('QUO-', quo_id) AS surrogate_id,
                       'QUO' AS doc_type, quo_id AS doc_id, deal_id, client_id,
                       quotation_code AS doc_code, total_amount AS amount, expired_date,
                       CONCAT('', status) AS status, created_at,
                       NULL AS client_name, NULL AS owner_employee_name
                FROM tbl_quotation_header
                """).executeUpdate();
        flushAndClear();

        Page<DocumentSummary> result = documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder()
                        .docType(DealType.QUO)
                        .build(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))),
                adminPrincipal()
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("CLIENT 조회는 client_id가 null인 ORD INV는 포함하지만 PAY는 제외한다")
    void searchDocumentsUsesDealClientForNullableClientDocsExceptPay() {
        Employee owner = persistEmployee("EMP-NULL-CLIENT");
        Client client = persistClient("CLI-NULL-CLIENT", "333-33-33333", owner);
        SalesDeal deal = persistDeal(client, owner, 301L, LocalDateTime.of(2026, 3, 10, 11, 0));
        ContractHeader contract = persistContract(
                client,
                deal,
                owner,
                "CNT-NULL-CLIENT",
                new BigDecimal("33000"),
                LocalDateTime.of(2026, 3, 8, 11, 0)
        );
        OrderHeader order = persistOrder(contract, null, deal, owner, "ORD-NULL-CLIENT",
                new BigDecimal("33000"), LocalDateTime.of(2026, 3, 9, 11, 0));
        Invoice invoice = persistInvoice(10L, null, deal, owner, "INV-NULL-CLIENT",
                new BigDecimal("33000"), LocalDateTime.of(2026, 3, 10, 11, 0));
        persistPayment(invoice, null, deal, "PAY-NULL-CLIENT",
                LocalDateTime.of(2026, 3, 10, 12, 0));
        flushAndClear();

        CustomUserDetails principal = clientPrincipal(client.getId());

        assertThat(searchDocCodes(DealType.ORD, principal)).containsExactly("ORD-NULL-CLIENT");
        assertThat(searchDocCodes(DealType.INV, principal)).containsExactly("INV-NULL-CLIENT");
        assertThat(searchDocCodes(DealType.PAY, principal)).isEmpty();
        assertThat(searchDocCodes(null, principal)).doesNotContain("PAY-NULL-CLIENT");
    }

    @Test
    @DisplayName("문서 목록 조회는 PAY 문서를 전체 목록에서도 제외한다")
    void searchDocumentsExcludesPayFromAllDocuments() {
        Employee owner = persistEmployee("EMP-PAY-EXCLUDED");
        Client client = persistClient("CLI-PAY-EXCLUDED", "444-44-44444", owner);
        SalesDeal deal = persistDeal(client, owner, 401L, LocalDateTime.of(2026, 3, 10, 12, 0));
        Invoice invoice = persistInvoice(11L, client, deal, owner, "INV-PAY-EXCLUDED",
                new BigDecimal("44000"), LocalDateTime.of(2026, 3, 10, 12, 0));
        persistPayment(invoice, client, deal, "PAY-PAY-EXCLUDED",
                LocalDateTime.of(2026, 3, 10, 12, 30));
        flushAndClear();

        CustomUserDetails principal = adminPrincipal();

        assertThat(searchDocCodes(null, principal)).doesNotContain("PAY-PAY-EXCLUDED");
        assertThat(searchDocCodes(DealType.PAY, principal)).isEmpty();
    }

    @Test
    @DisplayName("searchDocuments는 권한 정보가 없으면 AccessDeniedException을 던진다")
    void searchDocumentsRejectsMissingUserDetails() {
        assertThatThrownBy(() -> documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder().build(),
                PageRequest.of(0, 10),
                null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("사용자 권한 정보가 없습니다.");
    }

    @Test
    @DisplayName("searchDocuments는 SALES_REP의 employeeId가 없으면 IllegalArgumentException을 던진다")
    void searchDocumentsRejectsSalesRepWithoutEmployeeId() {
        CustomUserDetails principal = new CustomUserDetails(User.builder()
                .loginId("sales-rep-null")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .build());

        assertThatThrownBy(() -> documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder().build(),
                PageRequest.of(0, 10),
                principal
        ))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SALES_REP 사용자에 employeeId가 없습니다.");
    }

    @Test
    @DisplayName("searchDocuments는 CLIENT의 clientId가 없으면 IllegalArgumentException을 던진다")
    void searchDocumentsRejectsClientWithoutClientId() {
        CustomUserDetails principal = new CustomUserDetails(User.builder()
                .loginId("client-null")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.CLIENT)
                .build());

        assertThatThrownBy(() -> documentSummaryRepository.searchDocuments(
                DocumentSummarySearchCondition.builder().build(),
                PageRequest.of(0, 10),
                principal
        ))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLIENT 사용자에 clientId가 없습니다.");
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

    private ContractHeader persistContract(
            Client client,
            SalesDeal deal,
            Employee owner,
            String contractCode,
            BigDecimal totalAmount,
            ContractStatus status,
            LocalDateTime createdAt
    ) {
        ContractHeader contract = persistContract(client, deal, owner, contractCode, totalAmount, createdAt);
        contract.updateStatus(status);
        return contract;
    }

    private ContractHeader persistContract(
            Client client,
            SalesDeal deal,
            Employee owner,
            String contractCode,
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
        setModifiedAuditFields(contract, createdAt);
        entityManager.persist(contract);
        return contract;
    }

    private QuotationHeader persistQuotation(
            SalesDeal deal,
            Client client,
            Employee owner,
            String quotationCode,
            QuotationStatus status,
            LocalDateTime createdAt
    ) {
        QuotationHeader quotation = QuotationHeader.create(
                null,
                quotationCode,
                client,
                deal,
                owner,
                BigDecimal.valueOf(1000),
                null
        );
        quotation.updateStatus(status);
        ReflectionTestUtils.setField(quotation, "expiredDate", LocalDate.of(2026, 4, 10));
        setModifiedAuditFields(quotation, createdAt);
        entityManager.persist(quotation);
        return quotation;
    }

    private OrderHeader persistOrder(
            ContractHeader contract,
            Client client,
            SalesDeal deal,
            Employee owner,
            String orderCode,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
        OrderHeader order = OrderHeader.create(contract, client, deal, owner, orderCode);
        order.updateTotalAmount(totalAmount);
        setCreatedAuditField(order, createdAt);
        entityManager.persist(order);
        return order;
    }

    private Invoice persistInvoice(
            Long contractId,
            Client client,
            SalesDeal deal,
            Employee owner,
            String invoiceCode,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
        Invoice invoice = Invoice.create(
                contractId,
                client,
                deal,
                owner,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                invoiceCode,
                null
        );
        invoice.updateAmount(totalAmount);
        setCreatedAuditField(invoice, createdAt);
        entityManager.persist(invoice);
        return invoice;
    }

    private void persistPayment(
            Invoice invoice,
            Client client,
            SalesDeal deal,
            String paymentCode,
            LocalDateTime createdAt
    ) {
        Payment payment = Payment.create(invoice, client, deal, PaymentMethod.CREDIT_CARD, paymentCode);
        setCreatedAuditField(payment, createdAt);
        entityManager.persist(payment);
    }

    private java.util.List<String> searchDocCodes(DealType docType, CustomUserDetails principal) {
        return documentSummaryRepository.searchDocuments(
                        DocumentSummarySearchCondition.builder()
                                .docType(docType)
                                .build(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))),
                        principal
                )
                .map(DocumentSummary::getDocCode)
                .getContent();
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

    private String loadViewSql() {
        try {
            ClassPathResource resource = new ClassPathResource("db/migration/V1__create_v_document_summary.sql");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("v_document_summary SQL을 읽을 수 없습니다.", e);
        }
    }
}
