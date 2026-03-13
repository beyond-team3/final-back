package com.monsoon.seedflowplus.domain.statistics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monsoon.seedflowplus.config.JpaAuditingConfig;
import com.monsoon.seedflowplus.core.config.QuerydslConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsPeriod;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsRankingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, StatisticsRepository.class, JpaAuditingConfig.class})
class StatisticsRepositoryTest {

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void registerDateFormatAlias() {
        entityManager.getEntityManager()
                .createNativeQuery(
                        "CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR " +
                                "\"com.monsoon.seedflowplus.domain.statistics.billing.repository.H2DateFormatFunctions.dateFormat\""
                )
                .executeUpdate();
    }

    @Test
    @DisplayName("PAID 청구서만 사원별 추이에 합산된다")
    void paidInvoicesOnlyAreAggregatedForEmployeeTrend() {
        Employee employee = persistEmployee("001");
        createInvoiceGraph(employee, "CLIENT-1A", "TOMATO", "2026-01-10", "100", InvoiceStatus.PAID);
        createInvoiceGraph(employee, "CLIENT-1B", "TOMATO", "2026-01-20", "200", InvoiceStatus.PUBLISHED);

        List<StatisticsRepository.TrendBucketRow> result = statisticsRepository.findSalesTrendByEmployee(
                new StatisticsFilter(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        StatisticsPeriod.MONTHLY,
                        List.of(employee.getId()),
                        List.of(),
                        List.of(),
                        null,
                        10
                ),
                null
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sales()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("분기 버킷은 YEAR-QN 형식으로 생성된다")
    void quarterlyBucketFormatIsApplied() {
        Employee employee = persistEmployee("002");
        createInvoiceGraph(employee, "CLIENT-2A", "TOMATO", "2026-01-10", "100", InvoiceStatus.PAID);
        createInvoiceGraph(employee, "CLIENT-2B", "TOMATO", "2026-04-10", "200", InvoiceStatus.PAID);

        List<StatisticsRepository.TrendBucketRow> result = statisticsRepository.findSalesTrendByEmployee(
                new StatisticsFilter(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30),
                        StatisticsPeriod.QUARTERLY,
                        List.of(employee.getId()),
                        List.of(),
                        List.of(),
                        null,
                        10
                ),
                null
        );

        assertThat(result).extracting(StatisticsRepository.TrendBucketRow::period)
                .containsExactly("2026-Q1", "2026-Q2");
    }

    @Test
    @DisplayName("랭킹은 limit을 적용하고 매출 내림차순으로 정렬된다")
    void rankingAppliesLimitAndSortOrder() {
        Employee employee1 = persistEmployee("011");
        Employee employee2 = persistEmployee("012");
        Employee employee3 = persistEmployee("013");

        createInvoiceGraph(employee1, "CLIENT-A", "TOMATO", "2026-01-10", "100", InvoiceStatus.PAID);
        createInvoiceGraph(employee2, "CLIENT-B", "TOMATO", "2026-01-10", "300", InvoiceStatus.PAID);
        createInvoiceGraph(employee3, "CLIENT-C", "TOMATO", "2026-01-10", "200", InvoiceStatus.PAID);

        List<StatisticsRepository.RankingRow> result = statisticsRepository.findRanking(
                new StatisticsFilter(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        StatisticsPeriod.MONTHLY,
                        List.of(employee1.getId(), employee2.getId(), employee3.getId()),
                        List.of(),
                        List.of(),
                        StatisticsRankingType.EMPLOYEE,
                        2
                ),
                null
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(row -> row.sales().toPlainString())
                .containsExactly("300.00", "200.00");
    }

    @Test
    @DisplayName("varietyCodes 필터는 선택한 품종만 집계한다")
    void varietyCodesFilterIsApplied() {
        Employee employee = persistEmployee("021");
        createInvoiceGraph(employee, "CLIENT-V1", "TOMATO", "2026-01-10", "120", InvoiceStatus.PAID);
        createInvoiceGraph(employee, "CLIENT-V2", "CABBAGE", "2026-01-10", "80", InvoiceStatus.PAID);

        List<StatisticsRepository.TrendBucketRow> result = statisticsRepository.findSalesTrendByVariety(
                new StatisticsFilter(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        StatisticsPeriod.MONTHLY,
                        List.of(),
                        List.of(),
                        List.of("TOMATO"),
                        null,
                        10
                ),
                null
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetId()).isEqualTo("TOMATO");
        assertThat(result.get(0).sales()).isEqualByComparingTo("120");
    }

    private void createInvoiceGraph(
            Employee employee,
            String clientSuffix,
            String varietyCode,
            String invoiceDate,
            String amount,
            InvoiceStatus invoiceStatus
    ) {
        Client client = persistClient(clientSuffix, employee);
        SalesDeal deal = persistDeal(client, employee, clientSuffix);
        ContractHeader contract = persistContractHeader(clientSuffix, client, deal, employee, amount);
        OrderHeader orderHeader = persistOrderHeader(clientSuffix, contract, client, deal, employee, amount);
        ContractDetail contractDetail = persistContractDetail(contract, varietyCode, amount);
        persistOrderDetail(orderHeader, contractDetail);
        Statement statement = persistStatement(clientSuffix, orderHeader, deal, amount);
        Invoice invoice = persistInvoice(clientSuffix, contract, client, deal, employee, invoiceDate, amount, invoiceStatus);
        persistInvoiceStatement(invoice, statement);
        entityManager.flush();
        entityManager.clear();
    }

    private Employee persistEmployee(String suffix) {
        Employee employee = Employee.builder()
                .employeeCode("EMP-" + suffix)
                .employeeName("직원-" + suffix)
                .employeeEmail("employee-" + suffix + "@test.com")
                .employeePhone("010-1111-" + suffix)
                .address("서울")
                .build();
        return entityManager.persist(employee);
    }

    private Client persistClient(String suffix, Employee employee) {
        Client client = Client.builder()
                .clientCode("CLIENT-" + suffix)
                .clientName("거래처-" + suffix)
                .clientBrn("BRN-" + suffix)
                .ceoName("대표")
                .companyPhone("02-1111-1111")
                .address("서울")
                .clientType(ClientType.NURSERY)
                .managerName("매니저")
                .managerPhone("010-2222-2222")
                .managerEmail("manager-" + suffix + "@test.com")
                .managerEmployee(employee)
                .build();
        return entityManager.persist(client);
    }

    private SalesDeal persistDeal(Client client, Employee employee, String suffix) {
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.CNT)
                .latestRefId(1L)
                .latestTargetCode("CNT-" + suffix)
                .lastActivityAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        return entityManager.persist(deal);
    }

    private ContractHeader persistContractHeader(
            String suffix,
            Client client,
            SalesDeal deal,
            Employee employee,
            String amount
    ) {
        ContractHeader contractHeader = ContractHeader.create(
                "CNT-" + suffix,
                null,
                client,
                deal,
                employee,
                new BigDecimal(amount),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BillingCycle.MONTHLY,
                null,
                null
        );
        return entityManager.persist(contractHeader);
    }

    private OrderHeader persistOrderHeader(
            String suffix,
            ContractHeader contractHeader,
            Client client,
            SalesDeal deal,
            Employee employee,
            String amount
    ) {
        OrderHeader orderHeader = OrderHeader.create(contractHeader, client, deal, employee, "ORD-" + suffix);
        orderHeader.updateTotalAmount(new BigDecimal(amount));
        orderHeader.confirm();
        return entityManager.persist(orderHeader);
    }

    private ContractDetail persistContractDetail(ContractHeader contractHeader, String varietyCode, String amount) {
        ContractDetail detail = new ContractDetail(
                null,
                "품목-" + varietyCode,
                varietyCode,
                1,
                "BOX",
                new BigDecimal(amount),
                new BigDecimal(amount)
        );
        detail.setContract(contractHeader);
        return entityManager.persist(detail);
    }

    private OrderDetail persistOrderDetail(OrderHeader orderHeader, ContractDetail contractDetail) {
        OrderDetail detail = OrderDetail.create(orderHeader, contractDetail, 1L, null, null, null, null, null);
        return entityManager.persist(detail);
    }

    private Statement persistStatement(String suffix, OrderHeader orderHeader, SalesDeal deal, String amount) {
        Statement statement = Statement.create(orderHeader, deal, new BigDecimal(amount), "STMT-" + suffix);
        return entityManager.persist(statement);
    }

    private Invoice persistInvoice(
            String suffix,
            ContractHeader contractHeader,
            Client client,
            SalesDeal deal,
            Employee employee,
            String invoiceDate,
            String amount,
            InvoiceStatus invoiceStatus
    ) {
        Invoice invoice = Invoice.create(
                contractHeader.getId(),
                client,
                deal,
                employee,
                LocalDate.parse(invoiceDate),
                LocalDate.parse(invoiceDate),
                LocalDate.parse(invoiceDate),
                "INV-" + suffix,
                null
        );
        invoice.updateAmount(new BigDecimal(amount));
        if (invoiceStatus == InvoiceStatus.PUBLISHED) {
            invoice.publish();
        } else if (invoiceStatus == InvoiceStatus.PAID) {
            invoice.publish();
            invoice.paid();
        }
        return entityManager.persist(invoice);
    }

    private InvoiceStatement persistInvoiceStatement(Invoice invoice, Statement statement) {
        InvoiceStatement invoiceStatement = InvoiceStatement.create(invoice, statement);
        return entityManager.persist(invoiceStatement);
    }
}
