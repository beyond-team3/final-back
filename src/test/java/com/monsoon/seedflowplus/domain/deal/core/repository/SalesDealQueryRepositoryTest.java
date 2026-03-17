package com.monsoon.seedflowplus.domain.deal.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monsoon.seedflowplus.core.config.QuerydslConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

@DataJpaTest
@Import(QuerydslConfig.class)
class SalesDealQueryRepositoryTest {

    @Autowired
    private SalesDealRepository salesDealRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUpView() {
        entityManager.createNativeQuery("DROP VIEW IF EXISTS v_document_summary").executeUpdate();
        entityManager.createNativeQuery(loadViewSql()).executeUpdate();
    }

    @Test
    @DisplayName("bootstrap placeholder deal은 목록 조회에서 제외한다")
    void searchDealsExcludesBootstrapPlaceholder() {
        Employee owner = persistEmployee("EMP-PLACEHOLDER");
        Client client = persistClient("CLI-PLACEHOLDER", "111-22-33333", owner);
        persistDeal(
                client,
                owner,
                DealStage.CREATED,
                "PENDING",
                DealType.RFQ,
                0L,
                null,
                LocalDateTime.of(2026, 3, 15, 17, 22, 9)
        );
        flushAndClear();

        Page<SalesDeal> result = salesDealRepository.searchDeals(
                SalesDealSearchCondition.builder().build(),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("lastActivityAt")))
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 초기 견적서만 가진 deal은 목록에서 제외하고 진행 이력이 있는 deal은 유지한다")
    void searchDealsExcludesDeletedQuotationOnlyDeal() {
        Employee owner = persistEmployee("EMP-QUO");
        Client client = persistClient("CLI-QUO", "222-33-44444", owner);

        SalesDeal deletedOnlyDeal = persistDeal(
                client,
                owner,
                DealStage.CANCELED,
                QuotationStatus.DELETED.name(),
                DealType.QUO,
                10L,
                "QUO-DELETE",
                LocalDateTime.of(2026, 3, 15, 17, 25, 0)
        );
        persistQuotation(deletedOnlyDeal, client, owner, "QUO-DELETE", QuotationStatus.DELETED,
                LocalDateTime.of(2026, 3, 15, 17, 21, 0));

        SalesDeal activeDeal = persistDeal(
                client,
                owner,
                DealStage.PENDING_CLIENT,
                QuotationStatus.FINAL_APPROVED.name(),
                DealType.QUO,
                20L,
                "QUO-KEEP",
                LocalDateTime.of(2026, 3, 15, 18, 0, 0)
        );
        persistQuotation(activeDeal, client, owner, "QUO-KEEP", QuotationStatus.FINAL_APPROVED,
                LocalDateTime.of(2026, 3, 15, 17, 30, 0));
        flushAndClear();

        Page<SalesDeal> result = salesDealRepository.searchDeals(
                SalesDealSearchCondition.builder().build(),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("lastActivityAt")))
        );

        assertThat(result.getContent())
                .extracting(SalesDeal::getLatestTargetCode)
                .containsExactly("QUO-KEEP");
    }

    @Test
    @DisplayName("거래처 뷰는 관리자 승인 전 또는 관리자 반려된 견적서/계약 deal을 제외한다")
    void searchDealsExcludesAdminPendingDocumentsForClientView() {
        Employee owner = persistEmployee("EMP-CLIENT-VIEW");
        Client client = persistClient("CLI-CLIENT-VIEW", "333-44-55555", owner);

        persistDeal(
                client,
                owner,
                DealStage.REJECTED_ADMIN,
                QuotationStatus.REJECTED_ADMIN.name(),
                DealType.QUO,
                31L,
                "QUO-HIDDEN",
                LocalDateTime.of(2026, 3, 16, 9, 0, 0)
        );
        persistDeal(
                client,
                owner,
                DealStage.PENDING_CLIENT,
                QuotationStatus.WAITING_CLIENT.name(),
                DealType.QUO,
                32L,
                "QUO-VISIBLE",
                LocalDateTime.of(2026, 3, 16, 10, 0, 0)
        );
        flushAndClear();

        Page<SalesDeal> result = salesDealRepository.searchDeals(
                SalesDealSearchCondition.builder()
                        .clientId(client.getId())
                        .clientPostAdminApprovalOnly(true)
                        .build(),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("lastActivityAt")))
        );

        assertThat(result.getContent())
                .extracting(SalesDeal::getLatestTargetCode)
                .containsExactly("QUO-VISIBLE");
    }

    private Employee persistEmployee(String code) {
        Employee employee = Employee.builder()
                .employeeCode(code)
                .employeeName(code)
                .employeeEmail(code + "@seedflow.test")
                .employeePhone("010-0000-0000")
                .address("Seoul")
                .build();
        markTimestamps(employee, LocalDateTime.of(2026, 3, 1, 0, 0));
        entityManager.persist(employee);
        return employee;
    }

    private Client persistClient(String code, String brn, Employee owner) {
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
                .managerEmployee(owner)
                .totalCredit(BigDecimal.ZERO)
                .usedCredit(BigDecimal.ZERO)
                .build();
        markTimestamps(client, LocalDateTime.of(2026, 3, 1, 0, 0));
        entityManager.persist(client);
        return client;
    }

    private SalesDeal persistDeal(
            Client client,
            Employee owner,
            DealStage currentStage,
            String currentStatus,
            DealType latestDocType,
            Long latestRefId,
            String latestTargetCode,
            LocalDateTime lastActivityAt
    ) {
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(owner)
                .currentStage(currentStage)
                .currentStatus(currentStatus)
                .latestDocType(latestDocType)
                .latestRefId(latestRefId)
                .latestTargetCode(latestTargetCode)
                .lastActivityAt(lastActivityAt)
                .closedAt(null)
                .summaryMemo(null)
                .build();
        markTimestamps(deal, lastActivityAt);
        entityManager.persist(deal);
        return deal;
    }

    private void persistQuotation(
            SalesDeal deal,
            Client client,
            Employee owner,
            String code,
            QuotationStatus status,
            LocalDateTime createdAt
    ) {
        QuotationHeader quotation = QuotationHeader.create(null, code, client, deal, owner, BigDecimal.TEN, null);
        quotation.updateStatus(status);
        ReflectionTestUtils.setField(quotation, "expiredDate", LocalDate.of(2026, 4, 15));
        ReflectionTestUtils.setField(quotation, "quotationCode", code);
        ReflectionTestUtils.setField(quotation, "createdAt", createdAt);
        ReflectionTestUtils.setField(quotation, "updatedAt", createdAt);
        entityManager.persist(quotation);
    }

    private void markTimestamps(Object entity, LocalDateTime timestamp) {
        ReflectionTestUtils.setField(entity, "createdAt", timestamp);
        ReflectionTestUtils.setField(entity, "updatedAt", timestamp);
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
