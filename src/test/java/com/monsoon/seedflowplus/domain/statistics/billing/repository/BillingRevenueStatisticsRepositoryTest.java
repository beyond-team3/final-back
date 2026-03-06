package com.monsoon.seedflowplus.domain.statistics.billing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monsoon.seedflowplus.core.config.QuerydslConfig;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
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
@Import({QuerydslConfig.class, BillingRevenueStatisticsRepository.class})
class BillingRevenueStatisticsRepositoryTest {

    @Autowired
    private BillingRevenueStatisticsRepository repository;

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
    @DisplayName("category 없이 월별 조회하면 공통 필터를 만족하는 데이터만 합산된다")
    void shouldAggregateAllQualifiedMonthlyRevenueWhenCategoryIsNull() {
        seedMonthlyRevenueFixtures();

        List<MonthlyBilledRevenueDto> result = repository.findMonthlyRevenue(filter(null), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(result.get(0).getBilledRevenue()).isEqualByComparingTo("900");
    }

    @Test
    @DisplayName("category가 tomato면 해당 품종의 월별 매출만 합산된다")
    void shouldAggregateOnlyTomatoMonthlyRevenueWhenCategoryIsTomato() {
        seedMonthlyRevenueFixtures();

        List<MonthlyBilledRevenueDto> result = repository.findMonthlyRevenue(filter("tomato"), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(result.get(0).getBilledRevenue()).isEqualByComparingTo("350");
    }

    @Test
    @DisplayName("category가 cabbage면 해당 품종의 월별 매출만 합산된다")
    void shouldAggregateOnlyCabbageMonthlyRevenueWhenCategoryIsCabbage() {
        seedMonthlyRevenueFixtures();

        List<MonthlyBilledRevenueDto> result = repository.findMonthlyRevenue(filter("cabbage"), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(result.get(0).getBilledRevenue()).isEqualByComparingTo("550");
    }

    @Test
    @DisplayName("존재하지 않는 category로 월별 조회하면 집계 결과가 없다")
    void shouldReturnEmptyResultWhenCategoryDoesNotExist() {
        seedMonthlyRevenueFixtures();

        List<MonthlyBilledRevenueDto> result = repository.findMonthlyRevenue(filter("pepper"), null);

        assertThat(result).isEmpty();
        assertThat(sumRevenue(result)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("혼합 품종 invoice가 있어도 category 월별 조회는 해당 품종 금액만 집계한다")
    void shouldAggregateOnlyMatchedCategoryAmountForMixedCategoryInvoice() {
        seedMonthlyRevenueFixtures();

        List<MonthlyBilledRevenueDto> tomatoResult = repository.findMonthlyRevenue(filter("tomato"), null);
        List<MonthlyBilledRevenueDto> cabbageResult = repository.findMonthlyRevenue(filter("cabbage"), null);

        assertThat(tomatoResult).hasSize(1);
        assertThat(tomatoResult.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(tomatoResult.get(0).getBilledRevenue()).isEqualByComparingTo("350");

        assertThat(cabbageResult).hasSize(1);
        assertThat(cabbageResult.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(cabbageResult.get(0).getBilledRevenue()).isEqualByComparingTo("550");
    }

    @Test
    @DisplayName("혼합 품종 invoice가 있어도 품종별 조회는 각 품종 금액만 집계한다")
    void shouldAggregateCategoryRevenueWithoutDuplicatingMixedInvoiceTotal() {
        seedMonthlyRevenueFixtures();

        List<CategoryBilledRevenueDto> result = repository.findCategoryRevenue(filter(null), null);
        Map<String, BigDecimal> revenueMap = toCategoryRevenueMap(result);

        assertThat(revenueMap).containsKeys("tomato", "cabbage");
        assertThat(revenueMap.get("tomato")).isEqualByComparingTo("350");
        assertThat(revenueMap.get("cabbage")).isEqualByComparingTo("550");
    }

    @Test
    @DisplayName("혼합 품종 invoice가 있어도 월별 품종 조회는 각 품종 금액만 집계한다")
    void shouldAggregateMonthlyCategoryRevenueWithoutDuplicatingMixedInvoiceTotal() {
        seedMonthlyRevenueFixtures();

        List<MonthlyCategoryBilledRevenueDto> result = repository.findMonthlyCategoryRevenue(filter(null), null);
        Map<String, BigDecimal> revenueMap = toMonthlyCategoryRevenueMap(result);

        assertThat(revenueMap).containsKeys("2024-01|tomato", "2024-01|cabbage");
        assertThat(revenueMap.get("2024-01|tomato")).isEqualByComparingTo("350");
        assertThat(revenueMap.get("2024-01|cabbage")).isEqualByComparingTo("550");
    }

    @Test
    @DisplayName("월별 총매출은 invoice 총액이 아니라 issued statement 라인 금액 기준으로 집계한다")
    void shouldAggregateMonthlyRevenueFromIssuedStatementLines() {
        createInvoiceGraph(
                List.of(detailSpec("tomato", "400")),
                "999",
                InvoiceStatus.PUBLISHED,
                true,
                StatementStatus.ISSUED,
                "INV-MIS-001",
                "STMT-MIS-001",
                "ORD-MIS-001",
                "CT-MIS-001"
        );
        entityManager.flush();
        entityManager.clear();

        List<MonthlyBilledRevenueDto> monthlyResult = repository.findMonthlyRevenue(filter(null), null);
        List<CategoryBilledRevenueDto> categoryResult = repository.findCategoryRevenue(filter(null), null);

        assertThat(monthlyResult).hasSize(1);
        assertThat(monthlyResult.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(monthlyResult.get(0).getBilledRevenue()).isEqualByComparingTo("400");
        assertThat(sumRevenue(monthlyResult)).isEqualByComparingTo("400");
        assertThat(sumCategoryRevenue(categoryResult)).isEqualByComparingTo("400");
    }

    @Test
    @DisplayName("employeeId가 주어지면 해당 영업사원 담당 주문의 통계만 조회한다")
    void shouldAggregateOnlyScopedSalesRepRevenueWhenEmployeeIdProvided() {
        Employee scopedEmployee = persistEmployee("SCOPE-001");
        Employee otherEmployee = persistEmployee("SCOPE-002");

        createInvoiceGraphForEmployee(
                scopedEmployee,
                List.of(detailSpec("tomato", "120")),
                "120",
                InvoiceStatus.PUBLISHED,
                true,
                StatementStatus.ISSUED,
                "INV-SCOPE-001",
                "STMT-SCOPE-001",
                "ORD-SCOPE-001",
                "CT-SCOPE-001"
        );
        createInvoiceGraphForEmployee(
                otherEmployee,
                List.of(detailSpec("tomato", "80"), detailSpec("cabbage", "220")),
                "300",
                InvoiceStatus.PUBLISHED,
                true,
                StatementStatus.ISSUED,
                "INV-SCOPE-002",
                "STMT-SCOPE-002",
                "ORD-SCOPE-002",
                "CT-SCOPE-002"
        );
        entityManager.flush();
        entityManager.clear();

        List<MonthlyBilledRevenueDto> monthlyResult = repository.findMonthlyRevenue(filter(null), scopedEmployee.getId());
        List<CategoryBilledRevenueDto> categoryResult = repository.findCategoryRevenue(filter(null), scopedEmployee.getId());
        List<MonthlyCategoryBilledRevenueDto> monthlyCategoryResult =
                repository.findMonthlyCategoryRevenue(filter(null), scopedEmployee.getId());

        assertThat(monthlyResult).hasSize(1);
        assertThat(monthlyResult.get(0).getBilledRevenue()).isEqualByComparingTo("120");

        assertThat(categoryResult).hasSize(1);
        assertThat(categoryResult.get(0).getCategory()).isEqualTo("tomato");
        assertThat(categoryResult.get(0).getBilledRevenue()).isEqualByComparingTo("120");

        assertThat(monthlyCategoryResult).hasSize(1);
        assertThat(monthlyCategoryResult.get(0).getMonth()).isEqualTo("2024-01");
        assertThat(monthlyCategoryResult.get(0).getCategory()).isEqualTo("tomato");
        assertThat(monthlyCategoryResult.get(0).getBilledRevenue()).isEqualByComparingTo("120");
    }

    private void seedMonthlyRevenueFixtures() {
        createInvoiceGraph(List.of(detailSpec("tomato", "100")), "100", InvoiceStatus.PUBLISHED, true, StatementStatus.ISSUED, "INV-OK-001", "STMT-OK-001", "ORD-OK-001", "CT-OK-001");
        createInvoiceGraph(List.of(detailSpec("tomato", "200")), "200", InvoiceStatus.PAID, true, StatementStatus.ISSUED, "INV-OK-002", "STMT-OK-002", "ORD-OK-002", "CT-OK-002");
        createInvoiceGraph(List.of(detailSpec("cabbage", "300")), "300", InvoiceStatus.PUBLISHED, true, StatementStatus.ISSUED, "INV-OK-003", "STMT-OK-003", "ORD-OK-003", "CT-OK-003");
        createInvoiceGraph(
                List.of(detailSpec("tomato", "50"), detailSpec("cabbage", "250")),
                "300",
                InvoiceStatus.PUBLISHED,
                true,
                StatementStatus.ISSUED,
                "INV-MIX-001",
                "STMT-MIX-001",
                "ORD-MIX-001",
                "CT-MIX-001"
        );

        createInvoiceGraph(List.of(detailSpec("tomato", "400")), "400", InvoiceStatus.CANCELED, true, StatementStatus.ISSUED, "INV-EX-001", "STMT-EX-001", "ORD-EX-001", "CT-EX-001");
        createInvoiceGraph(List.of(detailSpec("tomato", "500")), "500", InvoiceStatus.PUBLISHED, false, StatementStatus.ISSUED, "INV-EX-002", "STMT-EX-002", "ORD-EX-002", "CT-EX-002");
        createInvoiceGraph(List.of(detailSpec("cabbage", "600")), "600", InvoiceStatus.PUBLISHED, true, StatementStatus.CANCELED, "INV-EX-003", "STMT-EX-003", "ORD-EX-003", "CT-EX-003");

        entityManager.flush();
        entityManager.clear();
    }

    private void createInvoiceGraph(
            List<DetailSpec> detailSpecs,
            String amount,
            InvoiceStatus invoiceStatus,
            boolean included,
            StatementStatus statementStatus,
            String invoiceCode,
            String statementCode,
            String orderCode,
            String contractCode
    ) {
        Employee employee = persistEmployee(invoiceCode);
        createInvoiceGraphForEmployee(
                employee,
                detailSpecs,
                amount,
                invoiceStatus,
                included,
                statementStatus,
                invoiceCode,
                statementCode,
                orderCode,
                contractCode
        );
    }

    private void createInvoiceGraphForEmployee(
            Employee employee,
            List<DetailSpec> detailSpecs,
            String amount,
            InvoiceStatus invoiceStatus,
            boolean included,
            StatementStatus statementStatus,
            String invoiceCode,
            String statementCode,
            String orderCode,
            String contractCode
    ) {
        Client client = persistClient(invoiceCode, employee);
        SalesDeal deal = persistDeal(client, employee, invoiceCode);
        ContractHeader contract = persistContractHeader(contractCode, client, deal, employee);
        OrderHeader orderHeader = persistOrderHeader(orderCode, contract, client, deal, employee, amount);
        for (DetailSpec detailSpec : detailSpecs) {
            ContractDetail contractDetail = persistContractDetail(contract, detailSpec.productCategory(), detailSpec.amount());
            persistOrderDetail(orderHeader, contractDetail);
        }
        Statement statement = persistStatement(statementCode, orderHeader, deal, amount, statementStatus);
        Invoice invoice = persistInvoice(invoiceCode, contract, client, deal, employee, amount, invoiceStatus);
        persistInvoiceStatement(invoice, statement, included);
    }

    private Employee persistEmployee(String suffix) {
        Employee employee = Employee.builder()
                .employeeCode("EMP-" + suffix)
                .employeeName("직원-" + suffix)
                .employeeEmail(suffix.toLowerCase() + "@seedflow.test")
                .employeePhone("010-0000-" + suffix.substring(suffix.length() - 4))
                .address("서울")
                .build();
        return entityManager.persist(employee);
    }

    private Client persistClient(String suffix, Employee employee) {
        Client client = Client.builder()
                .clientCode("CLIENT-" + suffix)
                .clientName("거래처-" + suffix)
                .clientBrn("BRN-" + suffix)
                .ceoName("대표-" + suffix)
                .companyPhone("02-0000-" + suffix.substring(suffix.length() - 4))
                .address("서울 강남구")
                .clientType(ClientType.DISTRIBUTOR)
                .managerName("담당자-" + suffix)
                .managerPhone("010-1111-" + suffix.substring(suffix.length() - 4))
                .managerEmail("manager-" + suffix.toLowerCase() + "@seedflow.test")
                .managerEmployee(employee)
                .build();
        return entityManager.persist(client);
    }

    private SalesDeal persistDeal(Client client, Employee employee, String suffix) {
        SalesDeal deal = SalesDeal.builder()
                .client(client)
                .ownerEmp(employee)
                .currentStage(DealStage.ISSUED)
                .currentStatus(InvoiceStatus.PUBLISHED.name())
                .latestDocType(DealType.INV)
                .latestRefId(1L)
                .latestTargetCode("TARGET-" + suffix)
                .lastActivityAt(LocalDateTime.of(2024, 1, 31, 12, 0))
                .summaryMemo("통계 테스트")
                .build();
        return entityManager.persist(deal);
    }

    private ContractHeader persistContractHeader(String contractCode, Client client, SalesDeal deal, Employee employee) {
        ContractHeader contract = ContractHeader.create(
                contractCode,
                null,
                client,
                deal,
                employee,
                new BigDecimal("1000"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                BillingCycle.MONTHLY,
                null,
                "통계 테스트 계약"
        );
        return entityManager.persist(contract);
    }

    private ContractDetail persistContractDetail(ContractHeader contract, String productCategory, String amount) {
        BigDecimal lineAmount = new BigDecimal(amount);
        ContractDetail contractDetail = new ContractDetail(
                null,
                productCategory + "-name",
                productCategory,
                1,
                "BOX",
                lineAmount,
                lineAmount
        );
        contract.addItem(contractDetail);
        return entityManager.persist(contractDetail);
    }

    private OrderHeader persistOrderHeader(
            String orderCode,
            ContractHeader contract,
            Client client,
            SalesDeal deal,
            Employee employee,
            String amount
    ) {
        OrderHeader orderHeader = OrderHeader.create(contract, client, deal, employee, orderCode);
        orderHeader.updateTotalAmount(new BigDecimal(amount));
        orderHeader.confirm();
        return entityManager.persist(orderHeader);
    }

    private OrderDetail persistOrderDetail(OrderHeader orderHeader, ContractDetail contractDetail) {
        OrderDetail orderDetail = OrderDetail.create(
                orderHeader,
                contractDetail,
                1L,
                "수령인",
                "010-2222-3333",
                "서울시 송파구",
                "101호",
                "문 앞"
        );
        return entityManager.persist(orderDetail);
    }

    private Statement persistStatement(
            String statementCode,
            OrderHeader orderHeader,
            SalesDeal deal,
            String amount,
            StatementStatus statementStatus
    ) {
        Statement statement = Statement.create(orderHeader, deal, new BigDecimal(amount), statementCode);
        if (statementStatus != StatementStatus.ISSUED) {
            statement.cancel();
        }
        return entityManager.persist(statement);
    }

    private Invoice persistInvoice(
            String invoiceCode,
            ContractHeader contract,
            Client client,
            SalesDeal deal,
            Employee employee,
            String amount,
            InvoiceStatus invoiceStatus
    ) {
        Invoice invoice = Invoice.create(
                contract.getId(),
                client,
                deal,
                employee,
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                invoiceCode,
                "통계 테스트 청구서"
        );
        invoice.updateAmount(new BigDecimal(amount));
        if (invoiceStatus == InvoiceStatus.PUBLISHED) {
            invoice.publish();
        } else if (invoiceStatus == InvoiceStatus.PAID) {
            invoice.publish();
            invoice.paid();
        } else if (invoiceStatus == InvoiceStatus.CANCELED) {
            invoice.cancel();
        }
        return entityManager.persist(invoice);
    }

    private InvoiceStatement persistInvoiceStatement(Invoice invoice, Statement statement, boolean included) {
        InvoiceStatement invoiceStatement = InvoiceStatement.create(invoice, statement);
        if (!included) {
            invoiceStatement.exclude();
        }
        return entityManager.persist(invoiceStatement);
    }

    private BillingRevenueStatisticsFilter filter(String category) {
        return new BillingRevenueStatisticsFilter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                category
        );
    }

    private BigDecimal sumRevenue(List<MonthlyBilledRevenueDto> rows) {
        return rows.stream()
                .map(MonthlyBilledRevenueDto::getBilledRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCategoryRevenue(List<CategoryBilledRevenueDto> rows) {
        return rows.stream()
                .map(CategoryBilledRevenueDto::getBilledRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> toCategoryRevenueMap(List<CategoryBilledRevenueDto> rows) {
        return rows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        CategoryBilledRevenueDto::getCategory,
                        CategoryBilledRevenueDto::getBilledRevenue
                ));
    }

    private Map<String, BigDecimal> toMonthlyCategoryRevenueMap(List<MonthlyCategoryBilledRevenueDto> rows) {
        return rows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.getMonth() + "|" + row.getCategory(),
                        MonthlyCategoryBilledRevenueDto::getBilledRevenue
                ));
    }

    private DetailSpec detailSpec(String productCategory, String amount) {
        return new DetailSpec(productCategory, amount);
    }

    private record DetailSpec(String productCategory, String amount) {
    }
}
