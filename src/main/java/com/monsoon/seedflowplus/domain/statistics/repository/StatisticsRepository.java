package com.monsoon.seedflowplus.domain.statistics.repository;

import static com.monsoon.seedflowplus.domain.account.entity.QClient.client;
import static com.monsoon.seedflowplus.domain.account.entity.QEmployee.employee;
import static com.monsoon.seedflowplus.domain.billing.invoice.entity.QInvoice.invoice;
import static com.monsoon.seedflowplus.domain.billing.invoice.entity.QInvoiceStatement.invoiceStatement;
import static com.monsoon.seedflowplus.domain.billing.statement.entity.QStatement.statement;
import static com.monsoon.seedflowplus.domain.sales.contract.entity.QContractDetail.contractDetail;
import static com.monsoon.seedflowplus.domain.sales.order.entity.QOrderDetail.orderDetail;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsPeriod;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsRankingType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatisticsRepository {

    private static final String ALL_TARGET_ID = "ALL";
    private static final String ALL_TARGET_NAME = "전체";

    private final JPAQueryFactory queryFactory;

    public List<TrendBucketRow> findMySalesTrend(StatisticsFilter filter, Long employeeId) {
        return findOverallTrend(filter, invoice.employee.id.eq(employeeId), String.valueOf(employeeId), null);
    }

    public List<TrendBucketRow> findAdminSalesTrend(StatisticsFilter filter) {
        return findOverallTrend(filter, null, ALL_TARGET_ID, ALL_TARGET_NAME);
    }

    public List<TrendBucketRow> findSalesTrendByEmployee(StatisticsFilter filter, BooleanExpression scopeCondition) {
        StringExpression periodExpr = periodExpression(filter.period());
        NumberExpression<BigDecimal> salesExpr = invoice.totalAmount.sum().coalesce(BigDecimal.ZERO);

        return queryFactory
                .select(employee.id.stringValue(), employee.employeeName, periodExpr, salesExpr)
                .from(invoice)
                .leftJoin(invoice.employee, employee)
                .leftJoin(invoice.client, client)
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        employeeIdIn(filter.employeeIds()),
                        scopeCondition
                )
                .groupBy(employee.id, employee.employeeName, periodExpr)
                .orderBy(employee.id.asc(), periodExpr.asc())
                .fetch()
                .stream()
                .map(row -> new TrendBucketRow(
                        row.get(employee.id.stringValue()),
                        row.get(employee.employeeName),
                        row.get(periodExpr),
                        safeAmount(row.get(salesExpr))
                ))
                .toList();
    }

    public List<TrendBucketRow> findSalesTrendByClient(StatisticsFilter filter, BooleanExpression scopeCondition) {
        StringExpression periodExpr = periodExpression(filter.period());
        NumberExpression<BigDecimal> salesExpr = invoice.totalAmount.sum().coalesce(BigDecimal.ZERO);

        return queryFactory
                .select(client.id.stringValue(), client.clientName, periodExpr, salesExpr)
                .from(invoice)
                .leftJoin(invoice.client, client)
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        clientIdIn(filter.clientIds()),
                        scopeCondition
                )
                .groupBy(client.id, client.clientName, periodExpr)
                .orderBy(client.id.asc(), periodExpr.asc())
                .fetch()
                .stream()
                .map(row -> new TrendBucketRow(
                        row.get(client.id.stringValue()),
                        row.get(client.clientName),
                        row.get(periodExpr),
                        safeAmount(row.get(salesExpr))
                ))
                .toList();
    }

    public List<TrendBucketRow> findSalesTrendByVariety(StatisticsFilter filter, BooleanExpression scopeCondition) {
        StringExpression periodExpr = periodExpression(filter.period());
        StringExpression varietyExpr = contractDetail.productCategory;
        NumberExpression<BigDecimal> lineAmountExpr = billedLineAmountExpression();
        NumberExpression<BigDecimal> groupedAmountExpr = lineAmountExpr.sum().coalesce(BigDecimal.ZERO);

        return queryFactory
                .select(varietyExpr, periodExpr, invoice.id, groupedAmountExpr)
                .from(invoice)
                .leftJoin(invoice.client, client)
                .join(invoiceStatement).on(invoiceStatement.invoice.id.eq(invoice.id), invoiceStatement.included.isTrue())
                .join(statement).on(statement.id.eq(invoiceStatement.statement.id))
                .join(orderDetail).on(orderDetail.orderHeader.id.eq(statement.orderHeader.id))
                .join(contractDetail).on(contractDetail.id.eq(orderDetail.contractDetail.id))
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        varietyIn(filter.varietyCodes()),
                        scopeCondition
                )
                .groupBy(varietyExpr, periodExpr, invoice.id)
                .orderBy(varietyExpr.asc(), periodExpr.asc(), invoice.id.asc())
                .fetch()
                .stream()
                .map(row -> new TrendBucketRow(
                        row.get(varietyExpr),
                        row.get(varietyExpr),
                        row.get(periodExpr),
                        safeAmount(row.get(groupedAmountExpr))
                ))
                .toList();
    }

    public List<RankingRow> findRanking(StatisticsFilter filter, BooleanExpression scopeCondition) {
        return switch (filter.type()) {
            case EMPLOYEE -> findEmployeeRanking(filter, scopeCondition);
            case CLIENT -> findClientRanking(filter, scopeCondition);
            case VARIETY -> findVarietyRanking(filter, scopeCondition);
        };
    }

    private List<TrendBucketRow> findOverallTrend(
            StatisticsFilter filter,
            BooleanExpression scopeCondition,
            String targetId,
            String targetName
    ) {
        StringExpression periodExpr = periodExpression(filter.period());
        NumberExpression<BigDecimal> salesExpr = invoice.totalAmount.sum().coalesce(BigDecimal.ZERO);

        return queryFactory
                .select(periodExpr, salesExpr)
                .from(invoice)
                .leftJoin(invoice.client, client)
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        scopeCondition
                )
                .groupBy(periodExpr)
                .orderBy(periodExpr.asc())
                .fetch()
                .stream()
                .map(row -> new TrendBucketRow(
                        targetId,
                        targetName,
                        row.get(periodExpr),
                        safeAmount(row.get(salesExpr))
                ))
                .toList();
    }

    private List<RankingRow> findEmployeeRanking(StatisticsFilter filter, BooleanExpression scopeCondition) {
        NumberExpression<BigDecimal> salesExpr = invoice.totalAmount.sum().coalesce(BigDecimal.ZERO);

        return queryFactory
                .select(employee.id.stringValue(), employee.employeeName, salesExpr)
                .from(invoice)
                .leftJoin(invoice.employee, employee)
                .leftJoin(invoice.client, client)
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        employeeIdIn(filter.employeeIds()),
                        scopeCondition
                )
                .groupBy(employee.id, employee.employeeName)
                .orderBy(salesExpr.desc(), employee.employeeName.asc())
                .limit(filter.limit())
                .fetch()
                .stream()
                .map(row -> new RankingRow(
                        row.get(employee.id.stringValue()),
                        row.get(employee.employeeName),
                        safeAmount(row.get(salesExpr))
                ))
                .toList();
    }

    private List<RankingRow> findClientRanking(StatisticsFilter filter, BooleanExpression scopeCondition) {
        NumberExpression<BigDecimal> salesExpr = invoice.totalAmount.sum().coalesce(BigDecimal.ZERO);

        return queryFactory
                .select(client.id.stringValue(), client.clientName, salesExpr)
                .from(invoice)
                .leftJoin(invoice.client, client)
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        clientIdIn(filter.clientIds()),
                        scopeCondition
                )
                .groupBy(client.id, client.clientName)
                .orderBy(salesExpr.desc(), client.clientName.asc())
                .limit(filter.limit())
                .fetch()
                .stream()
                .map(row -> new RankingRow(
                        row.get(client.id.stringValue()),
                        row.get(client.clientName),
                        safeAmount(row.get(salesExpr))
                ))
                .toList();
    }

    private List<RankingRow> findVarietyRanking(StatisticsFilter filter, BooleanExpression scopeCondition) {
        StringExpression varietyExpr = contractDetail.productCategory;
        NumberExpression<BigDecimal> lineAmountExpr = billedLineAmountExpression();
        NumberExpression<BigDecimal> groupedAmountExpr = lineAmountExpr.sum().coalesce(BigDecimal.ZERO);

        List<Tuple> rows = queryFactory
                .select(varietyExpr, invoice.id, groupedAmountExpr)
                .from(invoice)
                .leftJoin(invoice.client, client)
                .join(invoiceStatement).on(invoiceStatement.invoice.id.eq(invoice.id), invoiceStatement.included.isTrue())
                .join(statement).on(statement.id.eq(invoiceStatement.statement.id))
                .join(orderDetail).on(orderDetail.orderHeader.id.eq(statement.orderHeader.id))
                .join(contractDetail).on(contractDetail.id.eq(orderDetail.contractDetail.id))
                .where(
                        invoice.status.eq(InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.from(), filter.to()),
                        varietyIn(filter.varietyCodes()),
                        scopeCondition
                )
                .groupBy(varietyExpr, invoice.id)
                .fetch();

        Map<String, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(varietyExpr),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                row -> safeAmount(row.get(groupedAmountExpr)),
                                BigDecimal::add
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> new RankingRow(entry.getKey(), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RankingRow::sales).reversed().thenComparing(RankingRow::targetName))
                .limit(filter.limit())
                .toList();
    }

    private StringExpression periodExpression(StatisticsPeriod period) {
        if (period == StatisticsPeriod.QUARTERLY) {
            return Expressions.stringTemplate("CONCAT(YEAR({0}), '-Q', QUARTER({0}))", invoice.invoiceDate);
        }
        return Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", invoice.invoiceDate);
    }

    private BooleanExpression employeeIdIn(List<Long> employeeIds) {
        return employeeIds == null || employeeIds.isEmpty() ? null : invoice.employee.id.in(employeeIds);
    }

    private BooleanExpression clientIdIn(List<Long> clientIds) {
        return clientIds == null || clientIds.isEmpty() ? null : invoice.client.id.in(clientIds);
    }

    private BooleanExpression varietyIn(List<String> varietyCodes) {
        if (varietyCodes == null || varietyCodes.isEmpty()) {
            return null;
        }
        List<String> normalized = varietyCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toUpperCase)
                .toList();
        return normalized.isEmpty() ? null : contractDetail.productCategory.upper().in(normalized);
    }

    private NumberExpression<BigDecimal> billedLineAmountExpression() {
        return Expressions.numberTemplate(
                BigDecimal.class,
                "coalesce({0}, 0) * coalesce({1}, 0)",
                contractDetail.unitPrice,
                orderDetail.quantity
        );
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public record TrendBucketRow(
            String targetId,
            String targetName,
            String period,
            BigDecimal sales
    ) {
    }

    public record RankingRow(
            String targetId,
            String targetName,
            BigDecimal sales
    ) {
    }
}
